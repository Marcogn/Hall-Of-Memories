# Phase 3 — Hall of Fame entries and the six-slot editor

**Goal:** the heart of the app. Create a Hall of Fame under a hack, fill six
Pokémon slots with full detail, view it, edit it forever.

**Depends on:** Phase 1 (schema, pokédex cache, sprites), Phase 2 (hacks,
`ImageStorage`).

---

## 1. Entry form

```
ui/hof/HofFormScreen.kt, HofFormViewModel.kt, HofFormUiState.kt
ui/hof/HofSlotCard.kt          collapsed slot summary in the six-slot list
ui/hof/SlotEditorDialog.kt     full-screen editor for one slot
ui/common/ScreenshotPicker.kt
ui/common/EditableComboBox.kt  suggestion dropdown + free text
ui/common/StatGrid.kt          the IV / EV six-stat grid
```

`HofForm(hackId, entryId?)` — one screen for create and edit.

Layout, top to bottom:

1. **Team** — six `HofSlotCard`s. A filled card shows the sprite
   (`PokemonSprite`, with the parent hack's generation and the slot's shiny
   flag), nickname or species name, level, and a shiny marker. An empty card
   shows "Slot N — empty" and a plus. Tapping any card opens
   `SlotEditorDialog` for that index.
2. **Trainer** — player name (required, non-blank; the only save-blocking
   field), player ID (free text), playtime (free text with a `H:MM` hint),
   and an "Advanced" expander containing the secret ID.
3. **Screenshot** — optional, gallery or camera, through `ImageStorage`.
   Camera capture uses `ActivityResultContracts.TakePicture` with a
   `FileProvider` URI; add the provider and its `res/xml/file_paths.xml` in
   this phase.
4. **Recorded at** — date and time pickers, defaulting to now, editable
   (spec §3.2.4). Store as `Instant`.
5. **Notes** — multiline free text.

### Draft state and back

The whole draft (entry fields + six slot drafts) lives in
`HofFormUiState` inside the ViewModel; nothing is written to Room until Save.
The slot editor is a **dialog driven by the same ViewModel**, not a separate
navigation route — that avoids passing a mutable draft between destinations
entirely.

Leaving the screen with unsaved changes shows a confirmation. Because the
system back gesture bypasses a screen's own `onBack` lambda, the screen
**must** install an explicit `BackHandler` that routes both the top-bar back
button and the system gesture through the same "discard changes?" check.
This exact trap was hit twice in ThePatientGamerHelper.

### Save

`HallOfFameRepository.save(entry, slots)` in **one transaction**: upsert the
entry, then replace its six slot rows (delete-then-insert keyed by
`(entryId, slotIndex)` is fine and simplest).

Six rows always exist after a save, empty ones included: an empty slot is a
row with `speciesId = null` (the column is nullable from schema v1 — see
`../spec.md` §2), never a missing row. That keeps `(entryId, slotIndex)`
stable across edits and turns a future "reorder the team" feature into a
position swap rather than an insert/delete dance. If Phase 1 shipped the
column non-null by mistake, fix it with a numbered `MIGRATION_1_2` here, not
by recreating the schema.

---

## 2. Slot editor

`SlotEditorDialog(slotIndex, draft, onConfirm, onDismiss)` — a full-screen
`Dialog` with `usePlatformDefaultWidth = false`.

Fields, in order:

| Field | Control | Source |
|---|---|---|
| Species | search field + result list | `PokedexRepository.searchSpecies(query)`, sprite per result |
| Nickname | text | — |
| Gender | segmented button ♂ / ♀ / — | `PokemonGender` |
| Level | number, 1..100 | — |
| Shiny | switch | — |
| Nature | `EditableComboBox` | cached natures, showing "+Atk / −SpA" from `increasedStat`/`decreasedStat` |
| Ability | `EditableComboBox` | cached abilities |
| Held item | `EditableComboBox` | cached items |
| Moves 1–4 | four `EditableComboBox`es | cached moves, each row showing the move's type |
| IVs | `StatGrid`, 0..31 | — |
| EVs | `StatGrid`, 0..252, with a running total | — |
| Template actions | "Load from template" / "Save as template" | Phase 4 — leave the buttons out entirely in this phase |

`EditableComboBox` is the shared control the spec's A9 requires: it suggests
cached PokéAPI values but **accepts any free text**. Suggestions are filtered
by `searchName` prefix-then-contains, capped at 30 rows, and the field never
rejects or rewrites what the user typed.

