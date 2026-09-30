package com.example.myintervaltimerproject

import android.app.Application

import com.example.myintervaltimerproject.analytics.AppAnalytics
import com.example.myintervaltimerproject.data.preferences.PreferencesRepository
import com.example.myintervaltimerproject.data.presets.PresetStorage
import com.example.myintervaltimerproject.data.session.ActiveSessionStore

import com.example.myintervaltimerproject.platform.audio.AndroidAudioController
import com.example.myintervaltimerproject.platform.audio.AudioController

import com.example.myintervaltimerproject.platform.notification.AndroidNotificationController
import com.example.myintervaltimerproject.platform.notification.NotificationController
import com.example.myintervaltimerproject.platform.notification.TimerAlarmScheduler

import com.example.myintervaltimerproject.platform.vibration.AndroidVibrationController
import com.example.myintervaltimerproject.platform.vibration.VibrationController

import com.example.myintervaltimerproject.timer.TimerEngine
import com.example.myintervaltimerproject.timer.TimerSessionController


class IntervalTimerApp : Application() {


    // ---------------------------------------------------------
    // ANALYTICS / CRASH DIAGNOSTICS
    // ---------------------------------------------------------

    lateinit var analytics: AppAnalytics
        private set

    // ---------------------------------------------------------
    // PREFERENCES
    // ---------------------------------------------------------

    lateinit var preferences:
            PreferencesRepository
        private set


    // ---------------------------------------------------------
    // PRESETS
    // ---------------------------------------------------------

    lateinit var presets:
            PresetStorage
        private set


    // ---------------------------------------------------------
    // ACTIVE SESSION
    // ---------------------------------------------------------

    lateinit var sessions:
            ActiveSessionStore
        private set


    // ---------------------------------------------------------
    // ALARM SCHEDULER
    // ---------------------------------------------------------

    lateinit var alarmScheduler:
            TimerAlarmScheduler
        private set


    // ---------------------------------------------------------
    // AUDIO
    // ---------------------------------------------------------

    lateinit var audio:
            AudioController
        private set


    // ---------------------------------------------------------
    // VIBRATION
    // ---------------------------------------------------------

    lateinit var vibration:
            VibrationController
        private set


    // ---------------------------------------------------------
    // NOTIFICATIONS
    // ---------------------------------------------------------

    lateinit var notifications:
            NotificationController
        private set


    // ---------------------------------------------------------
    // TIMER ENGINE
    // ---------------------------------------------------------

    lateinit var timerEngine:
            TimerEngine
        private set


    // ---------------------------------------------------------
    // TIMER SESSION CONTROLLER
    // ---------------------------------------------------------

    lateinit var timerSessionController:
            TimerSessionController
        private set


    // ---------------------------------------------------------
    // APPLICATION
    // ---------------------------------------------------------

    override fun onCreate() {

        super.onCreate()

        analytics = AppAnalytics(this)


        // -----------------------------------------------------
        // DATA
        // -----------------------------------------------------

        preferences =
            PreferencesRepository(
                this
            )


        presets =
            PresetStorage(
                this
            )


        sessions =
            ActiveSessionStore(
                this
            )


        // -----------------------------------------------------
        // PLATFORM
        // -----------------------------------------------------

        alarmScheduler =
            TimerAlarmScheduler(
                this
            )


        audio =
            AndroidAudioController(
                this
            )


        vibration =
            AndroidVibrationController(
                this
            )


        notifications =
            AndroidNotificationController(
                this
            )


        // -----------------------------------------------------
        // TIMER
        // -----------------------------------------------------

        /*
         * IMPORTANT:
         *
         * There is ONE TimerEngine for the whole application
         * process.
         *
         * It must NOT be created inside WorkoutViewModelFactory
         * or inside TimerForegroundService.
         *
         * This prevents foreground UI and background service
         * from running two independent timers.
         */
        timerEngine =
            TimerEngine()


        /*
         * There is ONE TimerSessionController connected
         * to the same TimerEngine.
         *
         * Both WorkoutViewModel and TimerForegroundService
         * will use this same controller.
         */
        timerSessionController =
            TimerSessionController(

                engine =
                    timerEngine,

                store =
                    sessions,

                scheduler =
                    alarmScheduler,

                audio =
                    audio,

                vibration =
                    vibration,

                notifications =
                    notifications,

                preferences =
                    preferences.flow
            )
    }
}