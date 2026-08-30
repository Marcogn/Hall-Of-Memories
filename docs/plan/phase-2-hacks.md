# Phase 2 — Hacks: CRUD, TheGamesDB, box art and logo

**Goal:** the user can create, edit and delete a hack; give it a generation;
and give it box art and a logo either from a TheGamesDB search or from the
gallery. Home becomes a real hack library.

**Depends on:** Phase 1 (schema, repositories, `ImageStorage` does not exist
yet — it is created here).

---

## 1. Image storage

```
data/image/ImageStorage.kt      @Singleton, @Inject constructor(@ApplicationContext)
```

Port ThePatientGamerHelper's file of the same name, generalised from "covers"
to a `subdir` parameter (`images/`). Surface:

```kotlin
suspend fun persist(sourceUri: Uri): String            // gallery pick -> absolute path
suspend fun persistBytes(bytes: ByteArray): String     // downloaded image -> absolute path
suspend fun duplicate(path: String): String            // copy, new uuid (used in Phase 4/5)
fun delete(path: String?)                              // best-effort
```

Every write downsamples to a max edge of 900 px and re-encodes as JPEG at
quality 85 before saving as `filesDir/images/<uuid>.jpg`. This is not
premature: ThePatientGamerHelper shipped without it and full-size TheGamesDB
box art bloated both the device footprint and every backup, needing a
retroactive re-compression pass. Do it from the first write here.

Exception: **logos are stored as PNG**, unmodified except for downsampling,
because a JPEG re-encode destroys the transparency a clear-logo needs. Add a
`persistBytes(bytes, format: ImageFormat)` parameter rather than a second
method.

Picking uses `ActivityResultContracts.PickVisualMedia` (no runtime storage
permission).

---

## 2. TheGamesDB client

```
data/thegamesdb/TheGamesDbApiClient.kt      HttpURLConnection, kotlinx.serialization
data/thegamesdb/TheGamesDbPreferences.kt    SharedPreferences, runtime-entered API key
data/thegamesdb/GameArtSearchCoordinator.kt logic shared by the hack form
domain/model/GameArtSearchResult.kt
```

Port ThePatientGamerHelper's `TheGamesDbApiClient` closely — it is a
debugged, working client — with these changes:

- Drop the genre and developer lookup caches. This app needs neither.
- Keep the platform lookup cache (id → name) for labelling results.
- **Keep the desktop `User-Agent` verbatim.** TheGamesDB's anti-bot filtering
  answers an app-like UA with a misleading "invalid API key" error even when
  the key is valid. This cost real debugging time; do not "clean it up".
- Keep the `apikey` query param on every request — TheGamesDB has had no
  anonymous access since their 2026-02-17 policy change.
- Keep the indexed-array filter syntax (`filter%5Bplatform%5D%5B0%5D=`).

Endpoints:

1. `GET /v1/Games/ByGameName?apikey=&name=&fields=overview,release_date&include=boxart`
   — the picker list: id, title, platform name, release year, boxart thumb.
   Use the `include.boxart.base_url.thumb` URL for rows, `original` for the
   saved image (the same "don't download megabytes for a 48 dp row" reasoning
   as in the sibling app).
2. `GET /v1/Games/Images?apikey=&games_id={id}` — called **only for the game
   the user picks**, to get the logo. Response shape:
   `data.base_url.{original,thumb,...}` and `data.images.{gameId}: [{type, side, filename}]`.
   Logo selection order: `type == "clearlogo"` → `type == "banner"` →
   `type == "fanart"` → none. Box art: `type == "boxart" && side == "front"`
   → any `boxart` → the search result's boxart.
   **This endpoint's exact response shape could not be verified during
   planning** (it needs a key). Parse it defensively with
   `JsonObject` navigation and `?:` fallbacks exactly like the sibling
   client's boxart handling, log the raw body at `Log.d` on a parse miss, and
   degrade to "no logo found" — never crash, never block the save.

`GameArtSearchCoordinator.search(title)` returns
`Outcome.Results(List<GameArtSearchResult>)` or `Outcome.Message(String)`,
and never throws: a missing API key, no results, an HTTP error and a network
failure are all messages shown inside the dialog. Append the underlying
exception's message to the displayed text (`Log.w` as well) — a fixed generic
string is exactly what made the sibling app's search failures undiagnosable.

### The reality of ROM hacks (read this before designing the UX)

TheGamesDB catalogues commercial releases. **"Pokémon Radical Red" is not in
it; "Pokémon FireRed Version" is.** The search is therefore framed in the UI
as *"Find the artwork of the base game"*, with the hack's own name pre-filled
but freely editable, and the empty-result state saying so in words rather than
reading as a failure. Manual gallery pick and "no image at all" are
first-class outcomes, not fallbacks.

---

## 3. Hack form

```
ui/hack/HackFormScreen.kt, HackFormViewModel.kt, HackFormUiState.kt
ui/common/GameArtSearchDialog.kt
ui/common/GenerationPicker.kt
ui/common/HackArtwork.kt          box art / logo / placeholder rendering
```

Fields: name (required, non-blank), generation (dropdown of
`GameGeneration.entries` with localized labels), base game title (optional),
notes (optional), box art, logo.

