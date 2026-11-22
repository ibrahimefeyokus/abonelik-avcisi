package com.ibrah.abonelik.ui

import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Money
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Sub

/** Abonelik formu: alanlar kullanıcının yazdığı metin olarak tutulur, kaydederken doğrulanır. */
data class SubForm(
    val name: String = "",
    val amount: String = "",
    val currency: Currency = Currency.TRY,
    val rate: String = "",
    val period: Period = Period.MONTHLY,
    val day: String = "",
    val month: Int? = null,
    val cardId: String? = null,
    val color: Int = 0,
    val paused: Boolean = false,
) {
    fun validate(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ad gerekli")
        if (Money.parse(amount) == null) put("amount", "Geçerli bir tutar gir")
        if (currency != Currency.TRY && Money.parseRate(rate) == null) put("rate", "Kur gerekli")
        if (day.trim().toIntOrNull() !in 1..31) put("day", "Gün 1–31 olmalı")
        if (period == Period.YEARLY && month !in 1..12) put("month", "Ay seç")
    }

    /** Yalnızca [validate] boşken çağrılır. */
    fun toSub(id: String) = Sub(
        id = id,
        name = name.trim(),
        amountKurus = Money.parse(amount)!!,
        currency = currency,
        fxRateMilli = if (currency == Currency.TRY) null else Money.parseRate(rate),
        period = period,
        day = day.trim().toInt(),
        month = if (period == Period.YEARLY) month else null,
        cardId = cardId,
        color = color,
        paused = paused,
    )

    companion object {
        fun from(s: Sub) = SubForm(
            name = s.name,
            amount = Money.toInput(s.amountKurus),
            currency = s.currency,
            rate = s.fxRateMilli?.let { Money.rateToInput(it) }.orEmpty(),
            period = s.period,
            day = s.day.toString(),
            month = s.month,
            cardId = s.cardId,
            color = s.color,
            paused = s.paused,
        )
    }
}

data class CardForm(
    val name: String = "",
    val statementDay: String = "",
    val dueDay: String = "",
    val color: Int = 0,
) {
    fun validate(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ad gerekli")
        if (statementDay.trim().toIntOrNull() !in 1..31) put("statementDay", "Gün 1–31 olmalı")
        if (dueDay.trim().toIntOrNull() !in 1..31) put("dueDay", "Gün 1–31 olmalı")
    }

    fun toCard(id: String) = Card(id, name.trim(), statementDay.trim().toInt(), dueDay.trim().toInt(), color)

    companion object {
        fun from(c: Card) = CardForm(c.name, c.statementDay.toString(), c.dueDay.toString(), c.color)
    }
}
