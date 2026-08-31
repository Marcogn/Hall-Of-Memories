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

*(suggested coverage: first-launch sync completes and Settings shows seven
fresh stages; killing the app mid-sync resumes rather than restarting;
airplane mode during a sync produces a readable error and a working Retry;
"invalidate and re-download" refills the cache and leaves user data intact.)*

## Phase 2 — Hacks

*(suggested coverage: a real TheGamesDB search with a real key; a search for a
ROM hack name that finds nothing; a gallery pick; a hack with no artwork; the
API-key-missing message; deleting a hack removes its image files.)*

## Phase 3 — Hall of Fame

*(suggested coverage: the full create → view → edit → delete loop with six
filled slots; gallery and camera screenshots; sprites correct per generation
and shiny; a Gen-9 species inside a Gen-3 hack still renders; the EV 510
limit; discarding unsaved changes from both the top bar and the system back
gesture; the slot editor with an empty pokédex cache.)*

## Phase 4 — Templates

*(suggested coverage: save a slot as a template, load it into a different
hack, verify every field; delete the template and confirm the Hall of Fame is
untouched.)*

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
