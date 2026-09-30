package com.example.myintervaltimerproject.timer

sealed interface TimerState {

    data object Idle : TimerState

    data class Running(
        val phase: TimerPhase,
        val remainingSeconds: Int,
        val currentRound: Int,
        val totalRounds: Int,
        val phaseEndsAtElapsedRealtime: Long,
        val sessionStartedAtElapsedRealtime: Long = 0L,
        val elapsedBeforePauseMillis: Long = 0L,
    ) : TimerState

    data class Paused(
        val phase: TimerPhase,
        val remainingSeconds: Int,
        val currentRound: Int,
        val totalRounds: Int,
        val remainingMillis: Long = remainingSeconds.coerceAtLeast(0) * 1000L,
        val elapsedBeforePauseMillis: Long = 0L,
    ) : TimerState

    data class Completed(
        val completedRounds: Int,
        val totalRounds: Int,
        val totalTimeSeconds: Int,
    ) : TimerState

    data object Stopped : TimerState
}
