package com.ibrah.abonelik.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakdownTest {
    private fun sub(id: String, kurus: Long, period: Period = Period.MONTHLY, paused: Boolean = false) =
        Sub(id, id, kurus, Currency.TRY, null, period, 1, if (period == Period.YEARLY) 1 else null, null, 0, paused)

    @Test fun top_three_and_others_sorted_by_monthly_cost() {
        val b = Totals.breakdown(listOf(
            sub("a", 10000), sub("b", 50000), sub("c", 20000), sub("d", 5000), sub("e", 15000),
            sub("y", 1200000, Period.YEARLY), // aylık 1.000 TL → en büyük
            sub("p", 999999, paused = true),
        ))
        assertEquals(listOf("y", "b", "c", null), b.map { it.sub?.name }) // null = Diğer
        assertEquals(0, BigDecimal(30000).compareTo(b.last().monthlyKurus)) // a+d+e = 100+50+150
        val sum = b.sumOf { it.fraction }
        assertEquals(1.0, sum, 1e-9)
        assertEquals(0.5, b.first().fraction, 1e-9) // 1.000 / (100+500+200+50+150+1.000)
    }

    @Test fun no_others_slice_when_three_or_fewer() {
        val b = Totals.breakdown(listOf(sub("a", 10000), sub("b", 30000)))
        assertEquals(listOf("b", "a"), b.map { it.sub?.name })
        assertTrue(b.none { it.sub == null })
    }

    @Test fun empty_or_zero_gives_nothing() {
        assertTrue(Totals.breakdown(emptyList()).isEmpty())
        assertTrue(Totals.breakdown(listOf(sub("p", 100, paused = true))).isEmpty())
    }

    @Test fun setup_missing_count() {
        assertEquals(0, SetupState(notifications = true, battery = true, autoStart = true).missing)
        assertEquals(2, SetupState(notifications = false, battery = true, autoStart = false).missing)
    }
}
