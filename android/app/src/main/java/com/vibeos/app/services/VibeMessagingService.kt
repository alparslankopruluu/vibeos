package com.vibeos.app.services

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vibeos.app.MainActivity

class VibeMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        runCatching { AppServices.analytics.log("push_token_refreshed") }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val route = message.data["route"] ?: "discover"
        runCatching { AppServices.analytics.log("push_received", mapOf("route" to route)) }
        showNotification(
            title = message.notification?.title ?: message.data["title"] ?: "VibeOS",
            body = message.notification?.body ?: message.data["body"] ?: "A new theme just dropped.",
            route = route
        )
    }

    private fun showNotification(title: String, body: String, route: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "drops"
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Daily drops & offers", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            data = android.net.Uri.parse("vibeos://$route")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, route.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.star_big_on)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }
}
