# Implementation decisions

Non-obvious choices and the reasoning behind them, so they are not
re-litigated in a later session. Entries added during planning are marked
*(planning)*; add one whenever a phase forces a real choice.

## PokéAPI: no per-Pokémon detail records *(planning)*

CoverDex fetches `pokemon/{id}` for all ~1300 Pokémon on first load. Measured
against the live mirror, one such record is **532 KB** — the whole set is
roughly **650 MB over 1300 requests**. Tolerable in a desktop browser, not on
a phone on first launch.

Hall of Memories needs only names, types, and sprite URLs. So:

- Names come from the seven index files (~370 KB total).
- Types come from the 18 `type/{id}` records (~500 KB), which carry both a
  `pokemon[]` list with slot numbers and a `moves[]` list — the complete
  species→type and move→type mappings.
- Sprites are **derived URLs**, not payload data (below).

Total: ~57 requests, ~2.5 MB. Measured figures and response shapes are in
`docs/plan/reference-pokeapi.md`.

If a future feature genuinely needs a detail record, fetch it lazily for the
one species being shown — never as a sync stage.

## Sprites are derived, not stored *(planning)*

`https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/…`
follows a fixed convention: `<variant path>/[shiny/]<id>.png`. Every variant
used by the app was verified with a real request (table in
`docs/plan/reference-pokeapi.md` §4), including the negative cases that shape
the fallback chain:

- `versions/generation-i/red-blue/shiny/6.png` → 404 (shinies did not exist
  in Gen I), so `RED_BLUE.supportsShiny = false`.
- `versions/generation-ix/…` does not exist, so SV maps to `other/home`.
- `versions/generation-iii/emerald/906.png` → 404: a Gen-9 species has no
  Gen-3 sprite. `PokeSpecies.generationIntroduced` lets the resolver skip that
  candidate before making the request; the `onError` chain covers the rest.

Storing sprite URLs in the database would mean a schema migration every time
the resolution rules change, and a cache wipe could not fix a bad URL. A pure
function can be unit-tested; a stored column cannot.

## No Retrofit or Ktor *(planning)*

Spec assumption A3 proposed one. Overridden: the app talks to two hosts with
about six endpoint shapes between them. Android's `HttpURLConnection` is
OkHttp-backed (connection pooling, transparent gzip), and the sibling app has
three hand-rolled clients in production on exactly this pattern. Adding an
HTTP stack, its converters and its coroutine adapters buys nothing here and
breaks the projects' dependency-minimalism, which has held across eight
phases.

## The PokéAPI cache is a typing aid, never a dependency *(planning)*

`PokemonSlot` stores `speciesName`, `nature`, `ability`, `heldItem` and the
four moves as plain strings alongside `speciesId`. The alternative —
foreign keys into the cache tables — would mean "invalidate and re-download"
could blank a saved Hall of Fame, and an offline device with an empty cache
could not render one. A Hall of Fame is a permanent record; the pokédex is
disposable. `speciesId` is kept purely to derive a sprite.

## Six slot rows always exist *(planning)*

Including empty ones (`speciesId = null`). Keeps `(entryId, slotIndex)` stable
across edits, makes saving a delete-then-insert of exactly six rows, and turns
a future "reorder the team" feature into a position swap rather than an
insert/delete dance.

## `sourceTemplateId` has no foreign key *(planning)*

It is provenance metadata. A foreign key would either cascade a template
deletion into saved Halls of Fame or block the deletion outright — both wrong.

## Box art and logo are optional and user-overridable *(planning)*

TheGamesDB catalogues commercial releases. A search for a ROM hack's own name
("Radical Red") returns nothing; the base game ("Pokémon FireRed Version")
does. This is the *normal* case for this app, so the UI frames the search as
"find the base game's artwork", offers a gallery pick as an equal alternative,
and renders a deterministic name-derived placeholder when there is neither.
An empty result set is phrased as information, not failure.

Logos are stored as PNG rather than the JPEG used for other images: a JPEG
re-encode destroys the transparency that makes a clear-logo usable over a
header.

## Images are downsampled at write time, from the first write *(planning)*

The sibling app shipped without this and had to add a retroactive
re-compression pass over every existing file after full-size TheGamesDB box
art bloated both the device footprint and every backup. Same code, same
limits (max edge 900 px, JPEG quality 85), applied from the first write here.

## The pokédex sync does not block the UI *(planning)*

CoverDex holds its whole UI behind a loading screen during the first fetch; it
downloads two orders of magnitude more data. Here the sync is ~2.5 MB and
almost everything — creating hacks, editing artwork, browsing — works without
it. So it runs on an application-scoped coroutine with a non-blocking progress
banner, and only the slot editor's species search shows an explicit
"not downloaded yet" state.

