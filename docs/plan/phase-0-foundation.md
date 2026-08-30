# Phase 0 — Foundation

**Goal:** a real, buildable, installable Hall of Memories app that starts,
shows a themed empty Home, switches theme and language, and is verified by CI.
No data, no network, no features.

**Depends on:** nothing. This is the first commit of actual code.

---

## 1. Project setup

Root of the repository (`marcogn/hall-of-memories`). Mirror
ThePatientGamerHelper's layout exactly: a single-module Gradle build with
`:app`, Kotlin DSL, a version catalogue.

### Files

```
settings.gradle.kts            rootProject.name = "HallOfMemories"; include(":app")
build.gradle.kts               plugin aliases, all `apply false`
gradle/libs.versions.toml      the pinned catalogue below
gradle.properties              android.useAndroidX=true, org.gradle.jvmargs=-Xmx2048m,
                               kotlin.code.style=official, android.nonTransitiveRClass=true
gradlew, gradlew.bat, gradle/wrapper/*   Gradle 8.13 wrapper (generate, commit, chmod +x gradlew)
.gitignore                     JetBrains/Android template + local.properties + /app/build + .gradle
app/build.gradle.kts
app/proguard-rules.pro         empty default
app/src/main/AndroidManifest.xml
```

### `gradle/libs.versions.toml`

Copy ThePatientGamerHelper's catalogue **verbatim**, then delete the entries
this app has no use for: `work`, `hiltWork`, `credentials`, `googleid`,
`playServicesAuth` (all Drive/WorkManager-only — v1 has neither). Keep every
version number as-is; they are a known-good combination.

Pinned, for the record: `agp 8.13.0`, `kotlin 2.0.21`, `ksp 2.0.21-1.0.28`,
`coreKtx 1.13.1`, `lifecycle 2.8.7`, `activityCompose 1.9.3`,
`composeBom 2024.12.01`, `navigationCompose 2.8.5`, `room 2.6.1`,
`hilt 2.52`, `hiltNavigationCompose 1.2.0`,
`kotlinxSerializationJson 1.7.3`, `kotlinxCoroutines 1.9.0`, `coil 2.7.0`,
`junit 4.13.2`, `androidxTestExtJunit 1.3.0`, `espresso 3.6.1`,
`robolectric 4.16.1`, `datastorePreferences 1.1.1`, `appcompat 1.7.0`.

### `app/build.gradle.kts`

```
namespace / applicationId  com.marcogn.hallofmemories
compileSdk 36, minSdk 26, targetSdk 36
versionCode 1, versionName "0.1.0"
JavaVersion.VERSION_17 / jvmTarget "17"
buildFeatures { compose = true; buildConfig = true }
testOptions { unitTests { isIncludeAndroidResources = true } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```

Build types:

- `debug`: `buildConfigField("boolean", "SEED_DEBUG_DATA", "true")`.
- `release`: `isMinifyEnabled = false`, `SEED_DEBUG_DATA = false`, and the
  **same conditional signing block** as ThePatientGamerHelper — a `release`
  `signingConfig` populated from `RELEASE_KEYSTORE_PATH`,
  `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
  environment variables, applied only when `RELEASE_KEYSTORE_PATH` is
  non-blank so a local `assembleRelease` with no secrets still builds
  (unsigned) instead of failing. The keystore itself is Phase 6; the wiring
  goes in now so Phase 6 is only secrets and docs.

There is **no** `DRIVE_OAUTH_WEB_CLIENT_ID` / `local.properties` reading in
this app — no Drive in v1. TheGamesDB's key is entered at runtime (Phase 2),
never at build time.

Dependencies: core-ktx, lifecycle (runtime-ktx, viewmodel-compose,
runtime-compose), activity-compose, the Compose BOM (ui, ui-graphics,
ui-tooling-preview, material3, material-icons-extended, debug ui-tooling and
ui-test-manifest), navigation-compose, Room (runtime, ktx, ksp compiler),
Hilt (android, ksp compiler, hilt-navigation-compose),
kotlinx-serialization-json, kotlinx-coroutines-android, coil-compose,
datastore-preferences, appcompat. Tests: junit, kotlinx-coroutines-test,
robolectric, androidx-test-ext-junit; androidTest: ext-junit, espresso,
compose ui-test-junit4.

---

## 2. Application, Activity, manifest

```
app/src/main/java/com/marcogn/hallofmemories/
  HallOfMemoriesApplication.kt     @HiltAndroidApp, nothing else yet
  MainActivity.kt                  @AndroidEntryPoint, AppCompatActivity
