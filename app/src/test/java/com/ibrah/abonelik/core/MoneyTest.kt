package com.ibrah.abonelik.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parses_turkish_and_dot_decimals() {
        assertEquals(22999L, Money.parse("229,99"))
        assertEquals(128450L, Money.parse("1.284,50"))
        assertEquals(22999L, Money.parse("229.99"))
        assertEquals(7900L, Money.parse(" 79 "))
        assertEquals(100000L, Money.parse("1.000"))
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(15000000L, Money.parse("150.000,00"))
    }

    @Test fun rejects_bad_input() {
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("99999999999"))
        assertNull(Money.parse("1,234"))   // üç ondalık hane
        assertNull(Money.parse("0"))
        assertNull(Money.parse("1.2.3,4,5"))
    }

    @Test fun formats_turkish() {
        assertEquals("1.284,50 TL", Money.format(128450L))
        assertEquals("0,99 TL", Money.format(99L))
        assertEquals("12,35 TL", Money.format(BigDecimal("1234.5")))
        assertEquals("1.000.000,00 TL", Money.format(100000000L))
    }

    @Test fun parses_rates() {
        assertEquals(34250L, Money.parseRate("34,25"))
        assertEquals(34250L, Money.parseRate("34.25"))
        assertEquals(1000L, Money.parseRate("1"))
        assertEquals(37123L, Money.parseRate("37,123"))
        assertNull(Money.parseRate("0"))
        assertNull(Money.parseRate("x"))
    }

    @Test fun formats_input_back_for_editing() {
        assertEquals("229,99", Money.toInput(22999L))
        assertEquals("1284,5", Money.toInput(128450L))
        assertEquals("79", Money.toInput(7900L))
        assertEquals("34,25", Money.rateToInput(34250L))
    }
}
