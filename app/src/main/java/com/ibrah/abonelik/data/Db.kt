package com.ibrah.abonelik.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subs")
data class SubEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountKurus: Long,
    val currency: String,
    val fxRateMilli: Long?,
    val period: String,
    val day: Int,
    val month: Int?,
    val cardId: String?,
    val color: Int,
    val paused: Boolean,
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val statementDay: Int,
    val dueDay: Int,
    val color: Int,
)

/** Gönderilmiş hatırlatmalar: anahtar "tür:öğe:olay günü"; [sentOn] epoch günü. */
@Entity(tableName = "sent_reminders")
data class SentEntity(@PrimaryKey val key: String, val sentOn: Long)

@Dao
interface AppDao {
    @Query("SELECT * FROM subs") fun observeSubs(): Flow<List<SubEntity>>
    @Query("SELECT * FROM subs") suspend fun subs(): List<SubEntity>
    @Upsert suspend fun upsertSub(e: SubEntity)
    @Query("DELETE FROM subs WHERE id = :id") suspend fun deleteSub(id: String)

    @Query("SELECT * FROM cards") fun observeCards(): Flow<List<CardEntity>>
    @Query("SELECT * FROM cards") suspend fun cards(): List<CardEntity>
    @Upsert suspend fun upsertCard(e: CardEntity)
    @Query("UPDATE subs SET cardId = NULL WHERE cardId = :id") suspend fun detachCard(id: String)
    @Query("DELETE FROM cards WHERE id = :id") suspend fun deleteCardRow(id: String)

    /** Kart silinir, bağlı abonelikler kartsız kalır (silinmez). */
    @Transaction suspend fun deleteCard(id: String) { detachCard(id); deleteCardRow(id) }

    @Query("SELECT `key` FROM sent_reminders") suspend fun sentKeys(): List<String>
    @Upsert suspend fun upsertSent(e: List<SentEntity>)
    @Query("DELETE FROM sent_reminders WHERE sentOn < :beforeEpochDay") suspend fun pruneSent(beforeEpochDay: Long)
}

@Database(entities = [SubEntity::class, CardEntity::class, SentEntity::class], version = 1, exportSchema = true)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "abonelik.db")
                .build().also { instance = it }
        }
    }
}
