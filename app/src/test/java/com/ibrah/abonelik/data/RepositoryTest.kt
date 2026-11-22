package com.ibrah.abonelik.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.ReminderSettings
import com.ibrah.abonelik.core.Sub
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).allowMainThreadQueries().build()
    private val repo = Repository(db)

    @After fun close() = db.close()

    private fun sub(id: String, name: String, card: String? = null) =
        Sub(id, name, 22999, Currency.USD, 34250, Period.YEARLY, 29, 2, card, 3, false)

    @Test fun saves_and_reads_subscriptions_sorted_turkish() = runTest {
        repo.saveSub(sub("1", "Şahane"))
        repo.saveSub(sub("2", "spotify"))
        repo.saveSub(sub("3", "Apple"))
        assertEquals(listOf("Apple", "spotify", "Şahane"), repo.subs.first().map { it.name })
        assertEquals(sub("1", "Şahane"), repo.subs.first().last())
        repo.saveSub(sub("1", "Şahane").copy(paused = true))
        assertEquals(true, repo.subs.first().last().paused)
        repo.deleteSub("2")
        assertEquals(listOf("Apple", "Şahane"), repo.subs.first().map { it.name })
    }

    @Test fun deleting_card_keeps_subscriptions_without_card() = runTest {
        repo.saveCard(Card("c", "Bonus", 25, 5, 1))
        repo.saveSub(sub("1", "Netflix", card = "c"))
        repo.deleteCard("c")
        assertEquals(emptyList<Card>(), repo.cards.first())
        val s = repo.subs.first().single()
        assertEquals("Netflix", s.name)
        assertNull(s.cardId)
    }

    @Test fun sent_keys_are_recorded_and_pruned() = runTest {
        repo.markSent(listOf("SUB:a:2026-11-23", "DUE:b:2026-12-05"), LocalDate.parse("2026-10-01"))
        repo.markSent(listOf("STATEMENT:b:2026-11-25"), LocalDate.parse("2026-11-22"))
        assertEquals(setOf("SUB:a:2026-11-23", "DUE:b:2026-12-05", "STATEMENT:b:2026-11-25"), repo.sentKeys())
        repo.pruneSent(before = LocalDate.parse("2026-10-23"))
        assertEquals(setOf("STATEMENT:b:2026-11-25"), repo.sentKeys())
    }

    @Test fun settings_round_trip() = runTest {
        val store = SettingsStore(ctx)
        assertEquals(ReminderSettings(), store.flow.first())
        store.update { it.copy(time = LocalTime.of(8, 30), dueDaysBefore = 3, subOn = false) }
        assertEquals(ReminderSettings(time = LocalTime.of(8, 30), dueDaysBefore = 3, subOn = false), store.flow.first())
    }
}
