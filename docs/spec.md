# Hall of Memories — Functional specification v1

Android app for cataloguing the Hall of Fame of your Pokémon playthroughs,
official games and ROM hacks alike. Single user, offline-first, no account.

Naming, fixed:

| Context | Value |
|---|---|
| Display name / UI | **Hall of Memories** |
| Short form (docs, commits) | **HoM** |
| GitHub repository | `marcogn/hall-of-memories` |
| Gradle root project | `HallOfMemories` |
| `applicationId` / Kotlin package | `com.marcogn.hallofmemories` |
| Room database file | `hall_of_memories.db` |

The public branding never uses the "Pokémon" trademark: the store/app label,
the package name and the repository name are all franchise-neutral. Species,
move and ability *names* inside the app come from PokéAPI at runtime and are
never bundled as app assets.

Sibling projects this one deliberately follows: **ThePatientGamerHelper**
(same stack, same architecture, same CI/release process — copy its patterns
rather than inventing new ones) and **CoverDex** (PokéAPI acquisition logic
and cache philosophy only; CoverDex is TypeScript, none of its code is
reused, only its lessons).

---

## 1. Assumptions — resolved

The source spec (`HallOfFamer v1`) listed A1–A11 as open. Their resolved
status for this plan:

| # | Assumption | Status |
|---|---|---|
| A1 | Kotlin + Jetpack Compose + Material 3 | **Confirmed.** Same stack and versions as ThePatientGamerHelper. |
| A2 | Room for local persistence | **Confirmed.** Single source of truth, exposed as `Flow`. |
| A3 | Retrofit/Ktor for PokéAPI and TheGamesDB | **Overridden — no HTTP client dependency.** Hand-rolled `HttpURLConnection` + `kotlinx.serialization` clients, exactly as `DriveApiClient`/`TheGamesDbApiClient` do in ThePatientGamerHelper. Total surface is ~6 endpoint shapes; Android's `HttpURLConnection` is OkHttp-backed (pooling, transparent gzip) so nothing is lost. See `docs/implementation-decisions.md`. |
| A4 | Coil for images, disk-cached | **Confirmed.** Remote sprites are Coil-cached; box art, logos and screenshots the user owns are copied into internal storage instead (see §6). |
| A5 | Hilt | **Confirmed.** |
| A6 | Each slot carries level and held item | **Confirmed**, plus gender, nature, ability, shininess, IVs, EVs and 4 moves. |
| A7 | Player ID is free text | **Confirmed.** Free text, no numeric constraint — covers 5-digit and 6-digit trainer IDs and non-standard hack formats. |
| A8 | English README/CHANGELOG/docs, IT+EN UI | **Confirmed.** Italian is the default resource locale, English the translation, exactly like ThePatientGamerHelper. |
| A9 | No legality validation of moves/abilities/natures | **Confirmed.** Dropdowns are typing aids populated from real PokéAPI data; anything can be saved. Only *range* validation applies (§3.4). |
| A10 | A Hall of Fame entry stays editable in every field forever | **Confirmed.** No post-save lock anywhere in the app. |
| A11 | ROM hacks reshuffle the official Pokédex; no custom species | **Confirmed.** Every species is resolvable through PokéAPI, so no custom-species editor exists in v1. |

One addition decided while planning, confirmed with the user:

- **A12 — Box art and logo are user-overridable.** Every hack can take its
  box art and logo from the gallery instead of from a search, and a hack
  with neither renders a generated placeholder. This is the normal path for
  hacks TheGamesDB doesn't have, not an error path.

  > **Correction, from real on-device use (post-v1):** the original premise
  > above — "TheGamesDB catalogues commercial releases, not fan-made ROM
  > hacks: a search for 'Radical Red' will find nothing" — was wrong.
  > TheGamesDB does catalogue many well-known ROM hacks directly, "Radical
  > Red" included. The search now defaults to the hack's own name first (see
  > `docs/implementation-decisions.md`, "Post-launch bug-fix round"); the
  > gallery/placeholder fallback above still applies, just for a narrower
  > set of hacks than originally assumed.

Two things considered and explicitly rejected for v1, confirmed with the
user — do not re-add without an explicit new request:

- **No secret ID field.** A second, hidden trainer identifier was considered
  (some generations distinguish a public Trainer ID from a secret one) and
  rejected. `HallOfFameEntry` has no `playerSecretId` column.
