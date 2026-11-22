package com.ibrah.abonelik.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ibrah.abonelik.core.ReminderSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    private object K {
        val TIME = intPreferencesKey("time_minutes")
        val SUB_DAYS = intPreferencesKey("sub_days")
        val DUE_DAYS = intPreferencesKey("due_days")
        val SUB_ON = booleanPreferencesKey("sub_on")
        val ST_ON = booleanPreferencesKey("statement_on")
        val DUE_ON = booleanPreferencesKey("due_on")
    }

    val flow: Flow<ReminderSettings> = store.data.map { it.toSettings() }

    suspend fun current(): ReminderSettings = flow.first()

    suspend fun update(f: (ReminderSettings) -> ReminderSettings) {
        store.edit { p ->
            val n = f(p.toSettings()).normalized()
            p[K.TIME] = n.time.hour * 60 + n.time.minute
            p[K.SUB_DAYS] = n.subDaysBefore
            p[K.DUE_DAYS] = n.dueDaysBefore
            p[K.SUB_ON] = n.subOn
            p[K.ST_ON] = n.statementOn
            p[K.DUE_ON] = n.dueOn
        }
    }

    private fun Preferences.toSettings(): ReminderSettings {
        val d = ReminderSettings()
        val minutes = this[K.TIME] ?: (d.time.hour * 60 + d.time.minute)
        return ReminderSettings(
            time = LocalTime.of(minutes / 60 % 24, minutes % 60),
            subDaysBefore = this[K.SUB_DAYS] ?: d.subDaysBefore,
            dueDaysBefore = this[K.DUE_DAYS] ?: d.dueDaysBefore,
            subOn = this[K.SUB_ON] ?: d.subOn,
            statementOn = this[K.ST_ON] ?: d.statementOn,
            dueOn = this[K.DUE_ON] ?: d.dueOn,
        )
    }
}
