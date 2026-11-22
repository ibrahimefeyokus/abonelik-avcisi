package com.ibrah.abonelik.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ibrah.abonelik.core.AlarmTime
import com.ibrah.abonelik.core.Kind
import com.ibrah.abonelik.core.ReminderPlanner
import com.ibrah.abonelik.core.Totals
import com.ibrah.abonelik.data.Repository
import com.ibrah.abonelik.data.SettingsStore
import com.ibrah.abonelik.notify.Notifications
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Telafi geçişi: alarm çalınca, telefon açılınca ve uygulama açılınca çalışır. Gönderilmesi gereken
 * hatırlatmaları bildirir, gönderim kaydını yazar ve bir sonraki günlük alarmı kurar.
 */
object CatchUp {
    private val lock = Mutex()

    suspend fun run(context: Context, now: LocalDateTime = LocalDateTime.now()) = lock.withLock {
        val ctx = context.applicationContext
        val repo = Repository.get(ctx)
        val settings = SettingsStore(ctx).current()
        val subs = repo.subsNow()
        val cards = repo.cardsNow()
        // Bildirim izni yoksa hiçbir şey "gönderildi" sayılmaz; izin verilince telafi edilir.
        if (Notifications.enabled(ctx)) {
            val today = now.toLocalDate()
            val due = ReminderPlanner.due(subs, cards, settings, now, repo.sentKeys())
            val shown = due.mapNotNull { r ->
                val sub = subs.firstOrNull { it.id == r.itemId && r.kind == Kind.SUB }
                val card = cards.firstOrNull { it.id == r.itemId && r.kind != Kind.SUB }
                val st = if (r.kind == Kind.STATEMENT && card != null) Totals.onStatement(card, subs, r.eventDate) else null
                val text = ReminderText.of(r, sub, card, st, today) ?: return@mapNotNull r.key // silinmiş öğe: sessizce kapat
                Notifications.show(ctx, r.key.hashCode(), text.first, text.second)
                r.key
            }
            if (shown.isNotEmpty()) repo.markSent(shown, today)
            repo.pruneSent(today.minusDays(30))
        }
        DailyAlarm.schedule(ctx, AlarmTime.next(now, settings.time))
        // Gün değişince "yarın / N gün" etiketleri ve yaklaşanlar widget'ta da güncellensin.
        com.ibrah.abonelik.widget.WidgetUpdater.update(ctx)
    }

    /** Alıcılardan çağrılır: goAsync ile süreç işi bitene kadar canlı kalır. */
    fun runAsync(receiver: BroadcastReceiver, context: Context) {
        val pending = receiver.goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { run(context) } finally { pending.finish() }
        }
    }
}

object DailyAlarm {
    private const val REQUEST = 1001

    fun schedule(ctx: Context, at: LocalDateTime) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // USE_EXACT_ALARM kurulumda kendiliğinden verilir (kullanıcıya izin sorulmaz) → tam saatinde.
        // Herhangi bir nedenle yoksa 10 dakikalık pencereyle kurulur.
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(ctx))
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, millis, 10 * 60_000L, pending(ctx))
        }
    }

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQUEST, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = CatchUp.runAsync(this, context)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) CatchUp.runAsync(this, context)
    }
}
