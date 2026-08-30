# CLAUDE.md

Guide for AI coding agents working on this repository. Read it in full before
editing anything, then read the phase plan you are executing.

- [`docs/spec.md`](docs/spec.md) — the authoritative functional
  specification. Anything not in it, or in a phase plan, is out of scope.
- [`docs/plan/README.md`](docs/plan/README.md) — how the phased build is
  executed, the working rules, and the definition of done for every phase.
- [`docs/plan/reference-pokeapi.md`](docs/plan/reference-pokeapi.md) —
  measured endpoint sizes, response shapes and the verified sprite URL table.
- [`docs/implementation-decisions.md`](docs/implementation-decisions.md) —
  non-obvious choices and why they were made. Add to it as you go.
- [`docs/test-plan.md`](docs/test-plan.md) — manual, on-device verification.
  One new section per phase; one "Known regressions" entry per real bug found.

## What this project is

**Hall of Memories** (HoM) — a native, single-user, offline-first Android app
for cataloguing the Hall of Fame of Pokémon playthroughs, official games and
ROM hacks alike. Kotlin + Jetpack Compose + Material 3, Room, Hilt,
ViewModel/StateFlow with unidirectional data flow.

| | |
|---|---|
| Repository | `marcogn/hall-of-memories` |
| Gradle root project | `HallOfMemories` |
| `applicationId` / package | `com.marcogn.hallofmemories` |
| Database file | `hall_of_memories.db` |
| minSdk / targetSdk | 26 / 36 |

Public branding is franchise-neutral: no "Pokémon" trademark in the app label,
package name or repository name. Species, move and ability names come from
PokéAPI at runtime and are never bundled as assets.

## Sibling projects

- **ThePatientGamerHelper** — same author, same stack, eight shipped phases.
  It is the reference implementation for Gradle setup, Hilt modules, Room +
  `Flow` + `combine()` ViewModels, `ImageStorage`, SAF export/import,
  hand-rolled `HttpURLConnection` clients, and all three CI workflows.
  **Copy its patterns rather than inventing new ones.**
- **CoverDex** — a TypeScript PWA. None of its code is reused; its PokéAPI
  acquisition logic and cache-versioning discipline are, in Kotlin.

## Progress status by phase

- **Phase 0 — Foundation**: ⬜ not started
- **Phase 1 — Room schema + PokéAPI sync + sprites**: ⬜ not started
- **Phase 2 — Hacks, TheGamesDB, box art/logo**: ⬜ not started
- **Phase 3 — Hall of Fame entries + six-slot editor**: ⬜ not started
- **Phase 4 — Reusable Pokémon templates**: ⬜ not started
- **Phase 5 — Presentation polish + local backup**: ⬜ not started
- **Phase 6 — Signing, release pipeline, docs**: ⬜ not started
- **v2 (separate spec)** — Google Drive backup, deliberately out of v1.

Tick these off as phases land. Do not implement anything not present in
`docs/spec.md` or a phase plan unless a new session explicitly asks for it.

## Product decisions already made (do not ask again)

- **Slots are always six per Hall of Fame**, rows included for empty ones, so
  slot identity is stable across edits.
- **No legality validation** of moves, abilities or natures — dropdowns are
  typing aids over real PokéAPI data, and any free text is accepted. Only
  range validation applies (level 1–100, IV 0–31, EV 0–252, EV total ≤ 510).
- **Player ID is free text**, covering every generation's and hack's format.
- **A Hall of Fame is editable forever.** There is no post-save lock.
- **Species/nature/ability/item/move values on a slot are denormalized
  snapshots**, not references into the PokéAPI cache. Wiping the cache must
  never alter or blank saved user data.
- **`sourceTemplateId` carries no foreign key** — deleting a template must not
  cascade into, or block, the Halls of Fame made from it.
- **Sprite URLs are derived, never stored**, from
  `(speciesId, generation, shiny, alwaysUseLatestSprites)`.
