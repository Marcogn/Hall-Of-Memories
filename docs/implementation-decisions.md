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

TheGamesDB catalogues commercial releases; a hack not listed there returns no
results by its own name. This is a *normal* case for this app, so the UI
offers a gallery pick as an equal alternative and renders a deterministic
name-derived placeholder when there is neither. An empty result set is
phrased as information, not failure.

> **Correction, from real on-device use (post-Phase-6):** the original
> assumption above — "a search for a ROM hack's own name returns nothing" —
> was wrong. TheGamesDB does catalogue many well-known ROM hacks directly
> (confirmed by the user searching one up and finding it). The search now
> defaults to the hack's own name first (not a separate "base game title"
> field, which was removed from the form as redundant) and only needs a
> gallery pick/placeholder fallback for hacks TheGamesDB genuinely doesn't
> have — a narrower case than originally designed for, but the same fallback
> chain still covers it correctly.

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

## A new BackupDao, not per-repository `replaceAll()` methods *(Phase 5)*

Restore needs one atomic transaction across hacks, entries, slots and
templates — four tables owned by three separate repositories
(`HackRepository`/`HallOfFameRepository`/`PokemonTemplateRepository`), none
of which has any reason to know about the other two outside of a restore.
Rather than adding a `replaceAll()` method to each repository interface
(polluting their normal CRUD-shaped surface with a backup-only operation,
and still needing something above them to sequence three separate
transactions into one), `data/local/dao/BackupDao.kt` is a new DAO with
exactly one job: `@Transaction suspend fun replaceAll(hacks, entries,
slots, templates)`, the same default-method-in-DAO-interface pattern
`HallOfFameDao.saveEntryWithSlots` already established in Phase 1.
`BackupRepositoryImpl` is the only caller. Deleting every hack cascades to
its entries and slots for free (`ON DELETE CASCADE`); templates are
cleared with their own `DELETE`, matching `PokemonSlotEntity.sourceTemplateId`
carrying no foreign key to them.

## `BackupPayload` embeds entries and slots inside their hack, not as flat lists *(Phase 5)*

