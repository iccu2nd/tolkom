package com.tinta.reader.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tinta_settings")

data class ReaderSettings(
    val keepScreenOn: Boolean = true,
    val pageGap: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private val keepOn = booleanPreferencesKey("reader_keep_screen_on")
    private val gap = booleanPreferencesKey("reader_page_gap")

    val reader: Flow<ReaderSettings> = context.dataStore.data.map {
        ReaderSettings(keepScreenOn = it[keepOn] ?: true, pageGap = it[gap] ?: false)
    }

    suspend fun setKeepScreenOn(value: Boolean) { context.dataStore.edit { it[keepOn] = value } }
    suspend fun setPageGap(value: Boolean) { context.dataStore.edit { it[gap] = value } }
}
