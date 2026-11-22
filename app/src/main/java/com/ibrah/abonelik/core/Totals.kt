package com.ibrah.abonelik.core

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** Toplamlar yuvarlamasız (BigDecimal kuruş); yuvarlama yalnızca Money.format'ta. */
object Totals {
    private val MC = MathContext.DECIMAL64
    private val TWELVE = BigDecimal(12)
    private val THOUSAND = BigDecimal(1000)

    /** Aboneliğin bir ödemesinin TL karşılığı (kuruş). Yabancı parada kur yoksa 0. */
    fun tryKurus(s: Sub): BigDecimal {
        val amount = BigDecimal.valueOf(s.amountKurus)
        if (s.currency == Currency.TRY) return amount
        val rate = s.fxRateMilli ?: return BigDecimal.ZERO
        return amount.multiply(BigDecimal.valueOf(rate)).divide(THOUSAND, MC)
    }

    /** Aylık karşılık: aylık = ödeme; yıllık = ödeme / 12. */
    fun monthlyOf(s: Sub): BigDecimal = when (s.period) {
        Period.MONTHLY -> tryKurus(s)
        Period.YEARLY -> tryKurus(s).divide(TWELVE, MC)
    }

    /** Yıllıklar önce toplanıp tek seferde 12'ye bölünür: "aylık × 12" yıllık toplamla tutarlı kalsın. */
    fun monthlyKurus(subs: List<Sub>): BigDecimal {
        val active = subs.filterNot { it.paused }
        val monthly = active.filter { it.period == Period.MONTHLY }.fold(BigDecimal.ZERO) { a, s -> a.add(tryKurus(s)) }
        val yearly = active.filter { it.period == Period.YEARLY }.fold(BigDecimal.ZERO) { a, s -> a.add(tryKurus(s)) }
        return monthly.add(yearly.divide(TWELVE, MC))
    }

    /** Yıllık toplam: aylıklar × 12 + yıllıklar (bölme yok). */
    fun yearlyKurus(subs: List<Sub>): BigDecimal {
        val active = subs.filterNot { it.paused }
        return active.fold(BigDecimal.ZERO) { a, s ->
            a.add(if (s.period == Period.MONTHLY) tryKurus(s).multiply(TWELVE) else tryKurus(s))
        }
    }

    /** Karta bağlı etkin aboneliklerden bu ekstre dönemine düşen ödemelerin toplamı. */
    fun onStatement(card: Card, subs: List<Sub>, statement: LocalDate): BigDecimal {
        val p = Schedule.period(card, statement)
        return subs.filter { it.cardId == card.id && !it.paused }.fold(BigDecimal.ZERO) { acc, s ->
            val n = Schedule.renewalsBetween(s, p.start, p.endInclusive).size
            acc.add(tryKurus(s).multiply(BigDecimal(n)))
        }
    }
}
