package com.skofqq.domainmanager.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.ui.MainActivity
import kotlin.random.Random

/**
 * Small wrapper around the one notification channel this app uses (background
 * router-monitor alerts: WAN IP change, disk space, latency degradation — see
 * RouterMonitorWorker). Every post checks POST_NOTIFICATIONS (API 33+) itself,
 * so a caller that forgot to check permission just silently doesn't notify
 * instead of crashing.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "router_monitor"
    private const val REQUEST_CODE_OPEN_APP = 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    /** True when the app can actually post (permission granted or not required pre-33). */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /** Posts a simple text notification opening MainActivity on tap. No-op if permission is missing. */
    fun notify(context: Context, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Reuses the app's own adaptive-icon monochrome mark — already a
            // single-color silhouette suitable for status-bar tinting.
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // Random id: several distinct alerts (WAN change, disk, latency) must not
        // overwrite each other if they fire in the same worker run.
        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}
