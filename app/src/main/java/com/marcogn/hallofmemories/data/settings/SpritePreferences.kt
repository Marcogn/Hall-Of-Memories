package com.marcogn.hallofmemories.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val ALWAYS_USE_LATEST_SPRITES_KEY = booleanPreferencesKey("always_use_latest_sprites")

/**
 * Spec §3.4: when on, ignores a hack's [com.marcogn.hallofmemories.domain.model.GameGeneration]
 * and always shows the most recent artwork. Uses the same `settings_prefs` DataStore file as
 * [ThemePreferences] — one file for every app-wide setting, not one per preference.
 */
@Singleton
class SpritePreferences @Inject constructor(@ApplicationContext private val context: Context) {

    val alwaysUseLatestSprites: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[ALWAYS_USE_LATEST_SPRITES_KEY] ?: false
    }

    suspend fun setAlwaysUseLatestSprites(value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[ALWAYS_USE_LATEST_SPRITES_KEY] = value }
    }
}