- **No shareable "trainer card" export.** Rendering a Hall of Fame entry as a
  shareable image is not wanted right now — the user has other, unspecified
  features in mind for that slot instead. Not a v2 placeholder to build
  toward; revisit only on explicit request.

---

## 2. Data model

Room is the single source of truth. Everything below is a Room entity except
`AppSettings`, which is DataStore.

```
Hack
  id                 String (UUID) PK
  name               String
  generation         GameGeneration (enum, stored as name)
  baseGameTitle      String?      -- provenance only: the TheGamesDB search result title, if
                                     any was picked; not a user-editable field (see A12's
                                     correction above — search defaults to the hack's own name)
  boxArtPath         String?      -- absolute path in internal storage
  boxArtUrl          String?      -- remote origin, kept for reference/re-download
  logoPath           String?
  logoUrl            String?
  theGamesDbId       Long?
  notes              String?
  createdAt          Instant
  updatedAt          Instant

HallOfFameEntry
  id                 String (UUID) PK
  hackId             String FK -> Hack.id, ON DELETE CASCADE, indexed
  playerName         String
  playerId           String       -- free text, may be blank
  playtimeText       String       -- as typed, e.g. "42:17"
  playtimeMinutes    Int?         -- parsed from playtimeText when parseable, for sorting/stats
  screenshotPath     String?
  insertedAt         Instant      -- defaults to now, user-editable
  notes              String?
  createdAt          Instant
  updatedAt          Instant

PokemonSlot
  id                 String (UUID) PK
  entryId            String FK -> HallOfFameEntry.id, ON DELETE CASCADE, indexed
  slotIndex          Int          -- 0..5, UNIQUE(entryId, slotIndex)
  speciesId          Int?         -- PokéAPI pokemon id (forms included, e.g. 10034); null = empty slot
  speciesName        String?      -- denormalized snapshot, see below
  nickname           String?
  gender             PokemonGender (MALE / FEMALE / UNKNOWN)
  level              Int?         -- 1..100 when set
  nature             String?
  ability            String?
  isShiny            Boolean
  heldItem           String?
  ivHp..ivSpe        Int?         -- six columns, 0..31 each
  evHp..evSpe        Int?         -- six columns, 0..252 each
  move1..move4       String?
  sourceTemplateId   String?      -- provenance only, no FK constraint (see below)

PokemonTemplate            -- reusable "saved Pokémon" pool, shared across hacks
  id                 String (UUID) PK
  label              String       -- user-given, e.g. "Competitive Garchomp"
  <every PokemonSlot payload field except id/entryId/slotIndex/sourceTemplateId>
  createdAt          Instant
  updatedAt          Instant

-- PokéAPI cache tables (§4). Wiping these must never touch the four above.
PokeSpecies   id Int PK, name, displayName, searchName, generationIntroduced Int?,
              primaryType String?, secondaryType String?
PokeMove      id Int PK, name, displayName, searchName, type String?
PokeNature    id Int PK, name, displayName, increasedStat String?, decreasedStat String?
PokeAbility   id Int PK, name, displayName, searchName
PokeItem      id Int PK, name, displayName, searchName
PokeCacheMeta key String PK (= SyncStage.name), lastSyncedAt Instant,
              schemaVersion Int, itemCount Int

AppSettings (DataStore Preferences, not Room)
  themeMode              LIGHT / DARK / SYSTEM
  language               IT / EN / SYSTEM        (AppCompatDelegate, autoStoreLocales)
  alwaysUseLatestSprites Boolean, default false
```

**Relationships.** `Hack` 1—N `HallOfFameEntry` 1—N `PokemonSlot`, exactly six
slots per entry, created together with the entry and never individually
deleted — an unused slot is a row with `speciesId = null`, not a missing row,
so `(entryId, slotIndex)` stays stable across every edit. `PokemonTemplate` is independent.

**Two invariants that outrank convenience:**

1. `PokemonSlot.speciesName` (and `nature`/`ability`/`heldItem`/`move*`, all
   stored as plain strings) is a **denormalized snapshot**, not a foreign key
   into the PokéAPI cache. "Invalidate and re-download" (§4) wipes the cache
   tables; a saved Hall of Fame must survive that completely intact, and must
   still render its team when the device is offline and the cache is empty.
   The cache is a typing aid, never a dependency of user data.
2. `sourceTemplateId` is provenance metadata with **no foreign-key
   constraint**: deleting a template must not cascade into, or block, the
   Hall of Fame entries that were once created from it.

