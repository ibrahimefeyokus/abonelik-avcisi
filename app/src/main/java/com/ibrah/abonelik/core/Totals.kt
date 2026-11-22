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

    /** Dağılım dilimi; [sub] null ise "Diğer". */
    data class Slice(val sub: Sub?, val monthlyKurus: BigDecimal, val fraction: Double)

    /** En pahalı [top] etkin abonelik (aylık karşılığa göre) ve gerisi tek "Diğer" dilimi. */
    fun breakdown(subs: List<Sub>, top: Int = 3): List<Slice> {
        val items = subs.filterNot { it.paused }.map { it to monthlyOf(it) }.filter { it.second.signum() > 0 }
            .sortedByDescending { it.second }
        val total = items.fold(BigDecimal.ZERO) { a, p -> a.add(p.second) }
        if (total.signum() == 0) return emptyList()
        fun frac(v: BigDecimal) = v.divide(total, MC).toDouble()
        val head = items.take(top).map { (s, v) -> Slice(s, v, frac(v)) }
        val rest = items.drop(top).fold(BigDecimal.ZERO) { a, p -> a.add(p.second) }
        return if (rest.signum() > 0) head + Slice(null, rest, frac(rest)) else head
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
