package com.ibrah.abonelik.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ibrah.abonelik.MainActivity
import com.ibrah.abonelik.R

object Notifications {
    const val CHANNEL = "hatirlatmalar"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Abonelik yenilemeleri, ekstre kesimi ve son ödeme"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            },
        )
    }

    fun enabled(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /** Kilit ekranında yalnızca "Abonelik Avcısı · hatırlatma" görünür; tutar gizli kalır. */
    fun show(ctx: Context, id: Int, title: String, body: String) {
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val public = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Abonelik Avcısı")
            .setContentText("Hatırlatma")
            .build()
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }
}
