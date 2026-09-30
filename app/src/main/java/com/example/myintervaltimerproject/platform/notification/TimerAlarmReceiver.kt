package com.example.myintervaltimerproject.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.platform.timer.TimerForegroundService

/**
 * AlarmManager is a wake-up mechanism. The receiver forwards the event to the
 * single foreground service/controller; TimerEngine remains the source of truth.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        if (intent?.action == ACTION_COUNTDOWN) {
            // The foreground service may already be alive. Calling the singleton
            // controller directly avoids an extra startForegroundService ->
            // onStartCommand hop, which can add audible latency to a countdown cue.
            val app = appContext as? IntervalTimerApp
            if (app != null) {
                app.timerSessionController.onCountdownAlarm(
                    second = intent.getIntExtra(EXTRA_SECOND, -1),
                    expectedPhaseEndElapsedRealtime =
                        intent.getLongExtra(EXTRA_PHASE_END, -1L),
                )
            }
        } else {
            TimerForegroundService.alarm(appContext)
        }
    }

    companion object {
        const val ACTION_COUNTDOWN =
            "com.example.myintervaltimerproject.action.COUNTDOWN_TIMER"
        const val EXTRA_SECOND =
            "com.example.myintervaltimerproject.extra.COUNTDOWN_SECOND"
        const val EXTRA_PHASE_END =
            "com.example.myintervaltimerproject.extra.PHASE_END"
    }
}
