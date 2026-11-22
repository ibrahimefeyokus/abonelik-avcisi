package com.ibrah.abonelik.core

import java.util.Locale

/** Telefonda yüklü bir uygulama. */
data class AppEntry(val pkg: String, val label: String)

/** Abonelik simge anahtarı: "music" gibi genel simge ya da "app:<paket>" ile telefondaki uygulamanın simgesi. */
object IconKey {
    private const val PREFIX = "app:"
    fun forApp(pkg: String) = PREFIX + pkg
    fun appPackage(icon: String): String? = icon.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.ifEmpty { null }
}

/** Abonelik adına göre telefondaki uygulamayı önerir ("YouTube Premium" → YouTube). */
object AppMatch {
    private val TR = Locale.forLanguageTag("tr-TR")
    private val map = mapOf('ı' to 'i', 'ş' to 's', 'ç' to 'c', 'ğ' to 'g', 'ö' to 'o', 'ü' to 'u', 'â' to 'a')

    fun fold(s: String): String = s.trim().lowercase(TR).map { map[it] ?: it }.joinToString("")
        .replace(Regex("\\s+"), " ")

    /** Ad, uygulama adıyla aynıysa ya da onunla başlayıp boşlukla devam ediyorsa eşleşir; en uzun ad kazanır. */
    fun best(name: String, apps: List<AppEntry>): AppEntry? {
        val n = fold(name)
        if (n.length < 3) return null
        return apps.filter { a ->
            val l = fold(a.label)
            l.length >= 3 && (n == l || n.startsWith("$l "))
        }.maxByOrNull { it.label.length }
    }
}
