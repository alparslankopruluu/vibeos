package com.vibeos.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyDropScheduler {
    private const val UNIQUE_WORK = "vibeos_daily_drop"

    fun schedule(context: Context) {
        val now = ZonedDateTime.now()
        var next = now.withHour(19).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val initialDelay = Duration.between(now, next)
        val request = PeriodicWorkRequestBuilder<DailyDropWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        context.getSharedPreferences("vibeos", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("daily_drop_notifications", true)
            .apply()

        runCatching {
            AppServices.analytics.log(
                "daily_drop_notification_scheduled",
                mapOf("hour" to "19")
            )
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        context.getSharedPreferences("vibeos", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("daily_drop_notifications", false)
            .apply()
    }
}

class DailyDropWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channelId = "daily_drops"
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Daily Drops",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val themeName = runCatching {
            when (AppServices.remoteConfig.dailyDropThemeId) {
                "sakura_night" -> "Sakura Night"
                "midnight_glass" -> "Midnight Glass"
                else -> "your new theme"
            }
        }.getOrDefault("your new theme")

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle("Your Daily Drop is ready ✦")
            .setContentText("$themeName is waiting for you in VibeOS.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$themeName is waiting for you. Open VibeOS to claim today's drop.")
            )
            .setAutoCancel(true)
            .build()

        manager.notify(1900, notification)
        runCatching {
            AppServices.analytics.log("daily_drop_notification_delivered")
        }
        return Result.success()
    }
}
