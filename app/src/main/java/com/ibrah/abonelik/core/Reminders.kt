package com.ibrah.abonelik.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class Kind { SUB, STATEMENT, DUE }

data class ReminderSettings(
    val time: LocalTime = LocalTime.of(10, 0),
    val subDaysBefore: Int = 1,
    val dueDaysBefore: Int = 2,
    val subOn: Boolean = true,
    val statementOn: Boolean = true,
    val dueOn: Boolean = true,
) {
    /** Önceden hatırlatma 0–7 gün; telafi penceresi de bu yüzden en fazla 7 gün. */
    fun normalized() = copy(subDaysBefore = subDaysBefore.coerceIn(0, 7), dueDaysBefore = dueDaysBefore.coerceIn(0, 7))
}

/** [eventDate]: yenilenme / kesim / son ödeme günü; [remindDate]: hatırlatmanın gönderileceği gün. */
data class Reminder(val kind: Kind, val itemId: String, val eventDate: LocalDate, val remindDate: LocalDate) {
    val key: String get() = "$kind:$itemId:$eventDate"
}

object ReminderPlanner {
    /**
     * Şu an gönderilmesi gereken hatırlatmalar (telafi dahil): hatırlatma günü geldi ya da geçti, olay günü
     * henüz geçmedi, daha önce gönderilmedi. Bugünün hatırlatması ayarlanan saatten önce gönderilmez.
     */
    fun due(
        subs: List<Sub>, cards: List<Card>, settings: ReminderSettings, now: LocalDateTime, sent: Set<String>,
    ): List<Reminder> {
        val s = settings.normalized()
        val today = now.toLocalDate()
        val timeReached = now.toLocalTime() >= s.time
        val out = mutableListOf<Reminder>()

        if (s.subOn) subs.filterNot { it.paused }.forEach { sub ->
            Schedule.renewalsBetween(sub, today, today.plusDays(s.subDaysBefore.toLong())).forEach { ev ->
                out += Reminder(Kind.SUB, sub.id, ev, ev.minusDays(s.subDaysBefore.toLong()))
            }
        }
        cards.forEach { c ->
            if (s.statementOn) {
                val st = Schedule.nextStatement(c, today)
                if (st == today) out += Reminder(Kind.STATEMENT, c.id, st, st)
            }
            if (s.dueOn) {
                val lead = s.dueDaysBefore.toLong()
                var st = Schedule.nextStatement(c, today.minusMonths(2))
                while (st <= today.plusDays(lead)) {
                    val due = Schedule.dueAfter(c, st)
                    if (due in today..today.plusDays(lead)) out += Reminder(Kind.DUE, c.id, due, due.minusDays(lead))
                    st = Schedule.nextStatement(c, st.plusDays(1))
                }
            }
        }
        return out
            .filter { it.key !in sent }
            .filter { it.remindDate < today || (it.remindDate == today && timeReached) }
            .distinctBy { it.key }
            .sortedWith(compareBy({ it.eventDate }, { it.kind }))
    }
}

object AlarmTime {
    /** Bugün saat henüz gelmediyse bugün, yoksa yarın. */
    fun next(now: LocalDateTime, time: LocalTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (now < today) today else today.plusDays(1)
    }
}
