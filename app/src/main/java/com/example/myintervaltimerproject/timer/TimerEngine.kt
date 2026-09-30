package com.example.myintervaltimerproject.timer

import android.os.SystemClock
import com.example.myintervaltimerproject.data.session.ActiveSession

fun interface TimeProvider {
    fun nowMillis(): Long
}

sealed interface TimerEngineEvent {
    data class PhaseStarted(
        val state: TimerState.Running,
    ) : TimerEngineEvent

    data class Resumed(
        val state: TimerState.Running,
    ) : TimerEngineEvent

    /** A running session was restored without starting a new phase cue. */
    data class Restored(
        val state: TimerState.Running,
    ) : TimerEngineEvent

    data class Paused(
        val state: TimerState.Paused,
    ) : TimerEngineEvent

    data class Completed(
        val state: TimerState.Completed,
    ) : TimerEngineEvent

    data object Stopped : TimerEngineEvent
}

/**
 * Single source of truth for timer time and phase transitions.
 *
 * The engine is deliberately ticker-free. A foreground service drives [tick],
 * while all durations use SystemClock.elapsedRealtime() so wall-clock changes
 * cannot make the countdown jump or go negative.
 */
class TimerEngine(
    private val timeProvider: TimeProvider = TimeProvider(SystemClock::elapsedRealtime),
) {

    private val _state =
        kotlinx.coroutines.flow.MutableStateFlow<TimerState>(TimerState.Idle)

    val state: kotlinx.coroutines.flow.StateFlow<TimerState> = _state

    private var config: TimerConfig? = null
    private var plan: List<PhaseItem> = emptyList()
    private var phaseIndex = 0

    private var sessionStartedAtElapsedRealtime = -1L
    private var elapsedBeforePauseMillis = 0L

    @Synchronized
    fun start(config: TimerConfig): List<TimerEngineEvent> {
        validate(config)

        this.config = config
        this.plan = buildTimerPlan(config)
        this.phaseIndex = 0
        this.sessionStartedAtElapsedRealtime = timeProvider.nowMillis()
        this.elapsedBeforePauseMillis = 0L

        if (plan.isEmpty()) {
            val completed = TimerState.Completed(0, config.rounds, 0)
            _state.value = completed
            return listOf(TimerEngineEvent.Completed(completed))
        }

        val phaseStart = sessionStartedAtElapsedRealtime
        val running = createRunningState(
            item = plan[phaseIndex],
            phaseStartElapsedRealtime = phaseStart,
            phaseEndElapsedRealtime = phaseStart + durationMillis(plan[phaseIndex]),
        )
        _state.value = running
        return listOf(TimerEngineEvent.PhaseStarted(running))
    }

    @Synchronized
    fun pause(): List<TimerEngineEvent> {
        if (_state.value !is TimerState.Running) return emptyList()
        val now = timeProvider.nowMillis()

        val events = advanceTo(now, mutableListOf())
        val updated = _state.value as? TimerState.Running
            ?: return events

        closeActiveInterval(now.coerceAtMost(updated.phaseEndsAtElapsedRealtime))
        val remainingMillis = (updated.phaseEndsAtElapsedRealtime - now).coerceAtLeast(0L)
        val paused = TimerState.Paused(
            phase = updated.phase,
            remainingSeconds = remainingSeconds(remainingMillis),
            currentRound = updated.currentRound,
            totalRounds = updated.totalRounds,
            remainingMillis = remainingMillis,
            elapsedBeforePauseMillis = elapsedBeforePauseMillis,
        )
        _state.value = paused
        return events + TimerEngineEvent.Paused(paused)
    }

    @Synchronized
    fun resume(): List<TimerEngineEvent> {
        val current = _state.value as? TimerState.Paused ?: return emptyList()
        val now = timeProvider.nowMillis()
        val remainingMillis = current.remainingMillis.coerceAtLeast(0L)

        if (remainingMillis == 0L) {
            val events = mutableListOf<TimerEngineEvent>()
            phaseIndex += 1
            if (phaseIndex >= plan.size) {
                complete(now, events)
            } else {
                sessionStartedAtElapsedRealtime = now
                val next = plan[phaseIndex]
                val running = createRunningState(
                    item = next,
                    phaseStartElapsedRealtime = now,
                    phaseEndElapsedRealtime = now + durationMillis(next),
                )
                _state.value = running
                events += TimerEngineEvent.PhaseStarted(running)
            }
            return events
        }

        sessionStartedAtElapsedRealtime = now
        val running = createRunningState(
            item = PhaseItem(current.phase, remainingSeconds(remainingMillis).coerceAtLeast(1), current.currentRound),
            phaseStartElapsedRealtime = now,
            phaseEndElapsedRealtime = now + remainingMillis,
            exactDurationMillis = remainingMillis,
        )
        _state.value = running
        return listOf(TimerEngineEvent.Resumed(running))
    }

    @Synchronized
    fun skip(): List<TimerEngineEvent> {
        val current = _state.value
        if (current !is TimerState.Running && current !is TimerState.Paused) return emptyList()

        val now = timeProvider.nowMillis()
        if (current is TimerState.Running) {
            closeActiveInterval(now.coerceAtMost(current.phaseEndsAtElapsedRealtime))
        }

        val nextIndex = phaseIndex + 1
        if (nextIndex >= plan.size) {
            val events = mutableListOf<TimerEngineEvent>()
            complete(now, events)
            return events
        }

        phaseIndex = nextIndex
        sessionStartedAtElapsedRealtime = now
        val next = plan[phaseIndex]
        val running = createRunningState(
            item = next,
            phaseStartElapsedRealtime = now,
            phaseEndElapsedRealtime = now + durationMillis(next),
        )
        _state.value = running
        return listOf(TimerEngineEvent.PhaseStarted(running))
    }

    @Synchronized
    fun stop(): List<TimerEngineEvent> {
        val current = _state.value
        if (current is TimerState.Running) {
            closeActiveInterval(timeProvider.nowMillis().coerceAtMost(current.phaseEndsAtElapsedRealtime))
        }
        sessionStartedAtElapsedRealtime = -1L
        elapsedBeforePauseMillis = 0L
        _state.value = TimerState.Stopped
        return listOf(TimerEngineEvent.Stopped)
    }

    /**
     * Reconciles the current phase against the monotonic clock. When more than
     * one phase elapsed while the process was not scheduled, every crossed phase
     * is returned as an event so audio/vibration are not silently lost.
     */
    @Synchronized
    fun tick(): List<TimerEngineEvent> {
        if (_state.value !is TimerState.Running) return emptyList()
        return advanceTo(timeProvider.nowMillis(), mutableListOf())
    }

    @Synchronized
    fun close() {
        config = null
        plan = emptyList()
        phaseIndex = 0
        sessionStartedAtElapsedRealtime = -1L
        elapsedBeforePauseMillis = 0L
        _state.value = TimerState.Idle
    }

    @Synchronized
    fun restore(session: ActiveSession): List<TimerEngineEvent> {
        validate(session.config)
        if (session.phases.isEmpty()) {
            _state.value = TimerState.Idle
            return emptyList()
        }

        config = session.config
        plan = session.phases.map { PhaseItem(it.phase, it.durationSeconds, it.round) }
        phaseIndex = session.phaseIndex
        elapsedBeforePauseMillis = session.elapsedBeforePauseMillis.coerceAtLeast(0L)

        if (phaseIndex !in plan.indices) {
            _state.value = TimerState.Idle
            return emptyList()
        }

        val expectedPhase = plan[phaseIndex]
        if (expectedPhase.phase != session.phase || expectedPhase.round != session.currentRound) {
            _state.value = TimerState.Idle
            return emptyList()
        }

        if (session.phaseEndsAtElapsedRealtime <= 0L) {
            sessionStartedAtElapsedRealtime = -1L
            val remainingMillis = session.remainingMillis.coerceAtLeast(0L)
            val paused = TimerState.Paused(
                phase = session.phase,
                remainingSeconds = remainingSeconds(remainingMillis),
                currentRound = session.currentRound,
                totalRounds = session.totalRounds,
                remainingMillis = remainingMillis,
                elapsedBeforePauseMillis = elapsedBeforePauseMillis,
            )
            _state.value = paused
            return listOf(TimerEngineEvent.Paused(paused))
        }

        val now = timeProvider.nowMillis()
        val sessionStart = session.sessionStartedAtElapsedRealtime
        // A persisted elapsedRealtime deadline from a previous boot is invalid.
        if (sessionStart > now + 5_000L) {
            _state.value = TimerState.Idle
            return emptyList()
        }

        sessionStartedAtElapsedRealtime =
            if (sessionStart >= 0L) sessionStart
            else (session.phaseEndsAtElapsedRealtime - durationMillis(plan[phaseIndex]))

        val restored = createRunningState(
            item = plan[phaseIndex],
            phaseStartElapsedRealtime =
                sessionStartedAtElapsedRealtime,
            phaseEndElapsedRealtime =
                session.phaseEndsAtElapsedRealtime,
        )
        _state.value = restored

        val events = mutableListOf<TimerEngineEvent>(
            TimerEngineEvent.Restored(restored)
        )
        return advanceTo(now, events)
    }

    internal fun remainingSeconds(
        endMillis: Long,
        nowMillis: Long = timeProvider.nowMillis(),
    ): Int = remainingSeconds((endMillis - nowMillis).coerceAtLeast(0L))

    private fun remainingSeconds(remainingMillis: Long): Int =
        ((remainingMillis.coerceAtLeast(0L) + 999L) / 1000L).toInt()

    private fun advanceTo(
        now: Long,
        events: MutableList<TimerEngineEvent>,
    ): List<TimerEngineEvent> {
        while (true) {
            val current = _state.value as? TimerState.Running ?: return events
            var phaseEnd = current.phaseEndsAtElapsedRealtime

            if (now < phaseEnd) {
                _state.value = current.copy(
                    remainingSeconds = remainingSeconds(phaseEnd, now),
                    sessionStartedAtElapsedRealtime = sessionStartedAtElapsedRealtime,
                    elapsedBeforePauseMillis = elapsedBeforePauseMillis,
                )
                return events
            }

            // The current phase really ended at its planned boundary. Close the
            // active interval at that boundary (not at the delayed tick time) so
            // total workout time never includes scheduler/CPU lag.
            closeActiveInterval(phaseEnd)

            val nextIndex = phaseIndex + 1
            if (nextIndex >= plan.size) {
                completeFromAlreadyClosedInterval(events)
                return events
            }

            phaseIndex = nextIndex
            val next = plan[phaseIndex]
            val phaseStart = phaseEnd
            phaseEnd += durationMillis(next)
            sessionStartedAtElapsedRealtime = phaseStart

            val running = createRunningState(
                item = next,
                phaseStartElapsedRealtime = phaseStart,
                phaseEndElapsedRealtime = phaseEnd,
            )
            _state.value = running
            events += TimerEngineEvent.PhaseStarted(running)
        }
    }

    private fun createRunningState(
        item: PhaseItem,
        phaseStartElapsedRealtime: Long,
        phaseEndElapsedRealtime: Long,
        exactDurationMillis: Long? = null,
    ): TimerState.Running {
        val duration = exactDurationMillis ?: durationMillis(item)
        return TimerState.Running(
            phase = item.phase,
            remainingSeconds =
                remainingSeconds(phaseEndElapsedRealtime).coerceAtLeast(1).coerceAtMost(
                    ((duration + 999L) / 1000L).toInt().coerceAtLeast(1)
                ),
            currentRound = item.round,
            totalRounds = config?.rounds ?: 0,
            phaseEndsAtElapsedRealtime = phaseEndElapsedRealtime,
            sessionStartedAtElapsedRealtime = phaseStartElapsedRealtime,
            elapsedBeforePauseMillis = elapsedBeforePauseMillis,
        )
    }

    private fun complete(
        completedAtElapsedRealtime: Long,
        events: MutableList<TimerEngineEvent>,
    ) {
        closeActiveInterval(completedAtElapsedRealtime)
        completeFromAlreadyClosedInterval(events)
    }

    private fun completeFromAlreadyClosedInterval(
        events: MutableList<TimerEngineEvent>,
    ) {
        val totalMillis = elapsedBeforePauseMillis
        val completed = TimerState.Completed(
            completedRounds = config?.rounds ?: 0,
            totalRounds = config?.rounds ?: 0,
            totalTimeSeconds = (totalMillis / 1000L).toInt(),
        )
        _state.value = completed
        sessionStartedAtElapsedRealtime = -1L
        events += TimerEngineEvent.Completed(completed)
    }

    private fun closeActiveInterval(atElapsedRealtime: Long) {
        if (sessionStartedAtElapsedRealtime >= 0L) {
            elapsedBeforePauseMillis +=
                (atElapsedRealtime - sessionStartedAtElapsedRealtime).coerceAtLeast(0L)
            sessionStartedAtElapsedRealtime = -1L
        }
    }

    private fun durationMillis(item: PhaseItem): Long =
        item.durationSeconds.coerceAtLeast(1).toLong() * 1000L

    private fun validate(config: TimerConfig) {
        require(config.workSeconds > 0) { "Work duration must be greater than 0" }
        require(config.restSeconds >= 0) { "Rest duration cannot be negative" }
        require(config.rounds > 0) { "Rounds must be greater than 0" }
        require(config.warmUpSeconds >= 0) { "Warm-up duration cannot be negative" }
        require(config.cooldownSeconds >= 0) { "Cooldown duration cannot be negative" }
    }
}
