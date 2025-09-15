package com.jumblemint.cows.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance scoped to Context, made internal for module-wide access
internal val Context.tipsDataStore by preferencesDataStore(name = "tips")

object TipPrefsKeys {
    val DISABLE_ALL = booleanPreferencesKey("tips_disabled")
    fun dismissedKey(id: String): Preferences.Key<Boolean> = booleanPreferencesKey("tip_${'$'}{id}_dismissed")
}

class TipsManager(private val context: Context) {

    fun isTipVisible(id: String): Flow<Boolean> =
        context.tipsDataStore.data.map { prefs ->
            val disabled = prefs[TipPrefsKeys.DISABLE_ALL] == true
            val dismissed = prefs[TipPrefsKeys.dismissedKey(id)] == true
            !disabled && !dismissed
        }

    suspend fun dismissTip(id: String) {
        context.tipsDataStore.edit { it[TipPrefsKeys.dismissedKey(id)] = true }
    }

    suspend fun hideAllTips() {
        context.tipsDataStore.edit { it[TipPrefsKeys.DISABLE_ALL] = true }
    }

    suspend fun enableAllTips() {
        context.tipsDataStore.edit { prefs -> // Changed 'it' to 'prefs' for clarity
            prefs.clear() // Clear all existing tip preferences
            prefs[TipPrefsKeys.DISABLE_ALL] = false // Then explicitly set DISABLE_ALL to false
        }
    }
}