The DTO shape is `BackupPayload.hacks: List<BackupHackDto>`, each carrying
`entries: List<BackupEntryDto>`, each carrying `slots: List<BackupSlotDto>`
— nesting that mirrors the actual foreign-key hierarchy (`Hack` → `HallOfFameEntry`
→ `PokemonSlot`) instead of three flat top-level lists correlated by id.
This makes `BackupArchiveBuilder`'s "which images does this payload
reference" pass a simple recursive walk with no join logic, and makes a
malformed archive (an entry whose `hackId` doesn't exist, say) structurally
impossible to represent rather than something import has to validate for.

## Backup import counts skipped images instead of failing the row *(Phase 5)*

Spec §5: "a missing image inside an otherwise valid archive imports the row
with a null path and reports how many images were skipped." A hand-edited
or partially-transferred backup file with `data.json` intact but a missing
`images/` entry is still a fully useful backup — the alternative (failing
the whole import over one lost screenshot) would throw away far more data
than the missing image itself represents. `BackupRepositoryImpl.importPayload()`'s
private `resolveImage()` is the one place this policy lives: a `null` from
the archive's `images` map increments `imagesSkipped` and resolves to a
`null` path, never an exception.

## Post-launch bug-fix round, from real on-device use

Seven issues reported after Phases 0–6 shipped and were used for real. Not a
phase — v1's roadmap was already complete — but worth recording the same way.

### `HackDao.upsert()` used `INSERT OR REPLACE`, silently wiping a hack's Hall of Fame on every edit

The most serious of the seven: editing an existing hack (name, generation,
artwork, notes — any field) deleted every Hall of Fame entry underneath it,
slots included, with no error and no warning. Root cause: Room's
`OnConflictStrategy.REPLACE` compiles to SQLite `INSERT OR REPLACE`, which is
not an update — on a primary-key conflict SQLite deletes the existing row
first, then inserts the new one. `hall_of_fame_entries.hackId` has
`ON DELETE CASCADE` onto `hacks.id` (and `pokemon_slots.entryId` cascades the
same way off `hall_of_fame_entries.id`), so the delete-then-insert silently
cascaded away every entry and slot on every single hack edit — the hack row
itself survived (same id, new field values), which is exactly why it looked
like an ordinary, working edit.

Fixed by giving `HackDao` real `@Insert`/`@Update` methods and a `@Transaction`
default-method `upsert()` that checks `exists(id)` first and dispatches to
whichever one actually applies — an `UPDATE` never deletes the row, so no
cascade fires. `HallOfFameDao.saveEntryWithSlots()` uses the same `REPLACE`
pattern on `upsertEntry()` but was never at risk: it always deletes and fully
reinserts all six slots in the same transaction right after, so the
cascade-delete it also triggers is invisible — it's an entry editing its own
children, not a hack's edit wiping a *different* table's rows out from under
it with nothing to rebuild them afterward.

### Screen transitions felt slow, and a fast double-tap could land on a mid-transition screen

Ported the exact fix already shipped in ThePatientGamerHelper (found via its
git history, not still described in its own `CLAUDE.md`): `NavHost` gained
explicit 300 ms slide+fade `enterTransition`/`exitTransition`/
`popEnterTransition`/`popExitTransition` (replacing Navigation Compose's
default ~700 ms crossfade, which keeps the outgoing screen composed and
clickable while it fades out), and every `navigate()`/`popBackStack()` call
site in the graph is now guarded by a `NavBackStackEntry.lifecycleIsResumed()`
check on the specific entry that owns the callback — a `NavBackStackEntry`
only reaches `RESUMED` once its own transition has fully settled, so a tap
landing mid-animation is silently ignored instead of firing a second,
unintended navigation. No new dependency, same as the sibling.

### Hack detail: FAB, logo-only header, always a tile preview

Three related UI changes, from how the screen was actually used rather than
how it was originally speced: (1) a `FloatingActionButton` now exists on
`HackDetailScreen` unconditionally — before this, `onAddEntry` was wired only
to the empty-state's text link, so a hack that already had at least one entry
had no way to add another from its detail screen at all. (2) The header now
passes `boxArtPath = null` to the shared `HackArtwork` composable, showing the
logo alone (or the generated placeholder) — box art stays the identity shown
in the library's list/grid (`HackArtwork` itself is unchanged, still used
there with box art), but repeating a large box art image here too was
judged unnecessary once the FAB and carousel already carry the screen. (3)
The `entries.size == 1` branch that rendered the full six-slot
`HallOfFameContent` inline was removed — a hack with exactly one entry now
renders through the same `HallOfFameCarousel` tile as 2–6 entries, so the
"tap a tile to open the entry" interaction is consistent regardless of count,
rather than one entry behaving like a detail page and two+ behaving like a
gallery.

### One TheGamesDB search instead of two, and it defaults to the hack's own name

Two originally-separate corrections that turned out to share one root cause.
`GameArtSearchCoordinator.downloadArt()` already fetched both box art and
logo from a single `Games/Images` call for whichever result the user picked
— the "two searches" impression was purely a UI artifact of `HackFormScreen`
rendering two identical "Search online" buttons, one per artwork slot, both
wired to the same dialog. There's now exactly one "Search online" action,
above both artwork previews.

Separately: the original assumption that "TheGamesDB has no ROM hacks, so
search the *base game* instead" (see the "Box art and logo are optional"
entry above, and its correction) meant the query defaulted to a manually
typed "base game title" field rather than the hack's own name. That field is
now gone from the form entirely — the search defaults to `draft.name` — since
TheGamesDB does catalogue many hacks directly and a hack's own name is the
obviously-correct first guess. The underlying `baseGameTitle` column stays on
`Hack`/`HackEntity` unused rather than being dropped (no schema migration for
a single now-unedited nullable field) — `HackFormViewModel.onSearchResultSelected()`
still writes it from a selected search result as harmless provenance, it's
just no longer surfaced or user-editable.

