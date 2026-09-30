package com.example.myintervaltimerproject.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerCuePolicyTest {

    @Test
    fun every_non_empty_phase_is_spoken_at_phase_start() {
        assertTrue(TimerCuePolicy.shouldSpeakPhaseAtStart(TimerPhase.GET_READY, 1, 5))
        assertTrue(TimerCuePolicy.shouldSpeakPhaseAtStart(TimerPhase.WORK, 1, 5))
        assertTrue(TimerCuePolicy.shouldSpeakPhaseAtStart(TimerPhase.REST, 1, 4))
        assertTrue(TimerCuePolicy.shouldSpeakPhaseAtStart(TimerPhase.COOLDOWN, 1, 1))
    }

    @Test
    fun zero_duration_phase_is_not_spoken() {
        assertTrue(!TimerCuePolicy.shouldSpeakPhaseAtStart(TimerPhase.COOLDOWN, 1, 0))
    }

    @Test
    fun normal_phase_has_five_countdown_beeps() {
        assertEquals(listOf(5, 4, 3, 2, 1), TimerCuePolicy.countdownSeconds(30))
    }

    @Test
    fun five_second_phase_uses_voice_for_first_second_and_beeps_four_to_one() {
        assertEquals(listOf(4, 3, 2, 1), TimerCuePolicy.countdownSeconds(5))
    }

    @Test
    fun short_phase_does_not_overlap_voice_at_phase_start() {
        assertEquals(listOf(3, 2, 1), TimerCuePolicy.countdownSeconds(4))
        assertEquals(emptyList<Int>(), TimerCuePolicy.countdownSeconds(1))
        assertEquals(emptyList<Int>(), TimerCuePolicy.countdownSeconds(0))
    }

    @Test
    fun countdown_policy_matches_schedule() {
        assertTrue(TimerCuePolicy.shouldPlayCountdownBeep(5, 30))
        assertTrue(TimerCuePolicy.shouldPlayCountdownBeep(1, 30))
        assertTrue(TimerCuePolicy.shouldPlayCountdownBeep(4, 5))
        assertTrue(TimerCuePolicy.shouldPlayCountdownBeep(1, 5))
        assertTrue(!TimerCuePolicy.shouldPlayCountdownBeep(5, 5))
        assertTrue(!TimerCuePolicy.shouldPlayCountdownBeep(0, 30))
    }
}
