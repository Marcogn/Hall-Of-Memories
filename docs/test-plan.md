# Manual test plan

What cannot be covered by automated tests on this stack, and must be checked
by hand on a device or emulator: the real network (PokéAPI sync, TheGamesDB),
the photo picker and camera, locale switching, sprite rendering, and SAF
file dialogs.

**Rules for this file**

- Every phase adds its own section, written when the phase is implemented.
- Every real bug found during on-device verification gets an entry under
  "Known regressions" — including ones fixed immediately, because the entry is
  what stops the same mistake recurring.
- Steps are written so someone who has not read the code can follow them.

---

## Phase 0 — Foundation

1. Install the debug APK on a device or emulator and launch it. It opens on
   the hack library, showing the empty state ("No hacks yet" / "Nessuna hack
   ancora").
2. Tap the hamburger icon; the drawer opens with three entries: Hall of
   Memories, Saved Pokémon, Settings.
3. Tap each drawer entry in turn. Saved Pokémon shows its own empty state;
   Settings shows the Appearance and Language sections. Reopening the drawer
   and returning to Hall of Memories does not lose or duplicate back-stack
   entries (repeatedly switching sections should not make the system back
   button step through every prior visit).
4. In Settings, switch the theme radio between System, Light and Dark. Each
   choice applies immediately, with no visible flash or delay, everywhere in
   the app (not just the Settings screen).
5. In Settings, switch the language radio to English, then to Italian, then
   back to System. Every screen's text relabels immediately with no manual
   restart. Force-close and relaunch the app: the chosen language (and the
   chosen theme) are both still applied.
6. With the system set to a language other than Italian or English, set the
   in-app language to System and confirm the app falls back to Italian (the
   default resource locale) rather than crashing or showing raw resource
   keys.

## Phase 1 — Data and pokédex sync

1. Fresh install, launch with a normal network connection. The Home screen
   shows a non-blocking banner ("Downloading pokédex data: <stage> (n/total)")
   while the sync runs; the rest of the UI (drawer, Settings) stays usable
   throughout. The banner disappears once every stage completes.
2. Open Settings → Pokédex data. All seven stages (Species, Moves, Types,
   Natures, Abilities, Items, Generations) show a last-synced timestamp and
   an item count roughly matching `docs/plan/reference-pokeapi.md` §2
   (Species ~1300+, Moves ~937, Natures 25, Abilities ~370, Items ~2000+).
3. Force-close the app mid-sync (during the Home banner, before it
   disappears) and relaunch. The sync resumes rather than restarting from
   Species — stages already completed before the kill keep their timestamp
   from step 2 unchanged; only the interrupted stage (and anything after it)
   re-runs.
4. Turn on airplane mode, then Settings → "Invalidate and re-download". The
   banner/section shows a readable failure message naming what failed (not a
   generic "sync failed"), with a working Retry. Turn airplane mode back off
   and tap Retry: the sync completes normally.
5. Settings → "Invalidate and re-download" with a normal connection: the
   confirmation dialog states plainly that hacks/Halls of Fame/templates are
   unaffected. After confirming, every stage's timestamp and item count
   refresh. (No hack/Hall of Fame UI exists yet to verify "leaves user data
   intact" end-to-end — re-verify this step once Phase 2/3 land real data to
   check against.)
6. With the sync already complete, force-close and relaunch the app: no
   banner appears and no new network requests fire (the app opens directly
   to a synced state) — confirmable via Settings showing unchanged
   timestamps immediately after relaunch.

## Phase 2 — Hacks

1. Fresh install, no TheGamesDB API key set. Home shows the empty state
   ("No hacks yet" / "Nessuna hack ancora") with an add button. Tap it, fill
   in a name and generation, leave artwork untouched, and save. Home now
   shows the new hack with a generated placeholder (initials on a
   deterministic color) instead of artwork.
2. Open the new hack's detail screen. It shows the same placeholder, the
   name and generation, "0" Halls of Fame, and a `coming_soon` note for
   adding one (Phase 3 not built yet).
3. Edit the hack, open the box art search, and search for a real game title
   (e.g. "Pokemon FireRed") without an API key set. The dialog shows a clear
   "no API key configured" message rather than a network error, with no
   spinner left hanging.
4. Settings → TheGamesDB, paste a real API key and save. The save button
   disables once the field matches what's stored; relaunch the app and
   confirm the key round-trips (the field pre-fills with it, masked or not
   as implemented).
5. Back in the hack form, search box art for "Pokemon FireRed". Results list
   shows cover thumbnails, platform and year for more than one match. Pick
   one: box art and (if the picked game has a clear logo) logo both download
   and preview inline, with a downloading spinner visible while the request
   is in flight.
6. Search for a ROM hack's own name that TheGamesDB has never heard of
   (e.g. "Radical Red"). The dialog shows an explanatory "no results" state,
   not an error — manual gallery pick stays available from the same screen.
7. Pick a logo from the gallery instead (Choose from gallery action). It
   replaces whatever was there (TheGamesDB result or none) and previews
   correctly, including that a picked logo with transparency (a PNG) still
   looks correct over the box art background, not flattened to a solid
   color.
8. Edit the hack again, replace the box art with a different gallery image,
   and save. Confirm (via a file manager / adb shell on the emulator, or by
   editing again and checking nothing broke) that the previous box art file
   is gone from the app's internal storage and only the new one remains —
   not both.
9. Start editing the hack, change the artwork, then discard via system back
   without saving. Reopen the hack: the original artwork is unchanged and
   the discarded file was never written to the DB (a stray orphaned image
   file on disk here is an acceptable, non-corrupting side effect, not a
   pass/fail condition itself — but the *saved* state must be untouched).
10. Create 2–3 more hacks across different generations. Use the search field
    and the generation filter chips on Home together (e.g. search "red" +
    filter to Generation I) and confirm the AND-combination narrows
    correctly, including accented/uppercase input matching normally-typed
    names.
11. Toggle the list/grid view button on Home. Both views show the same
    filtered set; force-close and relaunch — the chosen view mode persists.
12. Delete a hack from its detail screen (with a confirmation dialog naming
    it has zero Hall of Fame entries). Confirm its box art/logo files are
    removed from internal storage and it disappears from Home immediately.

## Phase 3 — Hall of Fame

1. From a hack with no entries, tap "Add the first Hall of Fame". Fill in a
   player name (leave everything else blank) and save immediately. Confirm
   it saves with zero slots filled — nothing else is required.
2. Create another entry. Tap slot 1, search a species (e.g. "charizard"),
   pick it from the results, and confirm the sprite shown matches the
   hack's generation. Fill nickname, gender, level 100, toggle shiny, pick a
   nature (check the "+Atk / −SpA"-style hint appears in the dropdown),
   ability, held item, and all four moves via their suggestion dropdowns —
   confirm typing something not in the dropdown (e.g. a homebrew move name)
   is still accepted. Fill IVs at 31 each and EVs at 252/252/4 (total 508).
   Confirm the slot.
3. Repeat for the other five slots with varied data (leave one deliberately
   empty). Save the entry and confirm the detail view shows exactly six
   cards, the empty one dimmed and labeled, and every field survives the
   round trip exactly as typed (including a nickname distinct from the
   species, and a shiny marker).
4. Reopen the entry for editing, open a filled slot, and try to push a
   single EV field to 253 and to −1: both show an inline error and the
   value is not committed to the slot's saved data (re-close and reopen the
   slot to confirm the last valid value stuck, not the rejected one).
