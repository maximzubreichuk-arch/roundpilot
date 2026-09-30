package com.example.myintervaltimerproject.analytics

import android.content.Context
import android.os.Bundle

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Small analytics/diagnostics facade.
 *
 * No user identifiers or free-form user-entered text are logged.
 * Firebase calls are guarded so the app remains runnable before a
 * project-specific google-services.json is added.
 */
class AppAnalytics(
    context: Context,
) {

    private val appContext = context.applicationContext

    private val analytics: FirebaseAnalytics?
        get() = runCatching {
            FirebaseAnalytics.getInstance(appContext)
        }.getOrNull()

    private val crashlytics: FirebaseCrashlytics?
        get() = runCatching {
            FirebaseCrashlytics.getInstance()
        }.getOrNull()


    fun logWorkoutStarted(
        source: String,
        workSeconds: Int,
        restSeconds: Int,
        rounds: Int,
    ) {
        log(
            name = "workout_started",
            params = Bundle().apply {
                putString("source", source)
                putInt("work_seconds", workSeconds)
                putInt("rest_seconds", restSeconds)
                putInt("rounds", rounds)
            }
        )
    }

    fun logWorkoutCompleted(
        completedRounds: Int,
        totalRounds: Int,
        totalTimeSeconds: Int,
    ) {
        log(
            name = "workout_completed",
            params = Bundle().apply {
                putInt("completed_rounds", completedRounds)
                putInt("total_rounds", totalRounds)
                putInt("total_time_seconds", totalTimeSeconds)
            }
        )
    }

    fun logWorkoutStopped() = log("workout_stopped")
    fun logWorkoutPaused() = log("workout_paused")
    fun logWorkoutResumed() = log("workout_resumed")
    fun logWorkoutSkipped() = log("workout_skipped")

    fun logPresetCreated() = log("preset_created")
    fun logPresetUpdated() = log("preset_updated")
    fun logPresetDeleted() = log("preset_deleted")
    fun logPresetStarted() = log("preset_started")

    fun logThemeChanged(theme: String) {
        log(
            name = "theme_changed",
            params = Bundle().apply {
                putString("theme", theme)
            }
        )
    }

    fun logLanguageChanged(language: String) {
        log(
            name = "language_changed",
            params = Bundle().apply {
                putString("language", language)
            }
        )
    }

    fun logSettingChanged(
        setting: String,
        enabled: Boolean,
    ) {
        log(
            name = "setting_changed",
            params = Bundle().apply {
                putString("setting", setting)
                putBoolean("enabled", enabled)
            }
        )
    }

    fun setAppContext(
        language: String,
        theme: String,
    ) {
        crashlytics?.apply {
            setCustomKey("app_language", language)
            setCustomKey("theme", theme)
        }
    }

    fun setTimerState(state: String) {
        crashlytics?.setCustomKey("timer_state", state)
    }

    fun recordNonFatal(
        throwable: Throwable,
        context: String,
    ) {
        crashlytics?.apply {
            setCustomKey("error_context", context)
            recordException(throwable)
        }
    }

    private fun log(
        name: String,
        params: Bundle? = null,
    ) {
        analytics?.logEvent(name, params)
    }
}
