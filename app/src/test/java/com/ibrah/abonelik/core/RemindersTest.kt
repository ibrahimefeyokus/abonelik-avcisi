package com.ibrah.abonelik.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemindersTest {
    private val settings = ReminderSettings()
    private fun at(s: String) = LocalDateTime.parse(s)
    private fun sub(id: String, day: Int, paused: Boolean = false) =
        Sub(id, id, 10000, Currency.TRY, null, Period.MONTHLY, day, null, null, 0, paused)
    private val bonus = Card("bonus", "Bonus", 25, 5, 0)

    private fun keys(r: List<Reminder>) = r.map { it.key }

    @Test fun todays_reminder_waits_for_the_set_time() {
        val subs = listOf(sub("netflix", 23)) // yarın yenileniyor → bugün hatırlat
        assertTrue(ReminderPlanner.due(subs, emptyList(), settings, at("2026-11-22T09:59"), emptySet()).isEmpty())
        assertEquals(listOf("SUB:netflix:2026-11-23"),
            keys(ReminderPlanner.due(subs, emptyList(), settings, at("2026-11-22T10:00"), emptySet())))
    }

    @Test fun catch_up_sends_missed_ones_whose_event_has_not_passed() {
        // Telefon 21–24 Kasım kapalı; 25 Kasım 08:00'de açılıyor (henüz 10:00 değil).
        // Kesim 25 Kasım (bugün, ama bugünün saati gelmedi → bekler); son ödeme 5 Aralık (2 gün önce = 3 Aralık → henüz değil).
        // Abonelik 24 Kasım'da yenilendi (olay geçti → gönderilmez); 25 Kasım'da yenilenen var (hatırlatma 24'ünde → geç gönderilir).
        val subs = listOf(sub("past", 24), sub("today", 25))
        val r = ReminderPlanner.due(subs, listOf(bonus), settings, at("2026-11-25T08:00"), emptySet())
        assertEquals(listOf("SUB:today:2026-11-25"), keys(r))
        // 10:00 olunca kesim de gelir; gönderilmiş olan tekrar gelmez
        val r2 = ReminderPlanner.due(subs, listOf(bonus), settings, at("2026-11-25T10:00"), setOf("SUB:today:2026-11-25"))
        assertEquals(listOf("STATEMENT:bonus:2026-11-25"), keys(r2))
    }

    @Test fun due_reminder_two_days_before_and_caught_up_until_due_date() {
        // Son ödeme 5 Aralık; hatırlatma 3 Aralık. Telefon 3–4 Aralık kapalıydı, 5 Aralık 12:00'de açıldı → yine gelir.
        assertEquals(listOf("DUE:bonus:2026-12-05"),
            keys(ReminderPlanner.due(emptyList(), listOf(bonus), settings, at("2026-12-03T10:00"), emptySet())))
        assertEquals(listOf("DUE:bonus:2026-12-05"),
            keys(ReminderPlanner.due(emptyList(), listOf(bonus), settings, at("2026-12-05T12:00"), emptySet())))
        // 6 Aralık: olay geçti → yok
        assertTrue(ReminderPlanner.due(emptyList(), listOf(bonus), settings, at("2026-12-06T12:00"), emptySet()).isEmpty())
    }

    @Test fun lead_is_capped_at_7_days() {
        val s7 = settings.copy(subDaysBefore = 7)
        assertEquals(listOf("SUB:a:2026-11-29"),
            keys(ReminderPlanner.due(listOf(sub("a", 29)), emptyList(), s7, at("2026-11-22T10:00"), emptySet())))
        assertTrue(ReminderPlanner.due(listOf(sub("b", 30)), emptyList(), s7, at("2026-11-22T10:00"), emptySet()).isEmpty())
        assertEquals(7, settings.copy(subDaysBefore = 12).normalized().subDaysBefore)
        assertEquals(0, settings.copy(dueDaysBefore = -3).normalized().dueDaysBefore)
    }

    @Test fun disabled_kinds_and_paused_subs_are_skipped() {
        val off = settings.copy(subOn = false, statementOn = false, dueOn = false)
        assertTrue(ReminderPlanner.due(listOf(sub("a", 23)), listOf(bonus), off, at("2026-11-25T10:00"), emptySet()).isEmpty())
        assertTrue(ReminderPlanner.due(listOf(sub("p", 23, paused = true)), emptyList(), settings, at("2026-11-22T10:00"), emptySet()).isEmpty())
    }

    @Test fun zero_lead_reminds_on_the_day() {
        val s0 = settings.copy(subDaysBefore = 0)
        assertEquals(listOf("SUB:a:2026-11-22"),
            keys(ReminderPlanner.due(listOf(sub("a", 22)), emptyList(), s0, at("2026-11-22T10:00"), emptySet())))
    }

    @Test fun next_alarm_time() {
        assertEquals(at("2026-11-22T10:00"), AlarmTime.next(at("2026-11-22T09:30"), LocalTime.of(10, 0)))
        assertEquals(at("2026-11-23T10:00"), AlarmTime.next(at("2026-11-22T10:00"), LocalTime.of(10, 0)))
        assertEquals(at("2026-11-23T10:00"), AlarmTime.next(at("2026-11-22T18:00"), LocalTime.of(10, 0)))
    }

    @Test fun reminder_date_is_event_minus_lead() {
        val r = ReminderPlanner.due(listOf(sub("a", 23)), emptyList(), settings, at("2026-11-22T10:00"), emptySet()).single()
        assertEquals(LocalDate.parse("2026-11-22"), r.remindDate)
        assertEquals(Kind.SUB, r.kind)
    }
}
