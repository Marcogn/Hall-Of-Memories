# Hall of Memories

[![Android CI](https://github.com/marcogn/hall-of-memories/actions/workflows/android-ci.yml/badge.svg)](https://github.com/marcogn/hall-of-memories/actions/workflows/android-ci.yml)
[![Latest release](https://img.shields.io/github/v/release/marcogn/hall-of-memories?include_prereleases)](https://github.com/marcogn/hall-of-memories/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![minSdk 26](https://img.shields.io/badge/minSdk-26-brightgreen.svg)](app/build.gradle.kts)

A native Android app for cataloguing the Hall of Fame of your playthroughs —
official games and ROM hacks alike. Every finished run becomes a permanent
record: the six-strong team with its nicknames, natures, abilities, held
items, IVs, EVs and moves, the trainer behind it, the playtime, and the
screenshot of the moment it happened.

Offline-first, single user, no account, no telemetry.

## Features

### Hacks

Catalogue each game or ROM hack you play — name, generation, base game
title, notes. Box art and logo are searched on TheGamesDB (matched to the
hack's generation) or picked from your gallery; TheGamesDB has no ROM hacks
in it, so a gallery pick or a generated placeholder is the normal path here,
not a fallback for an error. Search and generation filters, list/grid view
on the home screen.

### Halls of Fame

Under each hack, one record per completed run: six Pokémon slots in full
detail (species, nickname, gender, level, shiny, nature, ability, held
item, up to four moves, all twelve IV/EV stats), the trainer's name and ID,
the playtime, an optional screenshot (gallery or camera, pinch-zoom and
double-tap-zoom to view), and the date you finished. Dropdowns for
nature/ability/item/moves are typing aids over real PokéAPI data, never a
legality filter — any free text is accepted, only numeric ranges are
validated. **An entry is editable forever** — there is no post-save lock.
A hack with 2–6 entries shows them as a scrollable carousel; more than that
switches to a sortable list.

### Reusable Pokémon templates

Configure a Pokémon once, save it as a template, and drop it into any Hall
of Fame in any hack. Deleting a template never touches the entries already
built from it — it carries no foreign key, only provenance.

### Generation-accurate sprites

A Gen-3 hack shows Gen-3 sprites, shinies included, with a verified
fallback chain to modern artwork where a sprite never existed for that
generation. Sprite URLs are always derived from
`(species, generation, shiny)`, never stored, so they can't go stale.

### Local backup

Export everything — every hack and its artwork, every Hall of Fame entry
with its screenshot and six slots, every saved template — into a single zip
via the system's file picker, and import it back on this or any other
device. Restoring asks for explicit confirmation first (it fully replaces
what's on the device) and a malformed or newer-than-supported backup file
is rejected with nothing written. The PokéAPI reference cache is never part
of a backup — it's re-downloadable, not user data. Google Drive backup is a
planned v2 feature; v1 ships only a disabled placeholder for it in Settings.

## Screenshots

*(coming soon — the app has no published screenshots yet.)*

## Download

Grab the latest signed APK from the
[Releases page](https://github.com/marcogn/hall-of-memories/releases).
Android will warn about installing from outside the Play Store (this app
isn't published there) — that's Play Protect being cautious about any
sideloaded APK, not a finding specific to this one.

## Building from source

```bash
git clone https://github.com/marcogn/hall-of-memories.git
cd hall-of-memories
./gradlew assembleDebug
```

Needs an Android SDK (`minSdk` 26, `compileSdk`/`targetSdk` 36) and JDK 17.
No build-time secret is required for a debug build — TheGamesDB's API key is
entered at runtime in Settings, not baked into the APK. A signed release
build additionally needs the four `RELEASE_KEYSTORE_*` environment variables
described in [`docs/release-signing.md`](docs/release-signing.md); without
them `assembleRelease` still succeeds, just unsigned.

```bash
./gradlew testDebugUnitTest   # JVM unit tests (domain + Robolectric)
./gradlew lintDebug           # Android Lint
```

## Privacy

No account, no analytics, no telemetry, no backend of its own. Network
access is used only for the PokéAPI reference data (species/types/moves/
abilities/items/natures, fetched once and cached), sprite images, and
TheGamesDB artwork search (only if you enter an API key). Everything you
enter — hacks, Halls of Fame, templates, screenshots — stays in the app's
local Room database and internal storage unless you explicitly export a
backup yourself.

## Tech stack

Kotlin, Jetpack Compose, Material 3, Room, Hilt, Coil, DataStore. HTTP
clients for PokéAPI and TheGamesDB are hand-rolled over
`java.net.HttpURLConnection` rather than a full networking dependency; local
backup uses `java.util.zip` directly. No dependency is added without an
explicit need — see [`CLAUDE.md`](CLAUDE.md) for the pinned catalogue.

## Project structure

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

## Documentation

| | |
|---|---|
| [`docs/spec.md`](docs/spec.md) | Functional specification |
| [`docs/plan/README.md`](docs/plan/README.md) | Phase-by-phase implementation plan |
| [`docs/plan/reference-pokeapi.md`](docs/plan/reference-pokeapi.md) | Measured PokéAPI + sprite reference |
| [`docs/implementation-decisions.md`](docs/implementation-decisions.md) | Why things are the way they are |
| [`docs/test-plan.md`](docs/test-plan.md) | Manual, on-device verification |
| [`docs/release-signing.md`](docs/release-signing.md) | Keystore, signing, and the release workflow's secrets |
| [`CHANGELOG.md`](CHANGELOG.md) | Release notes |
| [`CLAUDE.md`](CLAUDE.md) | Guide for AI coding agents |

## Demo data

Debug builds can seed the database with sample hacks and Halls of Fame for
UI development, behind `BuildConfig.SEED_DEBUG_DATA`
(`data/debug/DebugSeeder.kt`). It never runs in a release build, and no mock
data ships in the shipped UI otherwise.

## About this project

Built collaboratively with [Claude Code](https://claude.ai/code) —
architecture, phase planning, implementation, tests and this documentation
were developed across a series of guided sessions, following patterns
established in the author's earlier sibling app,
[ThePatientGamerHelper](https://github.com/marcogn/thepatientgamerhelper).

## License

[MIT](LICENSE)

## Disclaimer

Not affiliated with, endorsed by, or associated with Nintendo, Game Freak or
The Pokémon Company. Pokémon data is fetched at runtime from
[PokéAPI](https://pokeapi.co/); artwork for games is fetched from
[TheGamesDB](https://thegamesdb.net/). No game assets are bundled with this
app.