### Duplicate hacks are not detected or merged — deliberately left open

Adding "the same hack" twice creates two independent rows: `HackDao` has no
uniqueness constraint on `name` (unlike ThePatientGamerHelper's Platform/
Genre/Tag lookup tables, which are shared many-to-many reference data with a
`UNIQUE` index on a normalized name — a structurally different case from
`Hack`, which is itself primary user content, the same category as
`ReviewEntity`/`BacklogItemEntity`, neither of which dedupes by title in that
app either). Fixing this for real needs a name-collision check *and* a
merge/confirm UX ("a hack named X already exists — open it instead?") that
doesn't exist as a pattern anywhere in either app yet, and two hacks
legitimately sharing a display name (a hack and an unrelated remake, say)
would need the check to be advisory rather than a hard block. Left open,
not attempted in this round — flagged here so it isn't mistaken for an
oversight.

## Home library: tap targets and multi-select

Three related changes to `ui/home/HomeScreen.kt`, requested directly after
using the library for real. Not a phase — outside the v1 roadmap entirely.

### Grid and list tiles overlaid the logo on top of the box art

`HackGridTile` drew the logo in a `Box` centered over the box art whenever a
hack had both, and `HackListRow` called the shared `HackArtwork` composable
— which does the same thing by design, since it also serves `HackDetailScreen`
(logo-only header, `boxArtPath = null`, so the overlay branch never triggers
there) — with both paths populated. This section of `docs/spec.md` already
described Home as box-art-only; the code just didn't match it. Fixed by
having both tiles render box art alone, and by only reaching the logo-alone
path (or the placeholder) when a hack has no box art at all.
`HackArtwork` itself is unchanged — still shared with `HackDetailScreen`,
which passes `boxArtPath = null` and still wants its own logo-or-placeholder
fallback.

### Only the box-art thumbnail was clickable in list view

`HackListRow` put `onClick` on the artwork's `Surface` only; the title and
the rest of the row had no click handler at all. Replaced the `Surface` with
a plain `Box` (rounded via `Modifier.clip`, since `Surface` no longer
supplies it) and moved the click handling to the whole `Row`.

### Multi-select: long-press enters selection mode

Requested as a new capability: long-pressing a hack (grid or list) now
selects it and swaps Home's top bar for a contextual one (close, edit —
enabled only for exactly one selection, delete with a confirmation naming
the total Hall of Fame entries at stake, select-all). Tapping another hack
while active toggles it instead of navigating; the system back gesture exits
selection instead of leaving the screen (`BackHandler`, same pattern as
`HofFormScreen`'s unsaved-changes guard).

State lives in `HomeViewModel` as a plain `MutableStateFlow<Set<String>>` of
selected hack ids, combined into `HomeUiState` alongside the existing
search/filter/view-mode state — `HomeUiState.isSelectionMode` is a computed
property (`selectedHackIds.isNotEmpty()`), not a separate flag to keep in
sync. `HomeViewModel.combine()` already used the 5-flow overload for its
other state, so the selection flow is combined in a second step (`Flow<T>.
combine(otherFlow) { ... }`) rather than switching every branch to the
vararg/array-based overload for one more input.

`Modifier.combinedClickable` (needed for the long-press) is
`@ExperimentalFoundationApi`, same class of gotcha already noted in
`CLAUDE.md` for `LazyVerticalStaggeredGrid` — the missing `@OptIn` is always
a hard compile error in Kotlin, but this sandbox has no Android SDK to
compile against locally (see `CLAUDE.md`, "Build/test commands"), so it
first surfaced as an `Android CI` failure on the pushed PR. Confirmed by
reading that failure's actual compiler output before fixing it, rather than
guessing.
