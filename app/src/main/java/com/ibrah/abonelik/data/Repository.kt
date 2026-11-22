package com.ibrah.abonelik.data

import android.content.Context
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Sub
import java.text.Collator
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Repository(private val db: AppDb) {
    private val dao = db.dao()
    private val collator = Collator.getInstance(Locale.forLanguageTag("tr-TR")).apply { strength = Collator.PRIMARY }

    /** Yazma sonrası çağrılır (widget güncellemesi vb.); [get] ile kurulur. */
    var onChange: suspend () -> Unit = {}

    val subs: Flow<List<Sub>> = dao.observeSubs().map { l -> l.map { it.toModel() }.sortedWith(compareBy(collator) { it.name }) }
    val cards: Flow<List<Card>> = dao.observeCards().map { l -> l.map { it.toModel() }.sortedWith(compareBy(collator) { it.name }) }

    suspend fun subsNow(): List<Sub> = dao.subs().map { it.toModel() }
    suspend fun cardsNow(): List<Card> = dao.cards().map { it.toModel() }

    suspend fun saveSub(s: Sub) { dao.upsertSub(s.toEntity()); onChange() }
    suspend fun deleteSub(id: String) { dao.deleteSub(id); onChange() }
    suspend fun saveCard(c: Card) { dao.upsertCard(c.toEntity()); onChange() }
    suspend fun deleteCard(id: String) { dao.deleteCard(id); onChange() }

    suspend fun sentKeys(): Set<String> = dao.sentKeys().toSet()
    suspend fun markSent(keys: List<String>, on: LocalDate) = dao.upsertSent(keys.map { SentEntity(it, on.toEpochDay()) })
    suspend fun pruneSent(before: LocalDate) = dao.pruneSent(before.toEpochDay())

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(AppDb.get(context)).also { r ->
                val app = context.applicationContext
                r.onChange = { com.ibrah.abonelik.widget.WidgetUpdater.update(app) }
                instance = r
            }
        }
    }
}

private fun SubEntity.toModel() = Sub(
    id, name, amountKurus,
    runCatching { Currency.valueOf(currency) }.getOrDefault(Currency.TRY),
    fxRateMilli,
    runCatching { Period.valueOf(period) }.getOrDefault(Period.MONTHLY),
    day, month, cardId, color, paused, icon,
)

private fun Sub.toEntity() = SubEntity(
    id, name, amountKurus, currency.name, fxRateMilli, period.name, day, month, cardId, color, paused, icon,
)

private fun CardEntity.toModel() = Card(id, name, statementDay, dueDay, color)
private fun Card.toEntity() = CardEntity(id, name, statementDay, dueDay, color)
