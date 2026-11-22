package com.ibrah.abonelik.reminders

import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Kind
import com.ibrah.abonelik.core.Money
import com.ibrah.abonelik.core.Reminder
import com.ibrah.abonelik.core.Sub
import com.ibrah.abonelik.core.Totals
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Bildirim başlığı ve metni. Öğe silinmişse null (bildirim gönderilmez). */
object ReminderText {
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("tr-TR"))

    fun of(r: Reminder, sub: Sub?, card: Card?, statementKurus: BigDecimal?, today: LocalDate): Pair<String, String>? =
        when (r.kind) {
            Kind.SUB -> sub?.let { "${it.name} ${whenText(r.eventDate, today)} yenileniyor" to amountText(it) }
            Kind.STATEMENT -> card?.let {
                val total = statementKurus ?: BigDecimal.ZERO
                "${it.name} ekstresi ${whenText(r.eventDate, today)} kesiliyor" to
                    if (total.signum() > 0) "Bu ekstrede ${Money.format(total)} abonelik" else "Bu ekstrede abonelik yok"
            }
            Kind.DUE -> card?.let {
                "${it.name} son ödeme ${whenText(r.eventDate, today)}" to "Son ödeme: ${DAY_MONTH.format(r.eventDate)}"
            }
        }

    fun amountText(s: Sub): String =
        if (s.currency == Currency.TRY) Money.format(s.amountKurus)
        else "${Money.formatIn(s.amountKurus, s.currency)} ≈ ${Money.format(Totals.tryKurus(s))}"

    private fun whenText(date: LocalDate, today: LocalDate) = when (val n = ChronoUnit.DAYS.between(today, date)) {
        0L -> "bugün"
        1L -> "yarın"
        else -> "$n gün sonra"
    }
}
