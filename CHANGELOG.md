# Changelog

All notable changes to Hall of Memories are documented here.

The format follows one top-level bullet per significant, user-facing change,
leading with a short bold summary — the `Release` workflow extracts exactly
those lead-ins into the GitHub Release body. See `CLAUDE.md`, "Changelog and
release process".

## [Unreleased]

- **Planning documents for the phased v1 build.** The functional
  specification, a seven-phase implementation plan, a measured PokéAPI and
  sprite reference, and the agent guide that governs how the code is written.
- **Project foundation (Phase 0).** The app now builds and installs: Gradle
  project setup with the pinned dependency catalogue, Hilt, Material 3 theme
  with light/dark/system switching, an in-app Italian/English language
  picker, a navigation drawer with the three v1 sections (hack library,
  saved Pokémon, settings), and the `Android CI` workflow.
  - Fixed the launcher's round-icon manifest entry, which pointed at the
    square icon instead of `ic_launcher_round`.
- **Local data model and PokéAPI sync (Phase 1).** The app now owns its full
  Room schema — hacks, Hall of Fame entries, the six-slot teams inside them,
  reusable Pokémon templates, and the PokéAPI reference cache — and
  downloads that reference data (species, types, moves, natures, abilities,
  items, generations) once on first launch, ~57 requests and ~2.5 MB rather
  than the ~650 MB a naive per-species fetch would take.
  - The pokédex sync runs in the background without blocking the rest of the
    app, resumes a stage that was interrupted, and never touches saved
    hacks, Halls of Fame or templates when invalidated and re-run.
  - Settings gained a Pokédex data section (per-stage last-sync time and
    item count, "Invalidate and re-download") and a "Always use the latest
    sprites" toggle.
  - Sprite URLs are derived from a hack's generation, never stored, with a
    verified fallback chain for species or shiny forms a given generation
    has no sprite for.
- **Hack library and TheGamesDB artwork (Phase 2).** Home is now a real hack
  library: create, edit and delete hacks (name, generation, base game title,
  notes), search and generation filters, and a list/grid view toggle.
  - Box art and logo can be searched on TheGamesDB (a game title, matched to
    the hack's generation) or picked from the gallery — both optional, with
    a deterministic generated placeholder when neither is set.
  - Settings gained a TheGamesDB section to enter the API key the search
    requires; without one, "Search online" shows an explanatory message
    instead of failing.
  - Each hack's detail screen shows its artwork and its Hall of Fame entries
    (still a placeholder pending Phase 3) with an entry count.
- **Hall of Fame entries and the six-slot team editor (Phase 3).** The heart
  of the app: create a Hall of Fame under a hack, fill six Pokémon slots
  with full detail, view it, and edit it forever.
  - Each slot edits species (searched against the local pokédex cache, with
    a "not downloaded yet" card and a "Download now" action when the cache
    is empty), nickname, gender, level, shiny, nature, ability, held item,
    up to four moves, and all twelve IV/EV stats. Nature/ability/held
    item/moves are editable combo boxes — a suggestion dropdown over free
    text, never a restriction.
  - Range validation only, never legality: an out-of-range level/IV/EV
    value is simply not committed, and a total EV over 510 is the one rule
    that blocks confirming a slot.
  - Trainer name (the only required field), player ID, playtime, an
    optional screenshot from gallery or camera (tap to view full-screen
    with pinch-to-zoom), and an editable insertion date/time.
  - Leaving the form with unsaved changes asks for confirmation, from both
    the top bar and the system back gesture.
  - The entry detail view shows the screenshot, trainer info, and the full
    team with sprites, nature stat arrows, type-coloured move chips and
    IV/EV rows; a hack with exactly one entry now shows it inline instead
    of a placeholder, and a hack with several entries gets real
    screenshot/sprite thumbnails and a newest/oldest sort toggle.
- **Reusable Pokémon templates (Phase 4).** A Pokémon configured once can be
  reused in any Hall of Fame, in any hack.
  - The slot editor gained "Save as template" (a label dialog that offers
    Overwrite or Save as a copy when the name collides with an existing
    template) and "Load from template" (a bottom sheet that replaces the
    whole slot, with an Undo snackbar).
  - The Templates screen (already a drawer destination) now lists every
    saved template with its sprite, label and species/level, a search field
    over both label and species, and per-row Edit/Duplicate/Delete — reusing
    the same slot editor in a template mode rather than a second one.
  - Deleting a template leaves every Hall of Fame slot created from it
    completely unaffected — it carries no foreign key, only provenance.
- **Presentation polish and local backup (Phase 5).** A hack with 2–6 Halls
  of Fame now shows them as a horizontally scrollable carousel instead of a
  plain list (a hack with more still gets the list); the screenshot viewer
  gained double-tap-to-zoom alongside its existing pinch-zoom; Home's grid
  tiles keep a cover's real aspect ratio instead of a forced crop, with a
  crossfade on every sprite and cover load.
  - Settings gained a local Backup section: export everything (every hack
    and its artwork, every Hall of Fame entry with its screenshot and six
    slots, every saved template) into a single file via the system's file
    picker, and import it back on this or any other device. Restoring asks
    for explicit confirmation first — it fully replaces whatever's
    currently on the device — and a malformed or newer-than-supported file
    is rejected with nothing written, never a partial or silent failure.
  - A disabled "Google Drive backup" row with a "Coming soon" badge marks
    the v2 seam: the backup logic already knows nothing about where the
    bytes go, so Drive support later is a new transport, not a rewrite.
  - The PokéAPI cache is never part of a backup — it's re-downloadable, not
    user data.
