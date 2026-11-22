package com.ibrah.abonelik.core

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface UpcomingItem {
    val date: LocalDate
    data class Renewal(val sub: Sub, override val date: LocalDate) : UpcomingItem
    data class Statement(val card: Card, override val date: LocalDate, val subsKurus: BigDecimal) : UpcomingItem
    data class Due(val card: Card, override val date: LocalDate) : UpcomingItem
}

object Upcoming {
    /** Bugün dahil [days] gün (30 → bugün..bugün+29) içindeki yenilemeler, kesimler ve son ödemeler; tarih sırasıyla. */
    fun list(subs: List<Sub>, cards: List<Card>, today: LocalDate, days: Int = 30): List<UpcomingItem> {
        if (days <= 0) return emptyList()
        val end = today.plusDays(days - 1L)
        val out = mutableListOf<UpcomingItem>()
        subs.filterNot { it.paused }.forEach { s ->
            Schedule.renewalsBetween(s, today, end).forEach { out += UpcomingItem.Renewal(s, it) }
        }
        cards.forEach { c ->
            // Bugünden önce kesilmiş ekstrenin son ödemesi de pencereye düşebilir: bir ay geriden başla.
            var st = Schedule.nextStatement(c, today.minusMonths(1))
            while (st <= end) {
                if (st >= today) out += UpcomingItem.Statement(c, st, Totals.onStatement(c, subs, st))
                val due = Schedule.dueAfter(c, st)
                if (due in today..end) out += UpcomingItem.Due(c, due)
                st = Schedule.nextStatement(c, st.plusDays(1))
            }
        }
        return out.sortedWith(compareBy<UpcomingItem> { it.date }.thenBy { rank(it) })
    }

    private fun rank(i: UpcomingItem) = when (i) {
        is UpcomingItem.Due -> 0
        is UpcomingItem.Statement -> 1
        is UpcomingItem.Renewal -> 2
    }

    fun relativeLabel(date: LocalDate, today: LocalDate): String = when (val n = ChronoUnit.DAYS.between(today, date)) {
        0L -> "bugün"
        1L -> "yarın"
        else -> "$n gün"
    }
}
