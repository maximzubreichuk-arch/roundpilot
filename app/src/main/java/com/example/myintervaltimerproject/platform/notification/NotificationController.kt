package com.example.myintervaltimerproject.platform.notification

import com.example.myintervaltimerproject.data.preferences.AppLanguage

interface NotificationController {
    fun setLanguage(language: AppLanguage)

    fun showRunning(
        phase: String,
        phaseEndsAtElapsedRealtime: Long,
        round: Int,
        totalRounds: Int,
    )

    fun showPaused(
        phase: String,
        seconds: Int,
        round: Int,
        totalRounds: Int,
    )

    fun showCompleted()
    fun cancel()
}
