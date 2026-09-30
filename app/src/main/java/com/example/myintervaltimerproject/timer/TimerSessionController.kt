package com.example.myintervaltimerproject.timer

import com.example.myintervaltimerproject.data.preferences.AppPreferences
import com.example.myintervaltimerproject.data.session.ActiveSession
import com.example.myintervaltimerproject.data.session.ActiveSessionStore
import com.example.myintervaltimerproject.data.session.SessionPhase
import com.example.myintervaltimerproject.platform.audio.AudioController
import com.example.myintervaltimerproject.platform.notification.NotificationController
import com.example.myintervaltimerproject.platform.notification.TimerAlarmScheduler
import com.example.myintervaltimerproject.platform.vibration.VibrationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Serializes timer commands and side effects.
 *
 * The UI never drives time directly. The foreground service calls [tick],
 * the engine returns every phase transition that occurred, and this class
 * applies audio/vibration/persistence/notification effects for each transition.
 */
class TimerSessionController(
    private val engine: TimerEngine,
    private val store: ActiveSessionStore,
    private val scheduler: TimerAlarmScheduler,
    private val audio: AudioController,
    private val vibration: VibrationController,
    private val notifications: NotificationController,
    private val preferences: Flow<AppPreferences>,
) {

    /** Public observable state for the UI. Internal controller reads use the engine directly. */
    val state: StateFlow<TimerState> = engine.state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var preferencesState = AppPreferences()

    @Volatile
    private var sessionSoundEnabled = true

    val isSessionSoundEnabled: Boolean
        get() = sessionSoundEnabled

    fun currentPreferences(): AppPreferences = preferencesState

    private var currentConfig: TimerConfig? = null

    /** Prevents duplicate cues when the local scheduler and AlarmManager race. */
    private var lastCountdownSecond: Int? = null

    /**
     * Low-overhead in-process scheduler used while the app process is alive.
     * AlarmManager remains the OS wake-up safety net for process/Doze cases.
     */
    private val countdownExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "RoundPilot-countdown").apply { isDaemon = true }
    }
    private val countdownFutures = mutableListOf<ScheduledFuture<*>>()

    init {
        scope.launch {
            preferences.collect { prefs ->
                preferencesState = prefs
                audio.setLanguage(prefs.language)
                notifications.setLanguage(prefs.language)
            }
        }
    }

    suspend fun configurePreferences() {
        val prefs = preferences.first()
        configurePreferences(prefs)
    }

    @Synchronized
    fun configurePreferences(prefs: AppPreferences) {
        preferencesState = prefs
        audio.setLanguage(prefs.language)
        notifications.setLanguage(prefs.language)
    }

    @Synchronized
    fun setSessionSoundEnabled(enabled: Boolean) {
        val effectiveEnabled = enabled && preferencesState.soundEnabled
        sessionSoundEnabled = effectiveEnabled
        audio.setMuted(!effectiveEnabled)
        store.load()?.let { store.save(it.copy(soundEnabled = effectiveEnabled)) }
    }

    @Synchronized
    fun start(config: TimerConfig) {
        currentConfig = config
        sessionSoundEnabled = preferencesState.soundEnabled
        audio.setMuted(!sessionSoundEnabled)
        scheduler.cancel()
        process(engine.start(config))
    }

    @Synchronized
    fun pause() {
        process(engine.pause())
    }

    @Synchronized
    fun resume() {
        process(engine.resume())
    }

    @Synchronized
    fun skip() {
        process(engine.skip())
    }

    @Synchronized
    fun stop() {
        process(engine.stop())
    }

    /** Called by the foreground service watchdog/ticker. */
    @Synchronized
    fun tick() {
        // Ticks reconcile the engine and phase boundaries only. Countdown audio
        // is NOT generated from this polling loop: doing so makes a beep depend
        // on when a 200 ms coroutine happens to be scheduled.
        process(engine.tick())
    }

    @Synchronized
    fun restoreFromStore() {
        val session = store.load() ?: return
        currentConfig = session.config
        sessionSoundEnabled = session.soundEnabled && preferencesState.soundEnabled
        audio.setMuted(!sessionSoundEnabled)
        process(engine.restore(session))
    }

    @Synchronized
    fun close() {
        cancelCountdownScheduling()
        scheduler.cancel()
        engine.close()
        scope.cancel()
        countdownExecutor.shutdownNow()
    }

    private fun process(events: List<TimerEngineEvent>) {
        events.forEach { event ->
            when (event) {
                is TimerEngineEvent.PhaseStarted -> handlePhaseStarted(event.state)
                is TimerEngineEvent.Resumed -> handleResumed(event.state)
                is TimerEngineEvent.Restored -> handleRestored(event.state)
                is TimerEngineEvent.Paused -> handlePaused(event.state)
                is TimerEngineEvent.Completed -> handleCompleted(event.state)
                TimerEngineEvent.Stopped -> handleStopped()
            }
        }
    }

    private fun handlePhaseStarted(state: TimerState.Running) {
        lastCountdownSecond = null
        currentConfig?.let { config ->
            val plan = buildTimerPlan(config)
            val index = plan.indexOfFirst {
                it.phase == state.phase && it.round == state.currentRound
            }
            if (index >= 0) {
                store.save(
                    ActiveSession(
                        config = config,
                        phase = state.phase,
                        currentRound = state.currentRound,
                        totalRounds = state.totalRounds,
                        remainingSeconds = state.remainingSeconds,
                        phaseEndsAtElapsedRealtime = state.phaseEndsAtElapsedRealtime,
                        phases = plan.map { SessionPhase(it.phase, it.durationSeconds, it.round) },
                        phaseIndex = index,
                        remainingMillis = state.remainingSeconds.coerceAtLeast(0) * 1000L,
                        sessionStartedAtElapsedRealtime = state.sessionStartedAtElapsedRealtime,
                        elapsedBeforePauseMillis = state.elapsedBeforePauseMillis,
                        soundEnabled = sessionSoundEnabled,
                    )
                )
            }
        }

        scheduler.scheduleAtElapsedRealtime(state.phaseEndsAtElapsedRealtime)
        scheduleCountdownCues(state, emitImmediateCue = true)

        notifications.showRunning(
            phase = state.phase.name,
            phaseEndsAtElapsedRealtime = state.phaseEndsAtElapsedRealtime,
            round = state.currentRound,
            totalRounds = state.totalRounds,
        )

        if (canPlaySound() && TimerCuePolicy.shouldSpeakPhaseAtStart(
                phase = state.phase,
                currentRound = state.currentRound,
                phaseDurationSeconds = phaseDurationSeconds(state.phase),
            )
        ) {
            when (state.phase) {
                TimerPhase.GET_READY -> audio.speakGetReady()
                TimerPhase.WORK -> audio.speakWork()
                TimerPhase.REST -> audio.speakRest()
                TimerPhase.COOLDOWN -> audio.speakCooldown()
            }
        }

        if (preferencesState.vibrationEnabled) {
            vibration.phaseChange()
        }
    }

    private fun handleRestored(state: TimerState.Running) {
        lastCountdownSecond = null
        persistRunning(state)
        scheduler.scheduleAtElapsedRealtime(state.phaseEndsAtElapsedRealtime)
        scheduleCountdownCues(state, emitImmediateCue = false)
        notifications.showRunning(
            phase = state.phase.name,
            phaseEndsAtElapsedRealtime = state.phaseEndsAtElapsedRealtime,
            round = state.currentRound,
            totalRounds = state.totalRounds,
        )
    }

    private fun handleResumed(state: TimerState.Running) {
        lastCountdownSecond = null
        persistRunning(state)
        scheduler.scheduleAtElapsedRealtime(state.phaseEndsAtElapsedRealtime)
        scheduleCountdownCues(state, emitImmediateCue = false)
        notifications.showRunning(
            phase = state.phase.name,
            phaseEndsAtElapsedRealtime = state.phaseEndsAtElapsedRealtime,
            round = state.currentRound,
            totalRounds = state.totalRounds,
        )
    }

    /**
     * Schedules the five final cues from the same monotonic phase deadline.
     * The in-process scheduler gives low-jitter playback while the foreground
     * service is alive; AlarmManager is kept as a wake-up fallback.
     */
    private fun scheduleCountdownCues(
        state: TimerState.Running,
        emitImmediateCue: Boolean,
    ) {
        val duration = phaseDurationSeconds(state.phase)
        val phaseEnd = state.phaseEndsAtElapsedRealtime
        val seconds = TimerCuePolicy.countdownSeconds(duration)
        val now = monotonicNow()

        scheduler.cancelCountdowns()
        countdownFutures.forEach { it.cancel(false) }
        countdownFutures.clear()

        seconds.forEach { second ->
            val target = phaseEnd - second * 1000L

            // Keep AlarmManager as the OS-level safety net.
            scheduler.scheduleCountdownAtElapsedRealtime(
                triggerAtElapsedRealtime = target,
                second = second,
                expectedPhaseEndElapsedRealtime = phaseEnd,
            )

            val delayMillis = target - now
            when {
                delayMillis > 0L -> {
                    countdownFutures += countdownExecutor.schedule(
                        {
                            onCountdownAlarm(
                                second = second,
                                expectedPhaseEndElapsedRealtime = phaseEnd,
                            )
                        },
                        delayMillis,
                        TimeUnit.MILLISECONDS,
                    )
                }
                emitImmediateCue && second == seconds.firstOrNull() -> {
                    // A 5-second phase starts exactly at its first cue. There is
                    // no future instant at which that cue could be scheduled.
                    playCountdownCue(
                        second = second,
                        expectedPhaseEndElapsedRealtime = phaseEnd,
                    )
                }
            }
        }
    }

    /** Called by AlarmManager or by the in-process low-jitter scheduler. */
    @Synchronized
    fun onCountdownAlarm(
        second: Int,
        expectedPhaseEndElapsedRealtime: Long,
    ) {
        playCountdownCue(
            second = second,
            expectedPhaseEndElapsedRealtime = expectedPhaseEndElapsedRealtime,
        )
    }

    @Synchronized
    private fun playCountdownCue(
        second: Int,
        expectedPhaseEndElapsedRealtime: Long,
    ) {
        val current = engine.state.value as? TimerState.Running ?: return
        if (current.phaseEndsAtElapsedRealtime != expectedPhaseEndElapsedRealtime) return

        val duration = phaseDurationSeconds(current.phase)
        if (!TimerCuePolicy.shouldPlayCountdownBeep(second, duration)) return
        if (lastCountdownSecond == second) return

        lastCountdownSecond = second

        if (preferencesState.countdownEnabled && canPlaySound()) {
            audio.playCountdown()
        }
        if (preferencesState.vibrationEnabled) {
            vibration.phaseChange()
        }
    }

    private fun cancelCountdownScheduling() {
        scheduler.cancelCountdowns()
        countdownFutures.forEach { it.cancel(false) }
        countdownFutures.clear()
        lastCountdownSecond = null
    }

    private fun phaseDurationSeconds(phase: TimerPhase): Int =
        when (phase) {
            TimerPhase.GET_READY -> currentConfig?.warmUpSeconds ?: 0
            TimerPhase.WORK -> currentConfig?.workSeconds ?: 0
            TimerPhase.REST -> currentConfig?.restSeconds ?: 0
            TimerPhase.COOLDOWN -> currentConfig?.cooldownSeconds ?: 0
        }

    private fun handlePaused(state: TimerState.Paused) {
        cancelCountdownScheduling()
        scheduler.cancel()
        val config = currentConfig ?: return
        val plan = buildTimerPlan(config)
        val index = plan.indexOfFirst {
            it.phase == state.phase && it.round == state.currentRound
        }
        if (index >= 0) {
            store.save(
                ActiveSession(
                    config = config,
                    phase = state.phase,
                    currentRound = state.currentRound,
                    totalRounds = state.totalRounds,
                    remainingSeconds = state.remainingSeconds,
                    phaseEndsAtElapsedRealtime = 0L,
                    phases = plan.map { SessionPhase(it.phase, it.durationSeconds, it.round) },
                    phaseIndex = index,
                    remainingMillis = state.remainingMillis,
                    sessionStartedAtElapsedRealtime = 0L,
                    elapsedBeforePauseMillis = state.elapsedBeforePauseMillis,
                    soundEnabled = sessionSoundEnabled,
                )
            )
        }
        notifications.showPaused(
            phase = state.phase.name,
            seconds = state.remainingSeconds,
            round = state.currentRound,
            totalRounds = state.totalRounds,
        )
    }

    private fun handleCompleted(state: TimerState.Completed) {
        cancelCountdownScheduling()
        scheduler.cancel()
        store.clear()
        notifications.showCompleted()
        if (canPlaySound()) audio.playFinish()
        if (preferencesState.vibrationEnabled) vibration.phaseChange()
        audio.setMuted(false)
        sessionSoundEnabled = true
        currentConfig = null
    }

    private fun handleStopped() {
        cancelCountdownScheduling()
        scheduler.cancel()
        store.clear()
        notifications.cancel()
        audio.setMuted(false)
        sessionSoundEnabled = true
        currentConfig = null
    }

    private fun persistRunning(state: TimerState.Running) {
        val config = currentConfig ?: return
        val plan = buildTimerPlan(config)
        val index = plan.indexOfFirst {
            it.phase == state.phase && it.round == state.currentRound
        }
        if (index < 0) return
        store.save(
            ActiveSession(
                config = config,
                phase = state.phase,
                currentRound = state.currentRound,
                totalRounds = state.totalRounds,
                remainingSeconds = state.remainingSeconds,
                phaseEndsAtElapsedRealtime = state.phaseEndsAtElapsedRealtime,
                phases = plan.map { SessionPhase(it.phase, it.durationSeconds, it.round) },
                phaseIndex = index,
                remainingMillis = (state.phaseEndsAtElapsedRealtime - monotonicNow()).coerceAtLeast(0L),
                sessionStartedAtElapsedRealtime = state.sessionStartedAtElapsedRealtime,
                elapsedBeforePauseMillis = state.elapsedBeforePauseMillis,
                soundEnabled = sessionSoundEnabled,
            )
        )
    }

    private fun monotonicNow(): Long = android.os.SystemClock.elapsedRealtime()

    private fun canPlaySound(): Boolean =
        preferencesState.soundEnabled && sessionSoundEnabled
}