Artwork block, three actions per image: **Search online** (opens
`GameArtSearchDialog`, pre-filled with base game title or hack name),
**Choose from gallery**, **Remove**. Picking a search result downloads both
images through `TheGamesDbApiClient` → `ImageStorage.persistBytes`, stores
the local paths plus the remote URLs and `theGamesDbId`, and closes the
dialog. Replacing an image deletes the previous file.

`HackFormUiState` holds the draft, `isSaving`, `errorMessage`,
`searchState`. The ViewModel injects `@ApplicationContext Context` for
`getString()` (ViewModels cannot call `stringResource`).

Editing (`HackForm(hackId)`) loads the existing hack; the same screen serves
both. Save writes through `HackRepository.upsert`, setting `updatedAt`.

`HackArtwork` renders, in order: logo (if present) over box art, else box art,
else a generated placeholder — a Material container tinted by
`hack.name.hashCode()` with the hack's initials, deterministic so the same
hack always looks the same.

---

## 4. Home screen

```
ui/home/HomeScreen.kt, HomeViewModel.kt, HomeUiState.kt
ui/common/ViewModeToggle.kt        list / grid, shared with later screens
data/settings/ViewModePreferences.kt
```

- Grid of hacks (`LazyVerticalStaggeredGrid`,
  `@OptIn(ExperimentalFoundationApi::class)`, `StaggeredGridCells.Adaptive`),
  with a list toggle persisted in `ViewModePreferences` (SharedPreferences,
  same as the sibling app).
- Each tile: box art or placeholder, hack name, generation chip, and the
  Hall of Fame count ("3 Halls of Fame").
- Search field filtering by hack name, plus a generation filter chip row.
  The filtering itself is a **pure function in `domain/filter/HackFilters.kt`**
  (`filterHacks(hacks, query, generations): List<Hack>`), unit-tested, with
  the ViewModel `combine()`-ing the repository `Flow` with the UI state — the
  same UDF shape as everywhere else.
- FAB `+` → `HackForm(null)`.
- Empty state: an illustration-free card explaining what a hack is here and a
  primary "Add your first hack" button.
- The pokédex sync banner from Phase 1 sits above the grid.

---

## 5. Hack detail screen

```
ui/hack/HackDetailScreen.kt, HackDetailViewModel.kt, HackDetailUiState.kt
```

Header: logo over box art (`HackArtwork`, large), hack name, generation,
notes. Overflow menu: Edit, Delete.

Body depends on entry count, per spec §3.1:

- **0** → empty state + "Add the first Hall of Fame" button.
- **1** → the screen **renders the entry detail inline** (Phase 3 supplies the
  `HallOfFameContent` composable; in this phase, leave a `TODO` placeholder
  card and wire it in Phase 3). Do not navigate automatically — an automatic
  redirect makes the back button behave strangely and hides the hack's own
  header, which the spec explicitly wants as the header in this case.
- **N > 1** → a list of entry previews: thumbnail (screenshot, else slot-0
  sprite — Phase 3 supplies both; render a placeholder for now), player name,
  playtime, insertion date.

Delete: `AlertDialog` naming the number of Halls of Fame that will be deleted
with it, and deleting the hack's own image files after the row is gone.

---

## 6. Settings addition

TheGamesDB API key: a text field with a "how to get one" explanation and a
save button, stored via `TheGamesDbPreferences`. With no key set, the search
dialog shows an informational message and never calls the API.

---

## 7. Tests

Plain JVM:

- `HackFiltersTest` — name query is case- and accent-insensitive; generation
  filter; both combined; empty query returns everything in insertion order.
- `TheGamesDbResponseParsingTest` — parse a trimmed `ByGameName` fixture
  (including a game with `genres: null`, the explicit-null case that broke the
  sibling app in production) and a trimmed `Games/Images` fixture, plus the
  "no clearlogo present" fallback path.
- `HackMappersTest` — round-trip including all four nullable image fields.

Robolectric:

- `ImageStorageTest` — persist, downsample, duplicate, delete; a PNG stays a
  PNG.
- `HackRepositoryImplTest` — upsert then observe; delete cascades to entries.

Manual (`docs/test-plan.md`): a real TheGamesDB search with a real key, a
gallery pick, a hack with no artwork at all, and a hack whose name matches
nothing (the ROM-hack case).

---

## 8. Definition of done

- [ ] Create / edit / delete a hack, with and without artwork.
- [ ] Search online finds a commercial game and fills both images.
- [ ] With no API key, the dialog explains rather than failing.
- [ ] Gallery pick works with no permission prompt.
- [ ] Home grid/list toggle persists across restarts.
- [ ] Deleting a hack removes its image files as well as its rows.

## 9. Pitfalls

- Do not JPEG-re-encode a clear logo; transparency is the point of it.
- `filter[platform][0]` must stay percent-encoded as `filter%5B...%5D`.
- The `include=boxart` block is a sibling of `data`, not inside it; both the
  base URLs and the per-game image arrays live under `include.boxart`.
- Delete the replaced image file when the user changes artwork, or internal
  storage grows forever.
