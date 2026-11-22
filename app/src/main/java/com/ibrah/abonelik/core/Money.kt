package com.ibrah.abonelik.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class Currency(val symbol: String, val label: String) {
    TRY("TL", "Türk lirası"), USD("$", "Dolar"), EUR("€", "Euro"),
}

/** Tutarlar kuruş cinsinden Long tutulur; kurlar ×1000 (34,25 → 34250). */
object Money {
    private val TR = Locale.forLanguageTag("tr-TR")
    private const val MAX_KURUS = 100_000_000L // 1.000.000 TL
    private const val MAX_RATE = 100_000_000L  // 100.000 TL / birim

    /** "229,99", "1.284,50", "229.99", "1.000" → kuruş; geçersiz, sıfır ya da çok büyükse null. */
    fun parse(text: String): Long? =
        parseScaled(text, maxDecimals = 2)?.takeIf { it in 1..MAX_KURUS }

    /** Kur: en fazla 3 ondalık, ×1000. */
    fun parseRate(text: String): Long? =
        parseScaled(text, maxDecimals = 3)?.takeIf { it in 1..MAX_RATE }

    fun format(kurus: Long): String = format(BigDecimal.valueOf(kurus))

    /** Kendi para biriminde: "20,00 $", "229,99 TL". */
    fun formatIn(kurus: Long, currency: Currency): String =
        format(kurus).removeSuffix(" TL") + " " + currency.symbol

    /** Yuvarlamasız kuruş tutarını gösterim için kuruşa (HALF_UP) yuvarlar. */
    fun format(kurus: BigDecimal): String {
        val lira = kurus.setScale(0, RoundingMode.HALF_UP).movePointLeft(2)
        return DecimalFormat("#,##0.00", DecimalFormatSymbols(TR)).format(lira) + " TL"
    }

    /** Düzenleme alanına geri yazmak için: 22999 → "229,99", 7900 → "79". */
    fun toInput(kurus: Long): String = plain(BigDecimal.valueOf(kurus).movePointLeft(2))

    fun rateToInput(rateMilli: Long): String = plain(BigDecimal.valueOf(rateMilli).movePointLeft(3))

    private fun plain(v: BigDecimal): String = v.stripTrailingZeros().let {
        if (it.scale() < 0) it.setScale(0) else it
    }.toPlainString().replace('.', ',')

    /**
     * [maxDecimals] haneye ölçeklenmiş tamsayı. Virgül varsa ondalık ayırıcıdır, noktalar binliktir.
     * Yalnızca nokta varsa: tek nokta + 1–2 hane ondalıktır (kurda 1–3 hane); 3'er haneli gruplar binliktir.
     */
    private fun parseScaled(raw: String, maxDecimals: Int): Long? {
        val s = raw.trim()
        if (s.isEmpty() || s.any { !it.isDigit() && it != ',' && it != '.' }) return null
        val intPart: String
        val decPart: String
        if (',' in s) {
            if (s.count { it == ',' } > 1) return null
            val (a, b) = s.split(',')
            intPart = thousands(a) ?: return null
            decPart = b
        } else if ('.' in s) {
            val groups = s.split('.')
            val last = groups.last()
            val decimalDot = groups.size == 2 && last.length in 1..maxDecimals && (last.length < 3 || maxDecimals == 3)
            if (decimalDot) {
                intPart = groups[0]
                decPart = last
            } else {
                intPart = thousands(s) ?: return null
                decPart = ""
            }
        } else {
            intPart = s
            decPart = ""
        }
        if (intPart.isEmpty() || !intPart.all { it.isDigit() }) return null
        if (decPart.length > maxDecimals || !decPart.all { it.isDigit() }) return null
        if (intPart.trimStart('0').length > 9) return null
        return (intPart + decPart.padEnd(maxDecimals, '0')).toLongOrNull()
    }

    /** "1.284" → "1284"; gruplar 3 hane değilse null. Nokta yoksa olduğu gibi. */
    private fun thousands(a: String): String? {
        if ('.' !in a) return a
        val g = a.split('.')
        if (g.first().length !in 1..3 || g.drop(1).any { it.length != 3 }) return null
        return g.joinToString("")
    }
}