WorkManager was considered and rejected: it would be a new dependency for a
one-shot foreground task that an `@ApplicationScope` coroutine already
survives navigation and configuration changes for.

## The sync is stage-resumable, and never writes partially *(planning)*

Each stage commits in a single Room transaction and then records its own
`PokeCacheMeta` row. A killed first launch resumes at the first missing or
stale stage. A cache that reports itself synced while half-written is worse
than no cache — the same invariant CoverDex enforces with its
"complete and versioned, or fully absent" rule.

## Backups exclude the pokédex cache *(planning)*

It is re-downloadable, it would dominate the archive's size, and restoring a
stale copy onto a device is worse than re-fetching a fresh one.

## Google Drive is a transport, deferred *(planning)*

`BackupRepository` produces and consumes a `BackupPayload` with no knowledge
of where the bytes go; `LocalBackupManager` owns SAF. v2's `DriveBackupManager`
is then a sibling class, not a change to the repository or the UI. v1 ships
the disabled Settings row so the seam is visible and honest.

## The adaptive launcher icon keeps its `-v26` qualifier *(Phase 1)*

Android Lint suggests merging `mipmap-anydpi-v26/` into a plain
`mipmap-anydpi/` once `minSdk` reaches 26, since the version qualifier reads
as redundant. Tried it: AAPT2 fails resource linking entirely
(`resource mipmap/ic_launcher not found`) — an `<adaptive-icon>` XML resource
apparently still needs the explicit `-v26` folder qualifier to be recognized,
regardless of `minSdk`. Reverted; the lint suggestion is safe to leave
un-actioned. Re-verify before trying it again in Phase 6 when the real
launcher icon is designed.

## PokedexSyncManager uses `Mutex.tryLock`, not `withLock` *(Phase 1)*

