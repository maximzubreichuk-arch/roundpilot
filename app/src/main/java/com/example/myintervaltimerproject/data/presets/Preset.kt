package com.example.myintervaltimerproject.data.presets

import com.example.myintervaltimerproject.timer.TimerConfig

data class Preset(
    val id: Long,
    val name: String,
    val config: TimerConfig,
)
