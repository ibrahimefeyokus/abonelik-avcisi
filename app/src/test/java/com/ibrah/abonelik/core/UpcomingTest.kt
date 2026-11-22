package com.ibrah.abonelik.core

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcomingTest {
    private val today = LocalDate.parse("2026-11-22")
    private fun sub(id: String, day: Int, paused: Boolean = false, card: String? = null) =
        Sub(id, id, 10000, Currency.TRY, null, Period.MONTHLY, day, null, card, 0, paused)

    @Test fun lists_renewals_statements_and_dues_in_date_order() {
        val card = Card("c", "Bonus", 25, 5, 0)
        val items = Upcoming.list(listOf(sub("netflix", 23, card = "c"), sub("spotify", 5)), listOf(card), today)
        assertEquals(
            // pencere 22 Kasım–22 Aralık; aynı gün: son ödeme, kesim, yenileme sırası
            listOf("netflix 2026-11-23", "Bonus kesim 2026-11-25", "Bonus son 2026-12-05", "spotify 2026-12-05"),
            items.map { describe(it) },
        )
        val st = items.filterIsInstance<UpcomingItem.Statement>().first()
        assertEquals(0, BigDecimal(10000).compareTo(st.subsKurus)) // 23 Kasım netflix bu dönemde
    }

    @Test fun window_is_30_days_and_paused_hidden() {
        val items = Upcoming.list(listOf(sub("a", 22), sub("b", 21), sub("p", 25, paused = true)), emptyList(), today)
        assertEquals(listOf("a 2026-11-22", "b 2026-12-21"), items.map { describe(it) })
        assertTrue(Upcoming.list(listOf(sub("a", 22)), emptyList(), today, days = 1).map { describe(it) } == listOf("a 2026-11-22"))
        assertTrue(Upcoming.list(listOf(sub("a", 22)), emptyList(), today, days = 0).isEmpty())
    }

    @Test fun relative_labels() {
        assertEquals("bugün", Upcoming.relativeLabel(today, today))
        assertEquals("yarın", Upcoming.relativeLabel(today.plusDays(1), today))
        assertEquals("3 gün", Upcoming.relativeLabel(today.plusDays(3), today))
    }

    private fun describe(i: UpcomingItem) = when (i) {
        is UpcomingItem.Renewal -> "${i.sub.name} ${i.date}"
        is UpcomingItem.Statement -> "${i.card.name} kesim ${i.date}"
        is UpcomingItem.Due -> "${i.card.name} son ${i.date}"
    }
}
