package com.example.myintervaltimerproject.platform.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myintervaltimerproject.MainActivity
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.data.preferences.AppLanguage

class AndroidNotificationController(context: Context) : NotificationController {

    private val context = context.applicationContext

    @Volatile
    private var currentLanguage = AppLanguage.ENGLISH

    init { createChannel() }

    override fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        createChannel()
    }

    override fun showRunning(
        phase: String,
        phaseEndsAtElapsedRealtime: Long,
        round: Int,
        totalRounds: Int,
    ) {
        if (!hasNotificationPermission()) return

        val localized = localizedContext()
        val phaseText = localized.getStringForPhase(phase)
        val roundText = localized.getString(R.string.round_format, round, totalRounds)
        val nowElapsed = SystemClock.elapsedRealtime()
        val remainingMillis = (phaseEndsAtElapsedRealtime - nowElapsed).coerceAtLeast(0L)
        // Notification.when is wall-clock time. Android internally converts it to the
        // elapsedRealtime timebase for its Chronometer, so convert our monotonic deadline.
        val chronometerBaseWallClock = System.currentTimeMillis() + remainingMillis

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("${localized.getString(R.string.app_name)} • $phaseText")
            .setContentText(roundText)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(chronometerBaseWallClock)
            .setShowWhen(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    override fun showPaused(
        phase: String,
        seconds: Int,
        round: Int,
        totalRounds: Int,
    ) {
        if (!hasNotificationPermission()) return

        val localized = localizedContext()
        val phaseText = localized.getStringForPhase(phase)
        val roundText = localized.getString(R.string.round_format, round, totalRounds)
        val safeSeconds = seconds.coerceAtLeast(0)

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("${localized.getString(R.string.paused)} • $phaseText")
            .setContentText("${format(safeSeconds)} • $roundText")
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    override fun showCompleted() {
        if (!hasNotificationPermission()) return

        val localized = localizedContext()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(localized.getString(R.string.workout_complete))
            .setContentText(localized.getString(R.string.nice_work))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    override fun cancel() = NotificationManagerCompat.from(context).cancel(ID)

    private fun localizedContext(): Context {
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.setLocale(
            when (currentLanguage) {
                AppLanguage.ENGLISH -> java.util.Locale.US
                AppLanguage.RUSSIAN -> java.util.Locale("ru", "RU")
            }
        )
        return context.createConfigurationContext(configuration)
    }

    private fun Context.getStringForPhase(phase: String): String = when (phase.uppercase()) {
        "GET_READY" -> getString(R.string.get_ready)
        "WORK" -> getString(R.string.work)
        "REST" -> getString(R.string.rest)
        "COOLDOWN" -> getString(R.string.cooldown)
        else -> phase
    }

    private fun format(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        7312,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        val channelName = when (currentLanguage) {
            AppLanguage.ENGLISH -> context.getString(com.example.myintervaltimerproject.R.string.app_name)
            AppLanguage.RUSSIAN -> context.getString(com.example.myintervaltimerproject.R.string.app_name)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, channelName, NotificationManager.IMPORTANCE_LOW)
        )
    }

    private companion object {
        const val CHANNEL = "timer"
        const val ID = 1001
    }
}