- **Box art and logo are user-overridable and optional.** TheGamesDB has no
  ROM hacks in it; a gallery pick or a generated placeholder is the normal
  path, not an error path.
- **Backups never contain the PokéAPI cache** — it is re-downloadable data.
- **Restore is a full replace**, single transaction, ids and timestamps
  preserved. No merging, no conflict resolution.

## Architecture

```
com.marcogn.hallofmemories
├── data/
│   ├── local/        Room: entity/, dao/, Converters, HallOfMemoriesDatabase, Migrations
│   ├── repository/   repository implementations (transactional) + Mappers
│   ├── pokeapi/      PokeApiClient (HttpURLConnection), PokedexSyncManager
│   ├── thegamesdb/   TheGamesDbApiClient, TheGamesDbPreferences, GameArtSearchCoordinator
│   ├── image/        ImageStorage (internal storage, downsample + compress)
│   ├── backup/       BackupArchive (zip), LocalBackupManager (SAF)
│   ├── settings/     ThemePreferences (DataStore), ViewModePreferences (SharedPreferences)
│   └── debug/        DebugSeeder, behind BuildConfig.SEED_DEBUG_DATA
├── domain/
│   ├── model/        pure models, enums, conversions — no Android imports
│   ├── pokeapi/      pure parsers over JSON strings, SyncStage, schema version
│   ├── sprite/       SpriteVariant + SpriteUrlResolver (pure, unit-tested)
│   ├── filter/       pure filter/sort functions
│   ├── validation/   pure slot validation
│   ├── backup/       BackupPayload DTOs + mapping
│   └── repository/   repository interfaces
├── di/               Hilt modules (Database, Repository, Coroutines)
└── ui/
    ├── theme/        Material 3 theme + ThemeViewModel
    ├── navigation/   type-safe routes, ModalNavigationDrawer around the NavHost
    ├── home/         hack library
    ├── hack/         hack form + hack detail
    ├── hof/          Hall of Fame form, slot editor, entry detail
    ├── templates/    reusable Pokémon templates
    ├── settings/     theme, language, sprites, pokédex data, API key, backup
    └── common/       shared composables (PokemonSprite, EditableComboBox, StatGrid, ...)
```

**Room is the single source of truth**, exposed as `Flow`. ViewModels
`combine()` repository flows with local UI state into one `StateFlow` of UI
state; events flow up as lambdas, state flows down. Pure logic lives in
`domain/` with no Android imports so it is testable on the plain JVM without
Robolectric.

## Code conventions

- **Code, comments, commits and docs are English.** Only the UI's string
  resources are bilingual: `res/values/strings.xml` is Italian (the default
  locale), `res/values-en/strings.xml` English. Add a key to both in the same
  commit — a key present in only one silently falls back to Italian.
- **No hardcoded user-visible strings**: `stringResource()` in Compose,
  `context.getString()` in ViewModels (inject `@ApplicationContext`).
- **Enum labels are not on the enum.** A `@Composable fun X.displayName()` in
  `ui/common/` resolves the string resource, keeping `domain/` Android-free.
- Dates and times: `java.time.Instant` / `LocalDate` (available from API 26,
  our minSdk, without desugaring). Stored as epoch millis.
- Ids: `String` UUIDs for user data, generated in the repository. PokéAPI
  cache tables keep PokéAPI's own `Int` ids.
- **Room migrations are additive and numbered.**
  `fallbackToDestructiveMigration()` is banned — the app holds data that
  cannot be re-created.
- **No new dependencies** without an explicit request or a genuine need. The
  catalogue is pinned in Phase 0. HTTP clients, image fallback chains and
  placeholder art are all hand-rolled here, exactly as in the sibling app.
- No mock data in shipped UI; the only seed is `data/debug/DebugSeeder.kt`
  behind `BuildConfig.SEED_DEBUG_DATA` (debug builds only).

