package com.ibrah.abonelik.core

import java.time.LocalDate
import java.time.YearMonth

/** Tarih kuralları: ayda olmayan gün → ayın son günü (31 → 28/29/30, 29 Şubat → 28 Şubat). */
object Schedule {
    fun dateIn(ym: YearMonth, day: Int): LocalDate = ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))

    /** [from] günü ya da sonrasındaki ilk yenileme. */
    fun nextRenewal(s: Sub, from: LocalDate): LocalDate = when (s.period) {
        Period.MONTHLY -> {
            val here = dateIn(YearMonth.from(from), s.day)
            if (here >= from) here else dateIn(YearMonth.from(from).plusMonths(1), s.day)
        }
        Period.YEARLY -> {
            val m = s.month ?: 1
            val here = dateIn(YearMonth.of(from.year, m), s.day)
            if (here >= from) here else dateIn(YearMonth.of(from.year + 1, m), s.day)
        }
    }

    /** [onOrBefore] günü ya da öncesindeki son yenileme. */
    fun previousRenewal(s: Sub, onOrBefore: LocalDate): LocalDate = when (s.period) {
        Period.MONTHLY -> {
            val here = dateIn(YearMonth.from(onOrBefore), s.day)
            if (here <= onOrBefore) here else dateIn(YearMonth.from(onOrBefore).minusMonths(1), s.day)
        }
        Period.YEARLY -> {
            val m = s.month ?: 1
            val here = dateIn(YearMonth.of(onOrBefore.year, m), s.day)
            if (here <= onOrBefore) here else dateIn(YearMonth.of(onOrBefore.year - 1, m), s.day)
        }
    }

    /** [start]..[end] (ikisi dahil) arasındaki bütün yenilemeler. */
    fun renewalsBetween(s: Sub, start: LocalDate, end: LocalDate): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var d = nextRenewal(s, start)
        while (d <= end) { out += d; d = nextRenewal(s, d.plusDays(1)) }
        return out
    }

    fun nextStatement(c: Card, from: LocalDate): LocalDate {
        val here = dateIn(YearMonth.from(from), c.statementDay)
        return if (here >= from) here else dateIn(YearMonth.from(from).plusMonths(1), c.statementDay)
    }

    /** Son ödeme: son ödeme günü kesim gününden büyükse aynı ay, değilse sonraki ay. */
    fun dueAfter(c: Card, statement: LocalDate): LocalDate {
        val ym = YearMonth.from(statement)
        return if (c.dueDay > c.statementDay) dateIn(ym, c.dueDay) else dateIn(ym.plusMonths(1), c.dueDay)
    }

    /** Ekstre dönemi: önceki kesimden bir gün sonra → bu kesim (dahil). */
    fun period(c: Card, statement: LocalDate): ClosedRange<LocalDate> {
        val prev = dateIn(YearMonth.from(statement).minusMonths(1), c.statementDay)
        return prev.plusDays(1)..statement
    }
}
