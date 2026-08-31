package com.marcogn.hallofmemories.data.thegamesdb

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "thegamesdb_prefs"
private const val KEY_API_KEY = "api_key"

/**
 * Runtime-editable TheGamesDB API key (spec §3.9): entered by the user in Settings, never baked
 * into the build. TheGamesDB requires a key for every request since their 2026-02-17 policy
 * change — there is nothing to fall back to at build time, so the field starts empty and "Search
 * online" stays disabled until the user fills it in. Plain SharedPreferences, same
 * "no DataStore for one simple value" reasoning as `ThemePreferences`'s sibling counterparts.
 */
@Singleton
class TheGamesDbPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, value.trim())
        }
}