---

## 3. Features

### 3.1 Hacks

- Home is the hack list (grid of box art, list fallback), FAB `+` to create.
  Long-pressing a hack enters multi-select: the top bar is replaced with
  close, edit (only when exactly one hack is selected), delete (with a
  confirmation naming how many Halls of Fame will be lost too) and select-all
  actions, and tapping any other hack toggles its selection instead of
  opening it.

  > **Revision, from real on-device use (post-v1):** the grid and list tiles
  > originally overlaid the logo on top of the box art (both became hard to
  > read) and, in list view, only the box-art thumbnail was tappable — the
  > title did nothing. Both were bugs, not intended behavior: this section
  > already described box-art-only tiles. Fixed alongside adding multi-select,
  > requested directly after using the app for real. See
  > `docs/implementation-decisions.md`, "Home library: tap targets and
  > multi-select".
- Creation form: name, generation (§3.5), notes, and a single optional
  "Search online" step against TheGamesDB (query defaults to the hack's own
  name — see A12's correction above) that fills box art + logo together.
  Both images can instead be picked from the gallery, and both can be
  cleared. With neither image the UI renders a deterministic placeholder
  derived from the hack name.
- Hack page: logo header (never box art — box art stays the identity shown
  on Home's list/grid instead), and a floating `+` that always opens the
  six-slot editor for a new entry, regardless of how many already exist.
  - 0 entries → empty state with a prominent "Add the first Hall of Fame".
  - N ≥ 1 → a list/carousel of entry previews, tapping one opens its detail
    view. A preview's thumbnail is the entry screenshot if present,
    otherwise the sprite of the Pokémon in slot 0.

  > **Revision, from real on-device use (post-v1):** originally, exactly 1
  > entry opened straight onto that entry's full detail view instead of a
  > tile, and the header showed box art with the logo overlaid rather than
  > the logo alone. Direct user feedback after using the app for real asked
  > for both changed — a consistent "tap a tile to open it" interaction
  > regardless of entry count, a `+` that's always reachable (it previously
  > existed only on the empty state), and a logo-only header since box art
  > was already redundant with Home's grid. See
  > `docs/implementation-decisions.md`, "Post-launch bug-fix round".
- Editing and deleting a hack is available from its page. Deleting a hack
  deletes its entries and slots (CASCADE) behind an explicit confirmation
  that names how many entries will be lost.

### 3.2 Hall of Fame entry

From a hack page, `+` opens the entry form:

1. Six Pokémon slots (§3.3).
2. Player name, player ID, playtime.
3. Optional screenshot, from gallery or camera.
4. Insertion date/time, defaulting to now, editable.
5. Free-text notes.

Saving is unrestricted — a half-filled entry is savable, and everything stays
editable afterwards (A10). The only hard requirement is a non-blank player
name; empty slots are legal and render as "empty".

### 3.3 Pokémon slot editor

- Species search with autocomplete over the local PokéAPI cache, showing the
  sprite for the current context (§3.6) next to each result.
- Fields: nickname, gender, level, nature, ability, shiny toggle, held item,
  six IVs, six EVs, up to four moves.
- Nature / ability / move / item inputs are **editable combo boxes**: the
  dropdown suggests cached PokéAPI values, and any free text is accepted (A9).
- "Save as reusable template" writes a `PokemonTemplate` from the current slot.
- "Load from template" fills the slot from any existing template, from any
  hack, and records `sourceTemplateId`.

### 3.4 Validation

Range only, never legality:

- level 1..100; IV 0..31; EV 0..252.
- Total EVs across the six stats may not exceed 510. Over the limit is shown
  as an inline error on the EV block and **blocks saving that slot**, which is
  the one exception to "anything can be saved" — it is an arithmetic bound,
  not a legality rule.
- Playtime accepts `H:MM`, `HH:MM`, `HHH:MM` and a bare number of hours; it is
  stored verbatim in `playtimeText` and, when parseable, also as
  `playtimeMinutes`. An unparseable value is kept as typed and never rejected.

### 3.5 Generations

`GameGeneration` is a closed enum used for two things: labelling a hack and
choosing its default sprite set (§3.6).

`RB, GSC, RSE, FRLG, DPPT, HGSS, BW, XY, ORAS, SM, USUM, SWSH, SV, OTHER`

`OTHER` covers fan games with no clear base generation and falls back to
official artwork.

### 3.6 Sprites

A slot's sprite is a pure function of `(speciesId, hack generation, isShiny,
alwaysUseLatestSprites)` — see `docs/plan/reference-pokeapi.md` for the
verified URL table. No sprite URL is ever stored in the database or fetched
during the PokéAPI sync: URLs are derived on demand and Coil handles caching.

When `alwaysUseLatestSprites` is on, `Hack.generation` is ignored and the most
recent artwork is used everywhere.

### 3.7 PokéAPI data (§4 below is the how)

- One-time sync on first launch, then local reads only.
- Settings → "Invalidate and re-download data" clears `PokeCacheMeta` plus the
  cache tables and re-runs the sync. User data is untouched.

### 3.8 Templates

A dedicated screen lists every saved template with its sprite and label, and
supports rename, edit, duplicate and delete. Deleting a template leaves every
Hall of Fame slot created from it exactly as it is.

### 3.9 Settings

- Theme: light / dark / system.
- Language: Italian / English / system.
- Always use the latest sprites (toggle).
- PokéAPI data: last sync timestamp per stage, "Invalidate and re-download".
- TheGamesDB API key (entered at runtime, never baked into a build).
- Local backup: export / import (§5).
- "Google Drive backup" row, **disabled, with a "Coming soon" badge** (§5).

---

## 4. PokéAPI acquisition

Source: the static mirror
`https://raw.githubusercontent.com/PokeAPI/api-data/master/data/api/v2`,
the same one CoverDex uses — no rate limit, no API key, numeric-ID paths only.

