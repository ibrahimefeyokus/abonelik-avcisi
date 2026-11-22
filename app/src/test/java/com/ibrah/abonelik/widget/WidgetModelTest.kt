package com.ibrah.abonelik.widget

import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Sub
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTest {
    private val today = LocalDate.parse("2026-11-22")
    private fun sub(name: String, day: Int, kurus: Long = 22999) =
        Sub(name, name, kurus, Currency.TRY, null, Period.MONTHLY, day, null, null, 0, false)

    @Test fun shows_total_and_first_two_upcoming() {
        val m = WidgetModel.build(
            listOf(sub("Netflix", 23), sub("Spotify", 30, 9900), sub("YouTube", 25, 7999)),
            listOf(Card("b", "Bonus", 24, 5, 0)), today,
        )
        assertEquals("408,98 TL", m.total) // 229,99 + 99,00 + 79,99
        assertEquals(
            listOf(WidgetRow("Netflix", "yarın", "229,99 TL"), WidgetRow("Bonus kesim", "2 gün", null)),
            m.rows,
        )
        assertTrue(!m.empty)
    }

    @Test fun empty_when_no_subscriptions() {
        val m = WidgetModel.build(emptyList(), emptyList(), today)
        assertTrue(m.empty)
        assertEquals("0,00 TL", m.total)
        assertEquals(emptyList<WidgetRow>(), m.rows)
    }
}
