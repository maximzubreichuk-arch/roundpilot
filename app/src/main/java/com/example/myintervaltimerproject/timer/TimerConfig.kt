package com.example.myintervaltimerproject.timer

data class TimerConfig(
    val workSeconds: Int = 30,
    val restSeconds: Int = 15,
    val rounds: Int = 8,
    val warmUpSeconds: Int = 5,
    val cooldownSeconds: Int = 0,
)
