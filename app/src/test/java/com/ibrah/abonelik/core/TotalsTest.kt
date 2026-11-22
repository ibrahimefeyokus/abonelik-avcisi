package com.ibrah.abonelik.core

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TotalsTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun sub(
        id: String, kurus: Long, period: Period = Period.MONTHLY, day: Int = 1, month: Int? = null,
        currency: Currency = Currency.TRY, rate: Long? = null, card: String? = null, paused: Boolean = false,
    ) = Sub(id, id, kurus, currency, rate, period, day, month, card, 0, paused)

    private fun eq(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test fun monthly_total_mixes_periods_and_currencies() {
        val subs = listOf(
            sub("netflix", 22999),
            sub("prime", 120000, Period.YEARLY, 1, 3),
            sub("chatgpt", 1000, currency = Currency.USD, rate = 34250),
            sub("eski", 50000, paused = true),
        )
        eq("67249", Totals.monthlyKurus(subs)) // 229,99 + 100 + 342,50 = 672,49 TL
        assertEquals("672,49 TL", Money.format(Totals.monthlyKurus(subs)))
    }

    @Test fun yearly_over_12_is_not_rounded_early() {
        val subs = List(3) { sub("y$it", 1000, Period.YEARLY, 1, 1) } // 3 × 10 TL / 12
        val m = Totals.monthlyKurus(subs)
        assertEquals("2,50 TL", Money.format(m))
        eq("3000", m.multiply(BigDecimal(12)))
        eq("3000", Totals.yearlyKurus(subs))
    }

    @Test fun yearly_total_is_exact() {
        val subs = listOf(sub("m", 22999), sub("y", 120000, Period.YEARLY, 1, 3), sub("p", 999, paused = true))
        eq("395988", Totals.yearlyKurus(subs)) // 229,99×12 + 1.200 = 3.959,88
    }

    @Test fun foreign_without_rate_counts_zero() {
        eq("0", Totals.tryKurus(sub("x", 1000, currency = Currency.EUR, rate = null)))
    }

    @Test fun statement_total_takes_renewals_in_period_only() {
        val card = Card("c", "Bonus", 25, 5, 0)
        val subs = listOf(
            sub("a", 10000, day = 1, card = "c"),          // 1 Kasım → dönemde
            sub("b", 20000, day = 26, card = "c"),         // 26 Ekim → dönemde
            sub("c", 30000, Period.YEARLY, 10, 11, card = "c"), // 10 Kasım yıllık → tam
            sub("d", 40000, Period.YEARLY, 10, 6, card = "c"),  // Haziran → değil
            sub("e", 50000, day = 1),                       // başka kart yok
            sub("f", 60000, day = 1, card = "c", paused = true),
        )
        eq("60000", Totals.onStatement(card, subs, d("2026-11-25")))
    }
}
