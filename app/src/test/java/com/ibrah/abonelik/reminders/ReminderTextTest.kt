package com.ibrah.abonelik.reminders

import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Kind
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Reminder
import com.ibrah.abonelik.core.Sub
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTextTest {
    private val today = LocalDate.parse("2026-11-22")
    private val netflix = Sub("n", "Netflix", 22999, Currency.TRY, null, Period.MONTHLY, 23, null, null, 0, false)
    private val gpt = Sub("g", "ChatGPT", 2000, Currency.USD, 34250, Period.MONTHLY, 24, null, null, 0, false)
    private val bonus = Card("b", "Bonus", 22, 5, 0)
    private fun r(kind: Kind, id: String, event: String) = Reminder(kind, id, LocalDate.parse(event), today)

    @Test fun subscription_texts() {
        assertEquals("Netflix yarın yenileniyor" to "229,99 TL",
            ReminderText.of(r(Kind.SUB, "n", "2026-11-23"), netflix, null, null, today))
        assertEquals("ChatGPT 2 gün sonra yenileniyor" to "20,00 $ ≈ 685,00 TL",
            ReminderText.of(r(Kind.SUB, "g", "2026-11-24"), gpt, null, null, today))
        assertEquals("Netflix bugün yenileniyor" to "229,99 TL",
            ReminderText.of(r(Kind.SUB, "n", "2026-11-22"), netflix, null, null, today))
    }

    @Test fun card_texts() {
        assertEquals("Bonus ekstresi bugün kesiliyor" to "Bu ekstrede 520,00 TL abonelik",
            ReminderText.of(r(Kind.STATEMENT, "b", "2026-11-22"), null, bonus, BigDecimal(52000), today))
        assertEquals("Bonus ekstresi bugün kesiliyor" to "Bu ekstrede abonelik yok",
            ReminderText.of(r(Kind.STATEMENT, "b", "2026-11-22"), null, bonus, BigDecimal.ZERO, today))
        assertEquals("Bonus son ödeme 2 gün sonra" to "Son ödeme: 24 Kasım",
            ReminderText.of(r(Kind.DUE, "b", "2026-11-24"), null, bonus, null, today))
    }

    @Test fun deleted_items_give_no_text() {
        assertNull(ReminderText.of(r(Kind.SUB, "x", "2026-11-23"), null, null, null, today))
        assertNull(ReminderText.of(r(Kind.DUE, "x", "2026-11-23"), null, null, null, today))
    }
}
