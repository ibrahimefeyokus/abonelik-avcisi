package com.ibrah.abonelik.ui

/** Türkçe ek yardımcıları. */
object Turkish {
    /** Ayın günü + iyelik eki: 1'i, 2'si, 3'ü, 6'sı, 9'u, 10'u, 20'si, 30'u. */
    fun dayWithSuffix(n: Int): String {
        val suffix = when (if (n % 10 != 0) n % 10 else n % 100) {
            1, 5, 8 -> "i"
            2, 7, 20 -> "si"
            3, 4 -> "ü"
            6 -> "sı"
            9, 10, 30 -> "u"
            else -> "i"
        }
        return "$n'$suffix"
    }
}
