package com.ibrah.abonelik.core

enum class Period(val label: String) { MONTHLY("aylık"), YEARLY("yıllık") }

/**
 * Abonelik. [amountKurus] kendi para biriminde; [fxRateMilli] yabancı para için elle girilen kur ×1000.
 * Aylıkta [day] ayın günü (1–31); yıllıkta [day] + [month] (1–12).
 */
data class Sub(
    val id: String,
    val name: String,
    val amountKurus: Long,
    val currency: Currency,
    val fxRateMilli: Long?,
    val period: Period,
    val day: Int,
    val month: Int?,
    val cardId: String?,
    val color: Int,
    val paused: Boolean,
)

/** Kredi kartı: yalnızca kullanıcının verdiği ad ve günler; kart numarası tutulmaz. */
data class Card(
    val id: String,
    val name: String,
    val statementDay: Int,
    val dueDay: Int,
    val color: Int,
)
