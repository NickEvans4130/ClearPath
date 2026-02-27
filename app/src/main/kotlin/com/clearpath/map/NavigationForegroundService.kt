package com.clearpath.map

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clearpath.MainActivity
import com.clearpath.R

class NavigationForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Navigating…"))
        return START_STICKY
    }

    fun updateNotification(message: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClearPath Navigation")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Navigation",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "ClearPath turn-by-turn navigation"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID      = "clearpath_nav"
        const val NOTIFICATION_ID = 1001
    }
}