## Known gotchas

Carried over from the sibling projects; each one cost real debugging time.

- **`MainActivity` must extend `AppCompatActivity`.** With
  `ComponentActivity`, `AppCompatDelegate.setApplicationLocales()` is silently
  ignored and the in-app language picker does nothing. The app theme must then
  descend from `Theme.AppCompat.*`.
- **The system back gesture bypasses a screen's custom `onBack`** — Compose
  Navigation's own callback just calls `popBackStack()`. Any screen with
  custom back logic needs an explicit `BackHandler`.
- **`FlowRow`/`FlowColumn` need `@OptIn(ExperimentalLayoutApi::class)`** and
  `LazyVerticalStaggeredGrid` needs `@OptIn(ExperimentalFoundationApi::class)`
  on this Compose BOM — a missing annotation is a build error, not a warning.
- **kotlinx.serialization defaults do not cover an explicit `null`.** A
  default value only fills a *missing* key; `"field": null` still throws
  unless the type is nullable. Set `coerceInputValues = true` too.
- **`Json.encodeToString(value)`** without
  `import kotlinx.serialization.encodeToString` resolves to the wrong overload
  and fails with a misleading type error.
- **Do not set `Accept-Encoding` on `HttpURLConnection`.** Left alone it
  negotiates gzip and decompresses transparently; set it by hand and you get
  raw gzip bytes.
- **Third-party sites reject non-browser User-Agents misleadingly.**
  TheGamesDB answers an app-like UA with "invalid API key" for a valid key.
  Keep the realistic desktop UA string.
- **PokéAPI per-Pokémon records are ~530 KB each.** Never sync them; see
  `docs/plan/reference-pokeapi.md` for what to fetch instead.
- **Never `clearAllTables()`** — cache invalidation must name the cache
  tables explicitly or it takes the user's Halls of Fame with it.

## Build/test commands

```bash
./gradlew assembleDebug            # debug APK
./gradlew testDebugUnitTest        # JVM unit tests (domain + Robolectric)
./gradlew lint                     # Android Lint
./gradlew connectedDebugAndroidTest  # instrumented tests (needs a device)
```

Real verification happens in `.github/workflows/android-ci.yml` on every
push/PR. A sandboxed session may have the Maven repositories reachable but no
Android SDK — check `$ANDROID_HOME` and `command -v sdkmanager` before
assuming a local build is possible, and fall back to CI rather than fighting
it. Never report a build as passing that you did not run.

Testing approach: pure JVM unit tests for `domain/` (parsers, sprite
resolution, filters, validation, mappers, backup DTOs); Robolectric as JVM
tests for Room DAOs, repositories and zip archives. Anything needing the real
network (PokéAPI sync, TheGamesDB), the photo picker, the camera, locale
switching or actual image rendering is verified by hand — see
`docs/test-plan.md`.

## Changelog and release process

`CHANGELOG.md` is the release-notes source of truth. **Every change gets its
entry when it is made**, under `## [Unreleased]` at the top — never deferred
to release time.

Entry convention: one top-level bullet per significant, user-facing change,
leading with a short bold summary — `- **Summary.** further detail…` — with
nested bullets for detail. The `Release` workflow extracts exactly those bold
lead-ins into the GitHub Release body, followed by a link back to the
changelog section. Keep the bold span short and skimmable.

Cutting a release (Phase 6 onward) is a manual `Release` workflow dispatch
with the new `x.y.z` typed in; the workflow validates it, cuts the changelog,
bumps the version, builds, signs, publishes, and only then pushes the bump to
`main`.

## What NOT to do until explicitly requested

Google Drive backup (v2, separate spec — v1 ships only the disabled Settings
row and the repository seam). Any account or multi-user concept. Custom
Pokémon species. Legality validation. Battle or type-coverage analysis — that
is CoverDex's job. A statistics screen. Exporting a Hall of Fame as an image
or PDF.
