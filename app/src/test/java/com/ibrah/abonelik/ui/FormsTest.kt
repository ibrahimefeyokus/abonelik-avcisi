package com.ibrah.abonelik.ui

import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Sub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormsTest {
    private val ok = SubForm(name = "Netflix", amount = "229,99", day = "23")

    @Test fun valid_monthly_form_builds_sub() {
        assertTrue(ok.validate().isEmpty())
        assertEquals(
            Sub("id", "Netflix", 22999, Currency.TRY, null, Period.MONTHLY, 23, null, null, 0, false),
            ok.toSub("id"),
        )
    }

    @Test fun errors_are_reported_per_field() {
        val e = SubForm(name = "  ", amount = "abc", day = "32").validate()
        assertEquals("Ad gerekli", e["name"])
        assertEquals("Geçerli bir tutar gir", e["amount"])
        assertEquals("Gün 1–31 olmalı", e["day"])
    }

    @Test fun foreign_currency_needs_rate() {
        assertEquals("Kur gerekli", ok.copy(currency = Currency.USD, rate = "").validate()["rate"])
        val s = ok.copy(currency = Currency.USD, amount = "20", rate = "34,25").toSub("id")
        assertEquals(34250L, s.fxRateMilli)
        assertEquals(2000L, s.amountKurus)
        // TL'ye dönünce kur atılır
        assertEquals(null, ok.copy(rate = "34,25").toSub("id").fxRateMilli)
    }

    @Test fun yearly_needs_month() {
        assertEquals("Ay seç", ok.copy(period = Period.YEARLY).validate()["month"])
        val s = ok.copy(period = Period.YEARLY, month = 3).toSub("id")
        assertEquals(3, s.month)
        // aylıkta ay saklanmaz
        assertEquals(null, ok.copy(month = 3).toSub("id").month)
    }

    @Test fun form_round_trips_from_sub() {
        val s = Sub("x", "ChatGPT", 2000, Currency.USD, 34250, Period.YEARLY, 5, 6, "c", 4, true)
        assertEquals(s, SubForm.from(s).toSub("x"))
    }

    @Test fun card_form() {
        assertTrue(CardForm("Bonus", "25", "5").validate().isEmpty())
        assertEquals(Card("c", "Bonus", 25, 5, 2), CardForm("Bonus", "25", "5", 2).toCard("c"))
        val e = CardForm("", "0", "x").validate()
        assertEquals("Ad gerekli", e["name"])
        assertEquals("Gün 1–31 olmalı", e["statementDay"])
        assertEquals("Gün 1–31 olmalı", e["dueDay"])
        assertEquals(CardForm("Bonus", "25", "5", 2), CardForm.from(Card("c", "Bonus", 25, 5, 2)))
    }
}