5. Push the EV total over 510 (e.g. 252/252/10/0/0/0 = 514): confirm the
   slot's confirm button disables and an inline error appears on the EV
   block; bring the total back to exactly 510 and confirm it re-enables.
6. Test a shiny in an early generation that doesn't support them (e.g. a
   Gen-1 hack) and confirm the sprite resolver's documented fallback
   applies rather than a broken image. Separately, place a Gen-9-only
   species (e.g. a Paldea starter) into a Gen-3 hack's slot and confirm the
   sprite still renders via the fallback chain, not a blank box.
7. Add a screenshot from the gallery, save, then edit again and replace it
   with a camera capture (needs a device/emulator with a working camera
   app). Confirm the new photo appears immediately and the old file is no
   longer in the app's internal storage after save (not before — a
   cancelled edit must leave the original intact).
8. From the entry detail, tap the screenshot: confirm it opens full-screen
   and pinch-to-zoom/pan works, then dismiss it.
9. Start editing an entry, change the player name and a slot, then press
   the system back gesture (not the top bar's back arrow): confirm the
   "discard changes?" dialog appears, and that both "discard" and "cancel"
   behave correctly. Repeat using the top bar's back arrow instead of the
   gesture — both paths must ask.
10. On a fresh install with the pokédex cache not yet downloaded (or after
    "Invalidate and re-download" in Settings, checked before the sync
    finishes), open the slot editor: confirm the "Pokédex data hasn't been
    downloaded yet" card appears with a working "Download now" action, that
    species search returns nothing until the sync completes, and that every
    other field (nickname, nature, ability, held item, moves as free text)
    still works normally in the meantime.
11. With two or more entries under one hack, confirm the hack detail screen
    shows a real thumbnail per entry (the screenshot if set, else slot 0's
    sprite, else a generic placeholder) and that the newest/oldest sort
    toggle actually reorders the list.
12. Delete an entry from its detail screen (confirmation dialog) and confirm
    its screenshot file is removed from internal storage and it disappears
    from the hack's entry list immediately. Delete a hack with several
    entries and confirm all of their slots and screenshots are gone too
    (cascade, already covered at the DB layer by
    `HallOfFameDaoTest`/`HackRepositoryImplTest`, but worth a device
    sanity check for the files).

## Phase 4 — Templates

1. In a Hall of Fame entry's slot editor, fill a slot completely (species,
   nickname, nature, ability, held item, four moves, IVs, EVs) and tap "Save
   as template". Confirm the label field is pre-filled from the nickname
   (or species name if there's no nickname), edit it, and save. Open the
   Templates screen from the drawer and confirm the new template appears
   with the right sprite, label and level.
2. Create a second, unrelated Hall of Fame entry under a *different* hack.
   Open a slot's editor, tap "Load from template", and pick the template
   from step 1. Confirm every field arrives (species, nickname, nature,
   ability, item, all four moves, every IV/EV) and that the sprite renders
   using this second hack's own generation, not the template's origin hack.
3. Immediately after loading, tap "Undo" on the snackbar and confirm the
   slot reverts to exactly what it had before the load (including if it
   was completely empty before).
4. Fill a different slot, tap "Save as template", and type a label that
   exactly matches an existing template's name. Confirm the dialog offers
   "Overwrite" and "Save as a copy" instead of a single Save button; try
   "Save as a copy" and confirm the Templates screen now shows two entries
   with that label. Repeat and choose "Overwrite" instead, confirming the
   original template's data changes in place (and its position in the
   list, sorted by most-recently-updated, moves to the top).
