# Phase 4 — Reusable Pokémon templates

**Goal:** a Pokémon configured once can be reused in any Hall of Fame, in any
hack.

**Depends on:** Phase 3 (the slot editor and its draft model).

---

## 1. Repository and model

`PokemonTemplateEntity` and its DAO already exist from Phase 1. Add:

```
domain/repository/PokemonTemplateRepository.kt   observeAll, getById, save, delete, duplicate
data/repository/PokemonTemplateRepositoryImpl.kt
domain/model/PokemonTemplate.kt                  (exists) + conversions to/from a slot draft
```

The two conversions are the whole feature and belong in
`domain/model/TemplateConversions.kt`, pure and unit-tested:

```kotlin
fun PokemonSlot.toTemplateDraft(label: String): PokemonTemplate   // drops id/entryId/slotIndex
fun PokemonTemplate.toSlotDraft(slotIndex: Int): SlotDraft        // sets sourceTemplateId = id
```

`toSlotDraft` sets `sourceTemplateId`; `toTemplateDraft` never copies it —
a template made from a slot that itself came from a template is a new,
independent template.

---

## 2. Slot editor integration

Add the two buttons deferred in Phase 3, in the slot editor's top bar:

- **Load from template** → a bottom sheet listing every template (sprite,
  label, species, level). Picking one **replaces the entire slot draft** and
  shows a snackbar with an Undo action restoring the previous draft. Replacing
  rather than merging is the simpler, predictable behaviour; say so in the
  sheet's subtitle ("This replaces everything in the slot").
- **Save as template** → a dialog asking for a label, pre-filled with the
  nickname or species name. If a template with that exact label already
  exists, offer "Overwrite" or "Save as a copy" rather than silently doing
  either.

---

## 3. Templates screen

```
ui/templates/TemplatesScreen.kt, TemplatesViewModel.kt, TemplatesUiState.kt
ui/templates/TemplateFormScreen.kt (or reuse SlotEditorDialog with a template mode)
```

Prefer **reusing `SlotEditorDialog`** with a `mode: SlotEditorMode` parameter
(`Slot` or `Template`, the latter adding the label field and hiding the
template buttons) over writing a second near-identical editor. If that turns
out to entangle the two, split it and record why in
`docs/implementation-decisions.md`.

The screen (already a drawer destination since Phase 0) lists templates with
sprite + label + species/level summary, a search field filtering on label and
species, and per-row actions: Edit, Duplicate, Delete. Deleting asks for
confirmation and states plainly that Halls of Fame created from it are not
affected. Empty state explains what templates are for.

Templates have no hack context, so their sprites render with
`GameGeneration.OTHER` (official artwork) unless `alwaysUseLatestSprites` is
on, in which case the setting already produces the same thing.

---

## 4. Tests

Plain JVM:

- `TemplateConversionsTest` — slot → template drops identity fields and keeps
  every payload field; template → slot sets `sourceTemplateId` and the given
  `slotIndex`; a round trip is lossless for all payload fields (write it
  field-by-field so a newly added column fails the test loudly).

Robolectric:

- `PokemonTemplateRepositoryImplTest` — save/observe/delete; **deleting a
  template leaves slots with that `sourceTemplateId` untouched** (the
  no-foreign-key invariant from spec §2).

Manual: create a template from a filled slot, load it into a different hack's
entry, confirm every field arrives, delete the template and confirm the entry
is unchanged.

---

## 5. Definition of done

- [ ] A slot can be saved as a template and loaded into any other hack.
- [ ] Loading replaces the slot and is undoable.
- [ ] Templates can be renamed, edited, duplicated, deleted.
- [ ] Deleting a template does not touch any Hall of Fame.

## 6. Pitfalls

- Do not add a foreign key on `sourceTemplateId`.
- A duplicated template needs a new UUID **and** a distinct label
  ("Label (copy)"), or the list is confusing.
