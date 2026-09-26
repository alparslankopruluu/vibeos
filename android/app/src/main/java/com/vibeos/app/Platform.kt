package com.vibeos.app

import android.Manifest
import android.app.*
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.LocalDate

object Notifications {
    const val CHANNEL = "daily_drop"
    fun channel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(CHANNEL, "New themes and daily drops", NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun show(context: Context, title: String, body: String, themeId: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        channel(context)
        val intent = Intent(context, MainActivity::class.java).apply { putExtra("theme_id", themeId); flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(context).notify(101, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pending).build())
    }
    fun scheduleDaily(context: Context) {
        val next = java.time.ZonedDateTime.now().withHour(11).withMinute(0).withSecond(0).let { if (it.isBefore(java.time.ZonedDateTime.now())) it.plusDays(1) else it }
        val pending = PendingIntent.getBroadcast(context, 31, Intent(context, DailyReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setInexactRepeating(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pending)
    }
}
class DailyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.getSharedPreferences("vibe", 0).getBoolean("daily_notifications", false)) return
        val theme = Catalog.daily(context, Catalog.bundled(context))
        Notifications.show(context, "Today's drop is here", "${theme.title} is ready to explore", theme.id)
    }
}
class VibeMessaging : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val id = message.data["theme_id"] ?: "sakura-drift"
        val type = message.data["type"] ?: "theme"
        if (type !in setOf("theme", "daily_drop", "offer")) return
        Notifications.show(this, message.notification?.title ?: "A new look is here", message.notification?.body ?: "Open VibeOS to explore", id)
    }
    override fun onNewToken(token: String) { /* Firebase manages token transport; target only consented audiences. */ }
}
class VibeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val theme = Catalog.daily(context, Catalog.bundled(context))
        ids.forEach { id ->
            val view = RemoteViews(context.packageName, R.layout.widget)
            view.setTextViewText(R.id.widget_name, theme.title)
            val intent = Intent(context, MainActivity::class.java).putExtra("theme_id", theme.id)
            view.setOnClickPendingIntent(R.id.widget_name, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, view)
        }
    }
}
