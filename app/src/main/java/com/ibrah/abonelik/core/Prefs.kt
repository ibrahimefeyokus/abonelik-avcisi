package com.ibrah.abonelik.core

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem"), LIGHT("Açık"), DARK("Koyu");

    fun isDark(systemDark: Boolean) = when (this) { SYSTEM -> systemDark; LIGHT -> false; DARK -> true }
}

/** Bildirimin nasıl iletileceği: kanal seçimi ve uygulama içi aç/kapa. */
object Delivery {
    /**
     * Android kanal ses/titreşimini sonradan değiştirmeye izin vermez; her birleşim ayrı kanaldır,
     * kullanılmayanlar silinir.
     */
    fun channelId(sound: Boolean, vibrate: Boolean) = "hatirlatmalar_s${if (sound) 1 else 0}_v${if (vibrate) 1 else 0}"

    val allChannelIds = listOf(true, false).flatMap { s -> listOf(true, false).map { v -> channelId(s, v) } }
    val legacyChannelIds = listOf("hatirlatmalar")

    data class Plan(val show: List<Reminder>, val markSent: List<Reminder>)

    /**
     * Uygulamadan kapatıldıysa: gösterme ama "geçti" say (yeniden açılınca geçmişler yağmasın).
     * Telefon (izin/kanal) engelliyorsa: gösterme, sayma da (izin verilince telafi edilir).
     */
    fun plan(due: List<Reminder>, notificationsOn: Boolean, systemAllows: Boolean): Plan = when {
        !notificationsOn -> Plan(emptyList(), due)
        !systemAllows -> Plan(emptyList(), emptyList())
        else -> Plan(due, due)
    }
}
