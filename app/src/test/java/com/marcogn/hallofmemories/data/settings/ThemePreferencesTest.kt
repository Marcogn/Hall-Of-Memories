package com.marcogn.hallofmemories.data.settings

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// sdk = 26 (minSdk), same as every Robolectric test in ThePatientGamerHelper: Robolectric's
// shadow jar for the app's compileSdk (36) requires a newer JDK than CI runs (see CLAUDE.md,
// "Known gotchas") — pinning to minSdk sidesteps that without weakening what's tested here.
@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class ThemePreferencesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val preferences = ThemePreferences(context)

    // Robolectric reuses the same on-disk filesDir across test methods within a run, and
    // preferencesDataStore() caches one DataStore per (Context, name) pair — without this, a
    // value written by one test is still on disk for the next one.
    @Before
    fun clearPersistedState(): Unit = runBlocking {
        context.settingsDataStore.edit { it.clear() }
    }

    @Test
    fun `defaults to SYSTEM when nothing was ever written`() = runTest {
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
    }

    @Test
    fun `written mode is read back`() = runTest {
        preferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
    }

    @Test
    fun `overwriting a mode replaces it rather than accumulating`() = runTest {
        preferences.setThemeMode(ThemeMode.DARK)
        preferences.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, preferences.themeMode.first())
    }

    @Test
    fun `an unknown stored value falls back to SYSTEM instead of crashing`() = runTest {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = "NOT_A_REAL_THEME_MODE" }

        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
    }
}
