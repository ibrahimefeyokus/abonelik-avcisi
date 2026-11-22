package com.ibrah.abonelik.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTest {
    @Test fun theme_mode_resolves_against_system() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
    }

    @Test fun channel_id_encodes_sound_and_vibration() {
        assertEquals("hatirlatmalar_s1_v1", Delivery.channelId(sound = true, vibrate = true))
        assertEquals("hatirlatmalar_s0_v1", Delivery.channelId(sound = false, vibrate = true))
        assertEquals("hatirlatmalar_s0_v0", Delivery.channelId(sound = false, vibrate = false))
        assertEquals(4, Delivery.allChannelIds.size)
        assertTrue("hatirlatmalar" in Delivery.legacyChannelIds)
    }

    private val d = LocalDate.parse("2026-11-22")
    private val due = listOf(Reminder(Kind.SUB, "a", d, d), Reminder(Kind.DUE, "b", d, d))

    @Test fun notifications_off_in_app_consumes_silently() {
        val p = Delivery.plan(due, notificationsOn = false, systemAllows = true)
        assertEquals(emptyList<Reminder>(), p.show)
        assertEquals(due, p.markSent) // tekrar açılınca geçmişler yağmasın
    }

    @Test fun system_block_keeps_reminders_for_later() {
        val p = Delivery.plan(due, notificationsOn = true, systemAllows = false)
        assertEquals(emptyList<Reminder>(), p.show)
        assertEquals(emptyList<Reminder>(), p.markSent) // izin verilince telafi edilir
    }

    @Test fun normal_delivery_shows_and_marks() {
        val p = Delivery.plan(due, notificationsOn = true, systemAllows = true)
        assertEquals(due, p.show)
        assertEquals(due, p.markSent)
    }

    @Test fun reminder_settings_defaults() {
        val s = ReminderSettings()
        assertTrue(s.notificationsOn && s.sound && s.vibrate)
    }
}
