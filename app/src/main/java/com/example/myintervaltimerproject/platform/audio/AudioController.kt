package com.example.myintervaltimerproject.platform.audio

import com.example.myintervaltimerproject.data.preferences.AppLanguage

interface AudioController {

    fun playStart()

    fun playCountdown()

    fun playPhaseChange()

    fun playFinish()

    fun speakGetReady()

    fun speakWork()

    fun speakRest()

    fun speakCooldown()

    fun setLanguage(
        language: AppLanguage
    )

    fun setMuted(
        muted: Boolean
    )

    fun release()
}