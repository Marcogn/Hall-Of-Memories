package com.marcogn.hallofmemories.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The three language choices exposed in Settings. [tag] is the BCP-47 tag passed to
 * [AppCompatDelegate.setApplicationLocales]; `null` means "follow the system locale".
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ITALIAN("it"),
    ENGLISH("en"),
}

/**
 * Applies the choice via [AppCompatDelegate.setApplicationLocales]. Persistence is automatic
 * (`autoStoreLocales`, see AndroidManifest.xml's `AppLocalesMetadataHolderService`), and this
 * call also recreates the current Activity end to end — no manual `recreate()` needed, since
 * `MainActivity` is an `AppCompatActivity`. See CLAUDE.md, "Known gotchas".
 */
fun applyAppLanguage(language: AppLanguage) {
    val locales = if (language.tag == null) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(language.tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

/** Reads back the currently applied choice from [AppCompatDelegate]. */
fun currentAppLanguage(): AppLanguage {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
    return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
}
