package com.ibrah.abonelik.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun monthly(day: Int) = Sub("s", "X", 1000, Currency.TRY, null, Period.MONTHLY, day, null, null, 0, false)
    private fun yearly(day: Int, month: Int) = Sub("y", "Y", 1000, Currency.TRY, null, Period.YEARLY, day, month, null, 0, false)
    private fun card(statement: Int, due: Int) = Card("c", "Bonus", statement, due, 0)

    @Test fun day_31_clamps_to_month_end_and_returns() {
        assertEquals(d("2026-02-28"), Schedule.nextRenewal(monthly(31), d("2026-02-01")))
        assertEquals(d("2028-02-29"), Schedule.nextRenewal(monthly(31), d("2028-02-10")))
        assertEquals(d("2026-03-31"), Schedule.nextRenewal(monthly(31), d("2026-03-01")))
        assertEquals(d("2026-04-30"), Schedule.nextRenewal(monthly(31), d("2026-04-01")))
    }

    @Test fun renewal_on_from_day_is_same_day_and_after_rolls_over() {
        assertEquals(d("2026-11-22"), Schedule.nextRenewal(monthly(22), d("2026-11-22")))
        assertEquals(d("2026-12-21"), Schedule.nextRenewal(monthly(21), d("2026-11-22")))
        // ayın son günü geçince sonraki ay
        assertEquals(d("2026-03-31"), Schedule.nextRenewal(monthly(31), d("2026-03-01")))
        assertEquals(d("2027-01-31"), Schedule.nextRenewal(monthly(31), d("2027-01-01")))
    }

    @Test fun yearly_feb_29_falls_back_to_28() {
        assertEquals(d("2027-02-28"), Schedule.nextRenewal(yearly(29, 2), d("2026-11-22")))
        assertEquals(d("2028-02-29"), Schedule.nextRenewal(yearly(29, 2), d("2027-03-01")))
        assertEquals(d("2027-03-15"), Schedule.nextRenewal(yearly(15, 3), d("2026-11-22")))
        assertEquals(d("2026-12-01"), Schedule.nextRenewal(yearly(1, 12), d("2026-11-22")))
    }

    @Test fun previous_renewal_is_last_on_or_before() {
        assertEquals(d("2026-11-15"), Schedule.previousRenewal(monthly(15), d("2026-11-22")))
        assertEquals(d("2026-11-22"), Schedule.previousRenewal(monthly(22), d("2026-11-22")))
        assertEquals(d("2026-02-28"), Schedule.previousRenewal(monthly(31), d("2026-03-10")))
    }

    @Test fun statement_and_due_dates() {
        assertEquals(d("2026-11-25"), Schedule.nextStatement(card(25, 5), d("2026-11-22")))
        assertEquals(d("2026-12-05"), Schedule.dueAfter(card(25, 5), d("2026-11-25")))
        assertEquals(d("2026-12-05"), Schedule.nextStatement(card(5, 15), d("2026-11-22")))
        assertEquals(d("2026-12-15"), Schedule.dueAfter(card(5, 15), d("2026-12-05")))
        // eşit gün → sonraki ay
        assertEquals(d("2026-12-10"), Schedule.dueAfter(card(10, 10), d("2026-11-10")))
        // 31 kesim Şubat'ta 28
        assertEquals(d("2027-02-28"), Schedule.nextStatement(card(31, 10), d("2027-02-01")))
    }

    @Test fun statement_period_runs_from_previous_statement_plus_one() {
        val p = Schedule.period(card(25, 5), d("2026-11-25"))
        assertEquals(d("2026-10-26"), p.start)
        assertEquals(d("2026-11-25"), p.endInclusive)
        val q = Schedule.period(card(31, 10), d("2026-03-31"))
        assertEquals(d("2026-03-01"), q.start) // önceki kesim 28 Şubat
    }

    @Test fun renewals_between_lists_every_occurrence() {
        assertEquals(
            listOf(d("2026-11-25"), d("2026-12-25")),
            Schedule.renewalsBetween(monthly(25), d("2026-11-01"), d("2026-12-31")),
        )
        assertEquals(emptyList<LocalDate>(), Schedule.renewalsBetween(yearly(1, 6), d("2026-11-01"), d("2026-12-31")))
    }
}
