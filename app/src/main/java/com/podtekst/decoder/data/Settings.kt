package com.podtekst.decoder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "podtekst_settings")

object Settings {
    private val KEY_RELAY_URL = stringPreferencesKey("relay_url")
    private val KEY_RELAY_KEY = stringPreferencesKey("relay_key")
    private val KEY_MODEL = stringPreferencesKey("model")

    fun relayUrl(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_RELAY_URL] ?: DEFAULT_RELAY_URL }

    fun relayKey(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_RELAY_KEY] ?: "" }

    fun model(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_MODEL] ?: DEFAULT_MODEL }

    suspend fun setRelayUrl(ctx: Context, url: String) {
        ctx.dataStore.edit { it[KEY_RELAY_URL] = url.trim().trimEnd('/') }
    }

    suspend fun setRelayKey(ctx: Context, key: String) {
        ctx.dataStore.edit { it[KEY_RELAY_KEY] = key.trim() }
    }

    suspend fun setModel(ctx: Context, model: String) {
        ctx.dataStore.edit { it[KEY_MODEL] = model.trim() }
    }

    suspend fun read(ctx: Context): Triple<String, String, String> {
        val prefs = ctx.dataStore.data.first()
        return Triple(
            prefs[KEY_RELAY_URL] ?: DEFAULT_RELAY_URL,
            prefs[KEY_RELAY_KEY] ?: "",
            prefs[KEY_MODEL] ?: DEFAULT_MODEL,
        )
    }

    const val DEFAULT_RELAY_URL = "http://127.0.0.1:8080"
    const val DEFAULT_MODEL = "local"
}
