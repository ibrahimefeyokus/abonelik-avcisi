package com.ibrah.abonelik.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TurkishTest {
    @Test fun day_suffixes() {
        val expected = mapOf(
            1 to "1'i", 2 to "2'si", 3 to "3'ü", 4 to "4'ü", 5 to "5'i", 6 to "6'sı", 7 to "7'si", 8 to "8'i", 9 to "9'u",
            10 to "10'u", 11 to "11'i", 20 to "20'si", 23 to "23'ü", 26 to "26'sı", 29 to "29'u", 30 to "30'u", 31 to "31'i",
        )
        expected.forEach { (n, s) -> assertEquals(s, Turkish.dayWithSuffix(n)) }
    }
}