Species selection stores **both** `speciesId` and `speciesName` — the
denormalized snapshot from spec §2. Never look a saved slot's name up from
the cache at render time.

### Validation (spec §3.4)

- level 1..100, IV 0..31, EV 0..252 — out-of-range values show an inline error
  and are not committed to the draft.
- EV total > 510 shows an error on the EV block and **blocks confirming the
  slot**. It is the only save-blocking rule other than a blank player name.
- Everything else saves regardless. No legality checks, ever.

All of this lives in `domain/validation/SlotValidation.kt` as pure functions
returning a `SlotValidationResult(errors: Map<Field, ErrorKind>)`; the
ViewModel maps `ErrorKind` to a string resource. Unit-tested.

### When the pokédex cache is empty

Every suggestion source is empty and species search returns nothing. Show an
explicit inline card — "Pokédex data hasn't been downloaded yet · Download
now" wired to `PokedexSyncManager.startIfNeeded()` — rather than an
empty-looking search box. Manual text entry still works for every field except
species (which needs an id for its sprite); a species can then only be picked
once the cache exists, and that is acceptable and must be stated in the card.

---

## 3. Entry detail

```
ui/hof/HofDetailScreen.kt, HofDetailViewModel.kt, HofDetailUiState.kt
ui/hof/HallOfFameContent.kt     the reusable body, also embedded by HackDetailScreen
```

`HallOfFameContent(entry, hack, viewMode)` renders:

- The screenshot, if any, as a hero image (tap → full-screen zoomable view).
- The trainer block: name, ID, secret ID (only when set), playtime, recorded-at
  date.
- The team: six cards with sprite, nickname + species, level, gender, shiny
  marker, nature (with its stat arrows), ability, held item, the four moves as
  chips with type colours, and IV/EV rows. Empty slots render as a dimmed
  "empty" card.
- Notes.

`HackDetailScreen`'s single-entry case (Phase 2 §5) now embeds this composable
directly under the hack header, replacing the placeholder.

Top bar: Edit, Delete (confirmation dialog; deleting the entry deletes its
screenshot file).

Type colours: a single `ui/common/TypeColors.kt` map from the 18 type names to
`Color`, plus an `unknown` fallback for a move whose type never got filled in.

---

## 4. Hack detail — multi-entry list

Replace Phase 2's placeholder previews with real ones: thumbnail =
screenshot if present, else `PokemonSprite` of slot 0, else placeholder
(spec §3.1). Sort newest `insertedAt` first, with a sort toggle for oldest
first.

---

## 5. Tests

Plain JVM:

- `SlotValidationTest` — every bound (level 0/1/100/101, IV −1/0/31/32,
  EV 253, total 510 vs 511), and that a legality-nonsense but in-range slot
  validates clean (Magikarp with Hyper Beam and the wrong ability is legal
  input here).
- `PlaytimeParsingTest` — extend Phase 1's with formatting back to text.
- `HofMappersTest` — entry + six slots round-trip, empty slots included.
- `HofFiltersTest` — if the entry list gets search/sort, keep it pure here.

Robolectric:

- `HallOfFameRepositoryImplTest` — save creates exactly six slot rows; a
  re-save replaces rather than duplicates them; deleting the entry removes its
  slots; deleting the hack removes both.

Manual (`docs/test-plan.md`): the whole create → view → edit → delete loop,
camera and gallery screenshots, the pokédex-empty path, back-with-unsaved-
changes from both the top bar and the system gesture.

---

## 6. Definition of done

- [ ] A Hall of Fame with six fully-filled Pokémon can be created, viewed,
      edited and deleted.
- [ ] Every field survives a round trip through the database unchanged.
- [ ] Sprites respect the hack's generation and the shiny flag.
- [ ] EV total > 510 blocks the slot; nothing else does.
- [ ] Discarding unsaved changes is confirmed for both back paths.
- [ ] A hack with exactly one entry shows it inline under its own header.

## 7. Pitfalls

- Never resolve a saved slot's species name from the cache — it is stored on
  the row precisely so a cache wipe cannot blank a saved team.
- `remember(key)` the slot draft on `slotIndex`, or editing slot 3 after
  slot 1 shows stale state.
- A `Dialog` with `usePlatformDefaultWidth = true` (the default) will not go
  full screen.
- The camera path needs a `FileProvider` entry in the manifest **and**
  `res/xml/file_paths.xml`; forgetting the latter throws only at capture time.
