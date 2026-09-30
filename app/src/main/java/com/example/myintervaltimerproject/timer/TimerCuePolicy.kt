package com.example.myintervaltimerproject.timer

/** Pure timing/audio policy used by TimerSessionController. */
object TimerCuePolicy {

    /**
     * Every non-empty phase is announced when it starts.
     *
     * A short phase (including the default 5-second GET_READY) still gets the
     * voice announcement at phase start. Its countdown starts on the next
     * second, so voice and beep are never played at the same instant.
     */
    fun shouldSpeakPhaseAtStart(
        phase: TimerPhase,
        currentRound: Int,
        phaseDurationSeconds: Int,
    ): Boolean = phaseDurationSeconds > 0

    /**
     * Returns the seconds that receive countdown beeps. For a normal phase,
     * the final five seconds are cued. For a phase of exactly five seconds,
     * the first (5-second) cue is intentionally replaced by the phase voice
     * announcement, leaving 4,3,2,1 as the four audible countdown beeps.
     *
     * Examples:
     * 30s -> 5,4,3,2,1
     * 5s  -> 4,3,2,1 ("Get ready"/phase voice at t=0)
     * 4s  -> 3,2,1
     * 1s  -> no beep
     */
    fun countdownSeconds(phaseDurationSeconds: Int): List<Int> =
        when {
            phaseDurationSeconds <= 1 -> emptyList()
            phaseDurationSeconds <= 5 -> (1 until phaseDurationSeconds).reversed().toList()
            else -> listOf(5, 4, 3, 2, 1)
        }

    /**
     * A beep is valid only for a second returned by [countdownSeconds].
     */
    fun shouldPlayCountdownBeep(
        remainingSecond: Int,
        phaseDurationSeconds: Int,
    ): Boolean = remainingSecond in countdownSeconds(phaseDurationSeconds)
}
