package com.example.stepwalker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stepWalkerDataStore by preferencesDataStore("stepwalker_settings")

class SettingsStore(private val context: Context) {
    private val notificationsKey = booleanPreferencesKey("notifications")

    val notificationsEnabled: Flow<Boolean> =
        context.stepWalkerDataStore.data.map { it[notificationsKey] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.stepWalkerDataStore.edit { it[notificationsKey] = enabled }
    }
}
