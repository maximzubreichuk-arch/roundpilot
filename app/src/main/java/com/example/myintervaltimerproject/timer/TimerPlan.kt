package com.example.myintervaltimerproject.timer

data class PhaseItem(
    val phase: TimerPhase,
    val durationSeconds: Int,
    val round: Int,
)

fun buildTimerPlan(
    config: TimerConfig
): List<PhaseItem> {

    val result = mutableListOf<PhaseItem>()

    // ---------------------------------------------------------
    // WARM-UP
    // ---------------------------------------------------------

    if (config.warmUpSeconds > 0) {
        result += PhaseItem(
            phase = TimerPhase.GET_READY,
            durationSeconds = config.warmUpSeconds,
            round = 0,
        )
    }

    // ---------------------------------------------------------
    // WORK / REST
    // ---------------------------------------------------------

    for (round in 1..config.rounds) {

        result += PhaseItem(
            phase = TimerPhase.WORK,
            durationSeconds = config.workSeconds,
            round = round,
        )

        /*
         * Rest is needed only between rounds.
         * There is no REST after the last WORK.
         */
        if (
            round < config.rounds &&
            config.restSeconds > 0
        ) {
            result += PhaseItem(
                phase = TimerPhase.REST,
                durationSeconds = config.restSeconds,
                round = round,
            )
        }
    }

    // ---------------------------------------------------------
    // COOLDOWN
    // ---------------------------------------------------------

    if (config.cooldownSeconds > 0) {
        result += PhaseItem(
            phase = TimerPhase.COOLDOWN,
            durationSeconds = config.cooldownSeconds,
            round = config.rounds,
        )
    }

    return result
}