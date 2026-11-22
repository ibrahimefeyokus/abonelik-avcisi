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
import com.ibrah.abonelik.core.Delivery
import com.ibrah.abonelik.core.ReminderSettings

object Notifications {
    /**
     * Ses/titreşim ayarına uygun kanalı oluşturur, diğerlerini siler (telefonun ayarlarında tek
     * "Hatırlatmalar" kanalı görünsün). Android kanal sesini sonradan değiştirmeye izin vermediği için
     * her birleşim ayrı kanaldır.
     */
    fun ensureChannel(ctx: Context, s: ReminderSettings): String {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val id = Delivery.channelId(s.sound, s.vibrate)
        (Delivery.allChannelIds + Delivery.legacyChannelIds).filter { it != id }.forEach { nm.deleteNotificationChannel(it) }
        nm.createNotificationChannel(
            NotificationChannel(id, "Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Abonelik yenilemeleri, ekstre kesimi ve son ödeme"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                if (!s.sound) setSound(null, null)
                enableVibration(s.vibrate)
                if (s.vibrate) vibrationPattern = longArrayOf(0, 250, 150, 250)
            },
        )
        return id
    }

    /** Telefon tarafı izin veriyor mu: uygulama bildirimleri açık VE kanal kapatılmamış. */
    fun systemAllows(ctx: Context, s: ReminderSettings): Boolean {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false
        val id = ensureChannel(ctx, s)
        val ch = ctx.getSystemService(NotificationManager::class.java).getNotificationChannel(id)
        return ch == null || ch.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Kilit ekranında yalnızca "Abonelik Avcısı · hatırlatma" görünür; tutar gizli kalır. */
    fun show(ctx: Context, s: ReminderSettings, id: Int, title: String, body: String) {
        val channel = ensureChannel(ctx, s)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val public = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Abonelik Avcısı")
            .setContentText("Hatırlatma")
            .build()
        val n = NotificationCompat.Builder(ctx, channel)
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