The whole sync is **~57 requests and ~2.5 MB**, because the app deliberately
never downloads per-Pokémon detail records (they are ~530 KB each; 1300 of
them would be ~650 MB). Types come from the 18 `type/{id}` records, which
carry both the Pokémon and the moves of that type; sprites are derived from a
URL convention instead of read out of the detail payload. Full measured
figures, endpoint shapes and the sprite URL table: `docs/plan/reference-pokeapi.md`.

Stages, each committed in its own transaction and recorded in
`PokeCacheMeta`: `SPECIES, TYPES, MOVES, NATURES, ABILITIES, ITEMS,
GENERATIONS`. An interrupted sync resumes from the first stage that is
missing or stale; nothing is ever written partially.

---

## 5. Backup and restore

**v1 (in scope):** a local, SAF-based export/import of everything the user
owns — hacks, entries, slots, templates, plus the box art / logo / screenshot
files — as a single `.zip` containing `data.json` and `images/`. Import is a
full replace inside one transaction, ids and timestamps preserved. The PokéAPI
cache is never part of a backup: it is re-downloadable data, not user data.

**v2 (out of scope, spec to be written separately):** Google Drive backup.
v1 ships the Settings row for it, disabled, with a "Coming soon" badge. The
work in v1 that makes v2 cheap is the seam: `BackupRepository` produces and
consumes a `BackupPayload` with no knowledge of where the bytes go, so Drive
becomes a second transport rather than a rewrite.

---

## 6. Files owned by the app

Images the user "owns" — hack box art, hack logo, entry screenshot — are
copied into internal storage (`filesDir/images/<uuid>.jpg`) at pick/download
time and downsampled to a sane size, exactly as `ImageStorage` does in
ThePatientGamerHelper. Remote sprites are never copied; they are Coil-cached.
Picking from the gallery uses `ActivityResultContracts.PickVisualMedia`, which
requires no runtime storage permission.

---

## 7. Build, signing, CI

Mirrors ThePatientGamerHelper:

- A dedicated release keystore generated once, stored as GitHub secrets, so
  every signed build keeps the same SHA-1.
- `Android CI` on every push/PR: lint, unit tests, debug APK.
- `Build APK`, manual, signed release APK.
- `Release`, manual, cuts `CHANGELOG.md`'s `[Unreleased]` section, bumps
  `versionName`/`versionCode`, builds, publishes, and only then pushes the
  bump.

---

## 8. Out of scope for v1

Google Drive backup (v2). Multi-user or account concepts. Custom species
(A11). Any legality validation (A9). Battle/coverage analysis — that is
CoverDex's job, not this app's. A secret trainer ID field (rejected, not
just deferred). Sharing/export of a single Hall of Fame as an image or PDF
(a "trainer card" — rejected for now; the user has other, unspecified
features in mind for later).

## 9. Open questions

None currently blocking. Both items raised during planning (a secret ID
field, a trainer-card export) were resolved — see A12 above and §8.
