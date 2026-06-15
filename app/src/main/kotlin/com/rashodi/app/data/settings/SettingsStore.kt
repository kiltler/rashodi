package com.rashodi.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val darkTheme: Boolean = true,
    val leakShareThreshold: Float = 0.15f,
    val leakGrowthThreshold: Float = 0.30f,
    val demoEnabled: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val DARK = booleanPreferencesKey("dark_theme")
        val LEAK_SHARE = floatPreferencesKey("leak_share")
        val LEAK_GROWTH = floatPreferencesKey("leak_growth")
        val DEMO = booleanPreferencesKey("demo_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            darkTheme = p[Keys.DARK] ?: true,
            leakShareThreshold = p[Keys.LEAK_SHARE] ?: 0.15f,
            leakGrowthThreshold = p[Keys.LEAK_GROWTH] ?: 0.30f,
            demoEnabled = p[Keys.DEMO] ?: false,
        )
    }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[Keys.DARK] = value }
    }

    suspend fun setLeakShare(value: Float) {
        context.dataStore.edit { it[Keys.LEAK_SHARE] = value }
    }

    suspend fun setLeakGrowth(value: Float) {
        context.dataStore.edit { it[Keys.LEAK_GROWTH] = value }
    }

    suspend fun setDemoEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.DEMO] = value }
    }
}
