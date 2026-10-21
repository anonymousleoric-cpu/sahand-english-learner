package com.sahandsoft.englishlearner.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sahand_settings")

data class AppSettings(
    val themeMode: String = "system",
    val childMode: Boolean = false,
    val dailyGoalMinutes: Int = 10,
    val ttsRate: Float = 1.0f
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val CHILD = booleanPreferencesKey("child_mode")
        val GOAL = intPreferencesKey("daily_goal_minutes")
        val RATE = floatPreferencesKey("tts_rate")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME] ?: "system",
            childMode = p[Keys.CHILD] ?: false,
            dailyGoalMinutes = p[Keys.GOAL] ?: 10,
            ttsRate = p[Keys.RATE] ?: 1.0f
        )
    }

    suspend fun snapshot(): AppSettings = settings.first()

    suspend fun setTheme(mode: String) {
        context.dataStore.edit { it[Keys.THEME] = mode }
    }

    suspend fun setChildMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CHILD] = enabled }
    }

    suspend fun setDailyGoal(minutes: Int) {
        context.dataStore.edit { it[Keys.GOAL] = minutes }
    }

    suspend fun setTtsRate(rate: Float) {
        context.dataStore.edit { it[Keys.RATE] = rate }
    }
}
