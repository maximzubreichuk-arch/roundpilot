package com.example.myintervaltimerproject.platform.timer

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.platform.notification.TimerAlarmReceiver
import com.example.myintervaltimerproject.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps timer execution alive while the app is backgrounded/screen-off.
 * The service owns the ticker; TimerEngine owns all timing/state decisions.
 */
class TimerForegroundService : Service() {

    private lateinit var app: IntervalTimerApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var serviceStopping = false

    override fun onCreate() {
        super.onCreate()
        app = application as IntervalTimerApp

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createInitialNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        startTicker()
        observeTerminalState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceStopping = false
        when (intent?.action) {
            ACTION_START -> {
                app.timerSessionController.configurePreferences(app.timerSessionController.currentPreferences())
                if (app.timerSessionController.state.value is TimerState.Idle) {
                    app.timerSessionController.restoreFromStore()
                }
                app.timerSessionController.tick()
            }

            ACTION_ALARM -> {
                // AlarmManager is the safety-net for phase boundaries. Handle the
                // boundary synchronously before returning from onStartCommand so a
                // delayed coroutine cannot leave the notification/audio one phase behind.
                app.timerSessionController.configurePreferences(app.timerSessionController.currentPreferences())
                if (app.timerSessionController.state.value is TimerState.Idle) {
                    app.timerSessionController.restoreFromStore()
                }
                app.timerSessionController.tick()
            }

            ACTION_COUNTDOWN -> {
                app.timerSessionController.configurePreferences(app.timerSessionController.currentPreferences())
                if (app.timerSessionController.state.value is TimerState.Idle) {
                    app.timerSessionController.restoreFromStore()
                }
                val second = intent.getIntExtra(EXTRA_COUNTDOWN_SECOND, -1)
                val phaseEnd = intent.getLongExtra(EXTRA_PHASE_END, -1L)
                if (second in 1..5 && phaseEnd > 0L) {
                    app.timerSessionController.onCountdownAlarm(
                        second = second,
                        expectedPhaseEndElapsedRealtime = phaseEnd,
                    )
                }
            }

            ACTION_STOP -> app.timerSessionController.stop()

            null -> {
                app.timerSessionController.configurePreferences(app.timerSessionController.currentPreferences())
                if (app.timerSessionController.state.value is TimerState.Idle) {
                    app.timerSessionController.restoreFromStore()
                }
                if (app.timerSessionController.state.value is TimerState.Idle) {
                    stopService()
                }
            }
        }

        return START_STICKY
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                app.timerSessionController.tick()
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun observeTerminalState() {
        scope.launch {
            app.timerSessionController.state.collectLatest { state ->
                if (state is TimerState.Completed || state is TimerState.Stopped) {
                    stopService()
                }
            }
        }
    }

    private fun stopService() {
        if (serviceStopping) return
        serviceStopping = true
        tickerJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createInitialNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.timer_running))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

    override fun onDestroy() {
        tickerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.myintervaltimerproject.action.START_TIMER"
        const val ACTION_STOP = "com.example.myintervaltimerproject.action.STOP_TIMER"
        const val ACTION_ALARM = "com.example.myintervaltimerproject.action.ALARM_TIMER"
        const val ACTION_COUNTDOWN = TimerAlarmReceiver.ACTION_COUNTDOWN
        const val EXTRA_COUNTDOWN_SECOND = TimerAlarmReceiver.EXTRA_SECOND
        const val EXTRA_PHASE_END = TimerAlarmReceiver.EXTRA_PHASE_END

        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_INTERVAL_MS = 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerForegroundService::class.java).apply {
                    action = ACTION_START
                },
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TimerForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }

        fun countdown(
            context: Context,
            second: Int,
            expectedPhaseEndElapsedRealtime: Long,
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerForegroundService::class.java).apply {
                    action = ACTION_COUNTDOWN
                    putExtra(EXTRA_COUNTDOWN_SECOND, second)
                    putExtra(EXTRA_PHASE_END, expectedPhaseEndElapsedRealtime)
                },
            )
        }

        fun alarm(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerForegroundService::class.java).apply {
                    action = ACTION_ALARM
                },
            )
        }
    }
}