```

`MainActivity` **must extend `AppCompatActivity`, not `ComponentActivity`** —
`AppCompatDelegate.setApplicationLocales()` (the in-app language picker)
is silently ignored otherwise. This was a real, hard-to-diagnose bug in
ThePatientGamerHelper. As a consequence `res/values/themes.xml` must define
`Theme.HallOfMemories` descending from a `Theme.AppCompat.*` parent
(`Theme.AppCompat.DayNight.NoActionBar`). `setContent {}` stays the only UI
entry point; no XML layouts.

`MainActivity.onCreate` → `enableEdgeToEdge()` → `setContent { HallOfMemoriesApp() }`,
where `HallOfMemoriesApp` reads `ThemeViewModel.themeMode`, maps
`SYSTEM/LIGHT/DARK` onto `isSystemInDarkTheme()`, and wraps the nav graph in
`HallOfMemoriesTheme` + `Surface`.

`AndroidManifest.xml`:

- `<uses-permission android:name="android.permission.INTERNET" />` — needed
  from Phase 1 (PokéAPI) onward; declare it now with a comment saying why.
- `<application android:name=".HallOfMemoriesApplication"` … `android:label="@string/app_name"`,
  `android:localeConfig="@xml/locales_config"`, `android:theme="@style/Theme.HallOfMemories"`,
  `android:supportsRtl="true"`, launcher + round icons.
- The `AppLocalesMetadataHolderService` block with `autoStoreLocales=true`
  meta-data, copied from ThePatientGamerHelper — this is what persists the
  language choice without custom storage.
- **No** `androidx.startup.InitializationProvider` removal: that is only
  needed when `Application` implements `Configuration.Provider` for
  WorkManager, which this app does not.
- `MainActivity` exported with the LAUNCHER intent filter.

---

## 3. Theme and resources

```
ui/theme/Color.kt      light + dark colour roles
ui/theme/Type.kt       Material 3 typography (defaults are fine)
ui/theme/Theme.kt      HallOfMemoriesTheme(darkTheme, content), dynamic colour ON for API 31+
ui/theme/ThemeViewModel.kt   @HiltViewModel, exposes themeMode: StateFlow<ThemeMode> via stateIn
data/settings/ThemePreferences.kt   Preferences DataStore, name "settings_prefs"
domain/model/ThemeMode.kt   enum SYSTEM, LIGHT, DARK
```

`ThemePreferences` follows ThePatientGamerHelper's file of the same name:
a `by preferencesDataStore(name = "settings_prefs")` extension on `Context`,
a `stringPreferencesKey("theme_mode")`, a `Flow<ThemeMode>` that
`runCatching { ThemeMode.valueOf(it) }` and defaults to `SYSTEM`, and a
`suspend fun setThemeMode`. Use the **same DataStore instance/name** for the
other settings added in Phase 1 (`alwaysUseLatestSprites`) — one DataStore
file for app settings, not one per preference.

> Note, deliberate: this app's enums are named in **English**
> (`SYSTEM/LIGHT/DARK`), unlike ThePatientGamerHelper's Italian ones. Code and
> docs are English-only here; only the UI strings are bilingual.

Resources:

```
res/values/strings.xml        Italian (default locale)
res/values-en/strings.xml     English
res/values/themes.xml         Theme.HallOfMemories -> Theme.AppCompat.DayNight.NoActionBar
res/xml/locales_config.xml    <locale android:name="it"/> <locale android:name="en"/>
res/mipmap-*/ic_launcher*     placeholder launcher icon (a real icon is Phase 6)
```

Seed both string files with, at minimum: `app_name` (= `Hall of Memories`,
identical in both), the shared actions (`action_save`, `action_cancel`,
`action_delete`, `action_edit`, `action_confirm`), content descriptions
(`cd_back`, `cd_menu`), the drawer labels, the home empty state and every
settings string used below. **The two files must always have the same key
set** — a key present in only one silently falls back to Italian.

---

## 4. Navigation skeleton

```
ui/navigation/Destinations.kt          sealed interface Destination, @Serializable objects/classes
ui/navigation/HallOfMemoriesNavGraph.kt  ModalNavigationDrawer wrapping the NavHost
```

Type-safe routes via `kotlinx.serialization`, as in ThePatientGamerHelper.
Declare the full v1 route set now, even though most screens are placeholders:

```kotlin
sealed interface Destination {
    @Serializable data object Home : Destination                  // hack list, startDestination
    @Serializable data object Templates : Destination
    @Serializable data object Settings : Destination
    @Serializable data class HackDetail(val hackId: String) : Destination
    @Serializable data class HackForm(val hackId: String? = null) : Destination
    @Serializable data class HofDetail(val entryId: String) : Destination
    @Serializable data class HofForm(val hackId: String, val entryId: String? = null) : Destination
}
```

The drawer (`ModalNavigationDrawer`, `drawerState` hoisted at graph level) has
three entries — Hall of Memories (Home), Templates, Settings. Drawer
navigation uses `popUpTo(Destination.Home) { saveState = true }` +
`launchSingleTop = true` + `restoreState = true` so the back stack does not
grow. Screens receive only `onMenuClick: () -> Unit`, never the drawer state
(unidirectional data flow).

**Known trap, apply it pre-emptively:** the system back gesture bypasses a
screen's custom `onBack` lambda — Compose Navigation's own callback just calls
`popBackStack()`. Any screen that later needs custom back behaviour (the
Settings screen, the entry form's save-as-draft) must add an explicit
`BackHandler`. ThePatientGamerHelper hit this twice before recognising it.

Phase 0 screens: `HomeScreen` (top bar + menu icon + "no hacks yet" empty
state + a FAB that does nothing yet), `SettingsScreen` (theme + language
only), `TemplatesScreen` (empty state). The three detail/form destinations
can be `TODO()`-free stubs rendering a centered "Coming in phase N" text —
they must compile and navigate.

---

## 5. Settings screen (partial)

```
ui/settings/SettingsScreen.kt, SettingsViewModel.kt, SettingsUiState.kt, AppLanguage.kt
```

Two working controls only:

- **Theme**: three radio rows (System / Light / Dark) writing through
  `ThemePreferences`. `SettingsScreen` and `MainActivity` each obtain their own
  `hiltViewModel<ThemeViewModel>()`; both read the same DataStore and stay in
  sync without a shared scope.
- **Language**: System / Italiano / English via
  `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(...))`.
  No manual `recreate()` — with `AppCompatActivity` the switch applies
  end-to-end on its own.

Everything else on the screen comes in later phases; do not add disabled rows
for them yet (the Drive "Coming soon" row is Phase 5).

---

## 6. Hilt

```
di/DatabaseModule.kt   — NOT in this phase (no database yet)
```

Phase 0 needs no Hilt modules: `@HiltAndroidApp`, `@AndroidEntryPoint`,
`@HiltViewModel` and constructor injection cover it. Create `di/` in Phase 1.

---

## 7. CI

```
.github/workflows/android-ci.yml
```

Copy ThePatientGamerHelper's, minus the `DRIVE_OAUTH_WEB_CLIENT_ID` env var:
checkout → JDK 17 (temurin) → `android-actions/setup-android@v4` →
`gradle/actions/setup-gradle@v6` → `chmod +x gradlew` → `lintDebug` →
`testDebugUnitTest` → `assembleDebug`, uploading the lint and test reports as
artifacts with `if: always()`. Triggers: push and pull_request on `main`.

The `Build APK` and `Release` workflows are Phase 6.

---

## 8. Tests

There is almost nothing pure to test yet. Write one, to prove the JVM test
source set and Robolectric wiring work:

- `ThemePreferencesTest` (Robolectric): writing a mode and reading it back
  through the `Flow`, plus the "unknown stored value falls back to SYSTEM"
  case.

---

## 9. Docs to write in this phase

- `CLAUDE.md` — already written by the planning session; tick Phase 0 in its
  status list when done.
- `CHANGELOG.md` — `## [Unreleased]` with the Phase 0 entry.
- `docs/test-plan.md` — add the "Phase 0" section: app installs, launches,
  drawer opens, all three drawer destinations reachable, theme switch applies
  immediately, language switch relabels the UI and survives an app restart.

---

## 10. Definition of done

- [ ] `./gradlew assembleDebug` produces an installable APK (or CI does).
- [ ] `./gradlew testDebugUnitTest lintDebug` green.
- [ ] App launches to Home; drawer navigates to all three sections.
- [ ] Theme radio changes the app's colours immediately.
- [ ] Language radio switches IT/EN and the choice survives a cold start.
- [ ] `values/strings.xml` and `values-en/strings.xml` have identical key sets.
- [ ] `Android CI` green on the pushed branch.

## 11. Pitfalls

- `FlowRow`/`FlowColumn` need an explicit `@OptIn(ExperimentalLayoutApi::class)`
  on this Compose BOM, and the missing annotation is a **build error**, not a
  warning.
- `LazyVerticalStaggeredGrid` needs `@OptIn(ExperimentalFoundationApi::class)`.
- Getting `Theme.HallOfMemories` wrong (a `Theme.Material3` parent instead of
  `Theme.AppCompat`) makes the app crash on launch with an
  `AppCompatActivity` — "You need to use a Theme.AppCompat theme".
- Do not add a `fallbackToDestructiveMigration()` habit into the Room builder
  in Phase 1 "just to get going". It is banned.