`startIfNeeded()`/`forceResync()` are meant to be true no-ops when a sync is
already running (spec-adjacent requirement from the phase plan: "a second
call while running is a no-op that returns the same state"). `tryLock()` is
atomic and either acquires the lock or returns `false` immediately; the
alternative (`if (mutex.isLocked) return` followed by `withLock`) has a
race window where two near-simultaneous calls can both observe `isLocked ==
false` and both proceed, and would spend a stage's worth of no-op work
re-checking `isStageFresh()` rather than genuinely skipping.

## Artwork deletion is deferred to a successful save *(Phase 2)*

`HackFormViewModel` never calls `imageStorage.delete()` the moment a user
picks a replacement box art or logo — only the in-memory draft changes. The
original path is deleted only inside `save()`, after `hackRepository.upsert()`
has committed, and only if the final saved path actually differs from the
one the hack had on open. Deleting eagerly at pick-time would leave the
Room row pointing at a file that no longer exists the moment the user
discards the edit (system back, or navigating away) instead of saving —
Room's row is the source of truth, so the file must outlive it until the
row itself changes.

## Hack detail uses direct icon buttons, not an overflow menu *(Phase 2)*

Matches `ReviewDetailScreen`'s top bar in ThePatientGamerHelper: edit and
delete are the only two actions a hack detail screen ever has, both always
relevant, so a three-dot overflow menu would only add a tap with no benefit.
An overflow menu earns its place once a screen has three or more
situational actions.

## TheGamesDB platform hint is a static generation→platform map *(Phase 2)*

TheGamesDB has no concept of a ROM hack's generation, only a commercial
platform. `HackFormViewModel`'s private `GameGeneration.theGamesDbPlatformHint()`
maps each generation to the console its official games actually shipped on
(RB → "Game Boy", GSC → "Game Boy Color", RSE/FRLG → "Game Boy Advance",
DPPT/HGSS/BW → "Nintendo DS", XY/ORAS/SM/USUM → "Nintendo 3DS", SWSH/SV →
"Nintendo Switch", OTHER → no hint) and passes it to `bestPlatformMatch()` as
a tiebreaker, never a hard filter — a search still returns every matching
title across platforms, the hint only reorders which one is likeliest to be
right when several platforms share a title.

## Per-field slot validation is a commit filter, not a blocking error *(Phase 3)*

Spec §3.4 draws a real distinction: an out-of-range level/IV/EV "shows an
inline error and is not committed to the draft", while only the EV total
over 510 "blocks saving that slot". `SlotEditorDialog` implements this
literally — every numeric field is edited as a raw string
(`SlotEditorDialog.kt`'s private `SlotFormFields`), and `toSlotDraft()`
parses each one through `String.toValidIntOrNull(range)`, silently nulling
out whatever doesn't parse or falls outside range. The Confirm button itself
is only ever disabled by `SlotValidation.isEvTotalValid(evTotal)` — never by
an individual field being out of range. This means a slot can be confirmed
with, say, an invalid level typed and left uncorrected — it just saves as
"level not set" rather than the invalid number, which matches the spec's
wording exactly rather than the more defensive "block until everything is
valid" pattern used elsewhere in strict forms.

## Move type chips do a live cache lookup, denormalized data stays denormalized *(Phase 3)*

`PokemonSlot.move1..move4` are denormalized snapshot strings (`domain/model/
PokemonSlot.kt`'s doc comment) — a cache wipe must never blank them. But the
entry detail view wants to colour each move chip by its type (spec §3, "the
four moves as chips with type colours"), and type isn't stored on the slot.
The resolution: `PokedexDao.getMoveBySearchName()` /
`PokedexRepository.getMoveType()` do an exact-name lookup **only** to pick a
chip colour at render time — if the cache is empty or the move isn't found,
`typeColorFor(null)` renders a neutral fallback colour and the move's name
still displays in full. The saved slot itself is never touched by this
lookup; wiping the cache degrades a chip's colour, never the data.

## HallOfFameContent has no `viewMode` parameter *(Phase 3)*

The phase plan's draft signature was `HallOfFameContent(entry, hack,
viewMode)`. Implemented without it: this composable renders one entry's
full detail (screenshot, trainer block, six team cards, notes) and no part
of that view meaningfully differs between a list/grid "view mode" — that
concept applies to browsing *many* entries (Home's hack library, and now
the hack detail screen's own newest/oldest-sorted entry list), not to
viewing one entry's own detail. Read as a plan artifact rather than a real
requirement; nothing in the phase's definition-of-done depends on it.

## Screenshot capture is a `content://` handoff to the system camera app *(Phase 3)*

`ActivityResultContracts.TakePicture` writes into a `FileProvider`-backed
URI (`data/image/CameraCapture.kt`'s `createCameraCaptureUri()`, under
`cacheDir/camera/`), the same shape as a gallery pick's URI —
`ImageStorage.persist()` handles both identically, downsampling and moving
the bytes into `filesDir/images/`. No `CAMERA` runtime permission is
declared: the system camera app that actually handles the intent holds
that permission itself, this app only ever receives the finished photo
through the URI it handed out.

## Template conversions live beside SlotDraft, not in domain/model *(Phase 4)*

The phase plan put `PokemonSlot.toTemplateDraft()`/`PokemonTemplate.toSlotDraft()`
in `domain/model/TemplateConversions.kt`. Implemented in `ui/hof/TemplateConversions.kt`
instead, converting between `PokemonTemplate` and `SlotDraft` — the
in-progress, string-backed editing state `SlotEditorDialog` actually
operates on — rather than a saved `PokemonSlot`. `SlotDraft` was a Phase 3
addition that didn't exist when the plan was drafted; it has no Android
import, so the file stays exactly as pure and unit-testable as the plan
intended (`TemplateConversionsTest` is a plain JVM test), just correctly
placed on the `ui/` side of the boundary it actually crosses — `domain/`
has no reason to know about a screen's in-progress editing state.

## Reusing SlotEditorDialog for templates via a mode parameter *(Phase 4)*

The phase plan allowed either reusing `SlotEditorDialog` with a mode
parameter or writing a second, near-identical editor if reuse "turned out
to entangle the two." It didn't: `SlotEditorMode.SLOT`/`.TEMPLATE` gate
exactly two things — a label field replaces the "Load from
template"/"Save as template" buttons — and every other field (species,
stats, moves, IV/EV validation) is identical code either way. Editing an
existing template directly (from the Templates screen) never goes through
the collision-checking "Save as template" dialog: Confirm just upserts
that template's own id in place, since typing a label that happens to
collide with a *different* template isn't a meaningful conflict here
(`PokemonTemplateEntity.label` has no uniqueness constraint).

## Search normalization extracted to `SearchNormalization.kt` *(Phase 4)*

`HackFilters.kt` (Phase 2) had its own private `String.normalizedForSearch()`
(NFD-normalize, strip combining marks, lowercase). Phase 4's `TemplateFilters.kt`
needed the exact same behavior; Kotlin's top-level `private` is file-scoped,
not package-scoped, so it couldn't be reused as-is. Pulled into an
`internal` function in `domain/filter/SearchNormalization.kt`, shared by
both — the alternative (copy-pasting the same six lines a second time) is
exactly the kind of duplication the project's own conventions call out
to avoid.
