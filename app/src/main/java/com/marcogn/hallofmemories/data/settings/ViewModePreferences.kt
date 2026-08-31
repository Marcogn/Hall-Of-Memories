package com.marcogn.hallofmemories.data.settings

import android.content.Context
import androidx.core.content.edit
import com.marcogn.hallofmemories.domain.model.ViewMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "view_mode_prefs"
private const val KEY_HOME = "home_view_mode"

/** Persists the Home hack library's list/grid choice. Plain SharedPreferences — one flag doesn't need DataStore. */
@Singleton
class ViewModePreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var homeViewMode: ViewMode
        get() = prefs.getString(KEY_HOME, null).toViewMode()
        set(value) = prefs.edit { putString(KEY_HOME, value.name) }

    private fun String?.toViewMode(): ViewMode =
        this?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.LIST
}
