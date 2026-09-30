package com.example.myintervaltimerproject.platform.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class TimerAlarmScheduler(context: Context) {

    private val context = context.applicationContext

    private val alarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun scheduleAtElapsedRealtime(endElapsedRealtime: Long) {
        if (endElapsedRealtime <= SystemClock.elapsedRealtime()) return

        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endElapsedRealtime,
                createBoundaryPendingIntent(),
            )
        }
    }

    /** Schedules one exact countdown cue. */
    fun scheduleCountdownAtElapsedRealtime(
        triggerAtElapsedRealtime: Long,
        second: Int,
        expectedPhaseEndElapsedRealtime: Long,
    ) {
        if (triggerAtElapsedRealtime <= SystemClock.elapsedRealtime()) return

        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedRealtime,
                createCountdownPendingIntent(second, expectedPhaseEndElapsedRealtime),
            )
        }
    }

    fun cancelCountdowns() {
        for (second in 1..MAX_COUNTDOWN_SECONDS) {
            runCatching {
                createExistingCountdownPendingIntent(second)?.let(alarmManager::cancel)
            }
        }
    }

    fun cancel() {
        runCatching { alarmManager.cancel(createBoundaryPendingIntent()) }
        cancelCountdowns()
    }

    private fun createBoundaryPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BOUNDARY,
            Intent(context, TimerAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createExistingCountdownPendingIntent(second: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_COUNTDOWN_BASE + second,
            Intent(context, TimerAlarmReceiver::class.java).apply {
                action = TimerAlarmReceiver.ACTION_COUNTDOWN
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createCountdownPendingIntent(
        second: Int,
        expectedPhaseEndElapsedRealtime: Long,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_COUNTDOWN_BASE + second,
            Intent(context, TimerAlarmReceiver::class.java).apply {
                action = TimerAlarmReceiver.ACTION_COUNTDOWN
                putExtra(TimerAlarmReceiver.EXTRA_SECOND, second)
                putExtra(
                    TimerAlarmReceiver.EXTRA_PHASE_END,
                    expectedPhaseEndElapsedRealtime,
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val REQUEST_CODE_BOUNDARY = 7311
        const val REQUEST_CODE_COUNTDOWN_BASE = 7320
        const val MAX_COUNTDOWN_SECONDS = 5
    }
}
