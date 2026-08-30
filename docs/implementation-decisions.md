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
