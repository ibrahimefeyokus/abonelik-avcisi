package com.ibrah.abonelik.core

import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewFixesTest {
    private val base = ReminderSettings()

    // I2: "kaç gün önce" ya da aç/kapa denemeleri anında telafi yapıp bildirim yağdırmamalı;
    // yalnızca saat değişince (ör. geriye alınınca bugünkü hemen gelsin diye) telafi çalışır.
    @Test fun only_time_change_triggers_catch_up() {
        assertFalse(SettingsChange.needsCatchUp(base, base.copy(subDaysBefore = 7)))
        assertFalse(SettingsChange.needsCatchUp(base, base.copy(dueDaysBefore = 0)))
        assertFalse(SettingsChange.needsCatchUp(base, base.copy(subOn = false, statementOn = false)))
        assertTrue(SettingsChange.needsCatchUp(base, base.copy(time = LocalTime.of(9, 0))))
    }
}
