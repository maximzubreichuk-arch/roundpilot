package com.example.myintervaltimerproject.timer

import com.example.myintervaltimerproject.data.session.ActiveSession
import com.example.myintervaltimerproject.data.session.SessionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private class FakeTime(var now: Long = 0L) : TimeProvider {
        override fun nowMillis(): Long = now
    }

    private fun config(
        work: Int = 30,
        rest: Int = 15,
        rounds: Int = 2,
        warm: Int = 0,
        cooldown: Int = 0,
    ) = TimerConfig(work, rest, rounds, warm, cooldown)

    private fun phaseEvents(events: List<TimerEngineEvent>) =
        events.filterIsInstance<TimerEngineEvent.PhaseStarted>()

    @Test
    fun startCreatesFirstPhaseImmediately() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)

        val events = engine.start(config())
        val state = engine.state.value as TimerState.Running

        assertEquals(1, events.size)
        assertTrue(events.single() is TimerEngineEvent.PhaseStarted)
        assertEquals(TimerPhase.WORK, state.phase)
        assertEquals(30, state.remainingSeconds)
        assertEquals(1, state.currentRound)
        assertEquals(2, state.totalRounds)
        assertEquals(30_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(0L, state.sessionStartedAtElapsedRealtime)
    }

    @Test
    fun warmUpCountdownMovesByAbsoluteDeadline() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 30, rest = 15, rounds = 1, warm = 3))

        var state = engine.state.value as TimerState.Running
        assertEquals(TimerPhase.GET_READY, state.phase)
        assertEquals(3, state.remainingSeconds)

        clock.now = 1_100L
        engine.tick()
        state = engine.state.value as TimerState.Running
        assertEquals(2, state.remainingSeconds)

        clock.now = 2_999L
        engine.tick()
        state = engine.state.value as TimerState.Running
        assertEquals(1, state.remainingSeconds)

        clock.now = 3_000L
        val events = engine.tick()
        state = engine.state.value as TimerState.Running
        assertEquals(TimerPhase.WORK, state.phase)
        assertEquals(1, state.currentRound)
        assertEquals(33_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(1, phaseEvents(events).size)
    }

    @Test
    fun exactBoundaryStartsNextPhaseWithoutAddingSchedulerDelay() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 1, rest = 5, rounds = 2))

        clock.now = 1_000L
        val events = engine.tick()
        val rest = engine.state.value as TimerState.Running

        assertEquals(TimerPhase.REST, rest.phase)
        assertEquals(5, rest.remainingSeconds)
        assertEquals(6_000L, rest.phaseEndsAtElapsedRealtime)
        assertEquals(1, phaseEvents(events).size)
    }

    @Test
    fun catchUpEmitsEveryCrossedPhaseAndPreservesAbsoluteTimeline() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 1, rest = 1, rounds = 4))

        // Timeline: Work1 0..1, Rest1 1..2, Work2 2..3, Rest2 3..4, Work3 4..5 ...
        clock.now = 4_500L
        val events = engine.tick()
        val state = engine.state.value as TimerState.Running

        assertEquals(TimerPhase.WORK, state.phase)
        assertEquals(3, state.currentRound)
        assertEquals(5_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(4, phaseEvents(events).size)
    }

    @Test
    fun lateTickNeverProducesNegativeRemainingSeconds() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 15, rest = 15, rounds = 2))

        clock.now = 15_250L
        engine.tick()
        val state = engine.state.value as TimerState.Running

        assertTrue(state.remainingSeconds >= 1)
        assertEquals(TimerPhase.REST, state.phase)
    }

    @Test
    fun zeroRestGoesDirectlyToNextWorkRound() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 1, rest = 0, rounds = 2))

        clock.now = 1_000L
        val events = engine.tick()
        val state = engine.state.value as TimerState.Running

        assertEquals(TimerPhase.WORK, state.phase)
        assertEquals(2, state.currentRound)
        assertEquals(2_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(1, phaseEvents(events).size)

        clock.now = 2_000L
        val completionEvents = engine.tick()
        assertTrue(engine.state.value is TimerState.Completed)
        assertTrue(completionEvents.any { it is TimerEngineEvent.Completed })
    }

    @Test
    fun pauseStoresExactSubSecondRemainingTime() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 30, rest = 15, rounds = 1))

        clock.now = 10_500L
        engine.pause()
        val paused = engine.state.value as TimerState.Paused

        assertEquals(19_500L, paused.remainingMillis)
        assertEquals(20, paused.remainingSeconds)
    }

    @Test
    fun resumeUsesExactPausedDuration() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 30, rest = 15, rounds = 1))

        clock.now = 10_500L
        engine.pause()
        clock.now = 50_000L
        val events = engine.resume()
        val state = engine.state.value as TimerState.Running

        assertEquals(69_500L, state.phaseEndsAtElapsedRealtime)
        assertEquals(50_000L, state.sessionStartedAtElapsedRealtime)
        assertTrue(events.single() is TimerEngineEvent.Resumed)
    }

    @Test
    fun skipStartsNextPhaseAtCurrentMonotonicTime() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 10, rest = 5, rounds = 2))

        clock.now = 4_000L
        val events = engine.skip()
        val state = engine.state.value as TimerState.Running

        assertEquals(TimerPhase.REST, state.phase)
        assertEquals(9_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(1, phaseEvents(events).size)
    }

    @Test
    fun cooldownIsARealFinalPhase() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 1, rest = 0, rounds = 1, cooldown = 10))

        clock.now = 1_000L
        val events = engine.tick()
        val state = engine.state.value as TimerState.Running

        assertEquals(TimerPhase.COOLDOWN, state.phase)
        assertEquals(11_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(1, phaseEvents(events).size)
    }

    @Test
    fun completedTimeCountsActualWorkoutDurationWithoutRestAfterLastRound() {
        val clock = FakeTime()
        val engine = TimerEngine(clock)
        engine.start(config(work = 10, rest = 5, rounds = 1))

        clock.now = 10_000L
        val events = engine.tick()
        val completed = engine.state.value as TimerState.Completed

        assertEquals(10, completed.totalTimeSeconds)
        assertEquals(1, completed.completedRounds)
        assertTrue(events.any { it is TimerEngineEvent.Completed })
    }

    @Test
    fun restoreRunningSessionCatchesUpAgainstMonotonicDeadline() {
        val clock = FakeTime(10_500L)
        val engine = TimerEngine(clock)
        val cfg = config(work = 5, rest = 5, rounds = 2)
        val phases = buildTimerPlan(cfg).map {
            SessionPhase(it.phase, it.durationSeconds, it.round)
        }

        val events = engine.restore(
            ActiveSession(
                config = cfg,
                phase = TimerPhase.WORK,
                currentRound = 1,
                totalRounds = 2,
                remainingSeconds = 5,
                phaseEndsAtElapsedRealtime = 5_000L,
                phases = phases,
                phaseIndex = 0,
                remainingMillis = 5_000L,
                sessionStartedAtElapsedRealtime = 0L,
                elapsedBeforePauseMillis = 0L,
            )
        )
        val state = engine.state.value as TimerState.Running

        assertEquals(TimerPhase.WORK, state.phase)
        assertEquals(2, state.currentRound)
        assertEquals(15_000L, state.phaseEndsAtElapsedRealtime)
        assertEquals(1, events.count { it is TimerEngineEvent.Restored })
        assertEquals(2, phaseEvents(events).size)
    }

    @Test
    fun restorePausedSessionPreservesExactMilliseconds() {
        val clock = FakeTime(50_000L)
        val engine = TimerEngine(clock)
        val cfg = config(work = 30, rest = 15, rounds = 1)
        val phases = buildTimerPlan(cfg).map {
            SessionPhase(it.phase, it.durationSeconds, it.round)
        }

        engine.restore(
            ActiveSession(
                config = cfg,
                phase = TimerPhase.WORK,
                currentRound = 1,
                totalRounds = 1,
                remainingSeconds = 20,
                phaseEndsAtElapsedRealtime = 0L,
                phases = phases,
                phaseIndex = 0,
                remainingMillis = 19_500L,
                sessionStartedAtElapsedRealtime = 0L,
                elapsedBeforePauseMillis = 10_500L,
            )
        )

        val paused = engine.state.value as TimerState.Paused
        assertEquals(19_500L, paused.remainingMillis)
        assertEquals(20, paused.remainingSeconds)
    }

    @Test
    fun restoreRejectsPhaseIndexAndPhaseMismatch() {
        val clock = FakeTime(1_000L)
        val engine = TimerEngine(clock)
        val cfg = config(work = 5, rest = 5, rounds = 2)
        val phases = buildTimerPlan(cfg).map {
            SessionPhase(it.phase, it.durationSeconds, it.round)
        }

        engine.restore(
            ActiveSession(
                config = cfg,
                phase = TimerPhase.REST,
                currentRound = 1,
                totalRounds = 2,
                remainingSeconds = 5,
                phaseEndsAtElapsedRealtime = 5_000L,
                phases = phases,
                phaseIndex = 0,
                remainingMillis = 5_000L,
            )
        )

        assertFalse(engine.state.value is TimerState.Running)
    }
}