5. From the Templates screen, tap the search field and search by species
   name (not label) — confirm it still matches. Search by a label
   substring with accents/case varied and confirm it still matches.
6. Tap the "+" FAB to create a template directly (not from a slot). Confirm
   the label field is required (Confirm stays disabled until it's filled)
   and that saving it makes it immediately available from "Load from
   template" in any hack's slot editor.
7. Edit an existing template directly from the Templates screen (tap the
   row or its edit icon), change a field, and save. Confirm the change
   sticks and that no Hall of Fame entry that previously loaded this
   template retroactively changes (denormalized snapshot, not a live
   reference).
8. Duplicate a template and confirm the copy has a distinct label
   ("<name> (copy)"), a different id (editing one never affects the
   other), and identical payload data.
9. Delete a template that a saved Hall of Fame slot's `sourceTemplateId`
   points to. Confirm the confirmation dialog explicitly says existing
   Halls of Fame are unaffected, and then confirm that slot's data
   (species, moves, stats, everything) is still fully intact after the
   delete — only the template itself disappears from the Templates screen.
10. With the pokédex cache empty (see Phase 3's step 10), open the "New
    template" editor and confirm the same "not downloaded yet" card and
    "Download now" action appear as in the Hall of Fame slot editor.

## Phase 5 — Polish and backup

*(suggested coverage: export to Downloads, uninstall, reinstall, import —
everything back including images; import a truncated archive and confirm
nothing changed; the zoomable screenshot viewer; carousel behaviour with 2, 5
and 20 entries.)*

## Phase 6 — Release

*(suggested coverage: install a signed release APK over a previous one; a full
`Release` workflow dry run on a throwaway version number.)*

---

## Known regressions

*(one entry per real bug found on device: what was observed, the root cause,
where it was fixed. Empty until the first is found.)*
