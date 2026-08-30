# Phase 5 — Multi-entry presentation, screenshots, local backup

**Goal:** the app stops feeling like a form collection. Plus the one piece of
safety-net infrastructure v1 owes the user: a local backup they control, and
the seam that makes Google Drive (v2) a transport swap rather than a rewrite.

**Depends on:** Phases 2–4.

---

## 1. Presentation polish

- **Hack detail, N > 1:** a horizontally scrollable carousel of entry
  previews above the most recent entry rendered inline, or a plain list —
  pick the carousel, it is what the spec asks for, and keep the list as the
  layout for a hack with more than six entries where a carousel stops being
  scannable.
- **Entry preview thumbnails:** screenshot → slot-0 sprite → placeholder,
  already specified in Phase 3; here they get proper aspect handling
  (`ContentScale.Crop` for screenshots, `Fit` for sprites) and a loading
  shimmer.
- **Full-screen screenshot viewer**: pinch-zoom and double-tap-to-zoom, using
  `Modifier.pointerInput` + `graphicsLayer` — no new dependency.
- **Home grid tiles**: keep the real aspect ratio of box art
  (`ContentScale.FillWidth` inside a staggered grid), fixed 2:3 for the
  placeholder — the sibling app's `GameGridTile` solved exactly this.
- **Empty and error states** reviewed across every screen for a consistent
  voice, all strings in both locales.
- **Sprite loading**: give `PokemonSprite` a `crossfade(true)` and a stable
  placeholder so a list of six sprites does not flash.

---

## 2. Local backup and restore

```
domain/backup/BackupPayload.kt        @Serializable DTOs + domain mapping + file naming
domain/repository/BackupRepository.kt exportPayload(): BackupPayload
                                      importPayload(payload, images): ImportResult
data/repository/BackupRepositoryImpl.kt
data/backup/BackupArchive.kt          zip builder + reader (java.util.zip)
data/backup/LocalBackupManager.kt     SAF write/read, orchestration
ui/settings/BackupSection.kt
```

Format: a single `.zip` containing `data.json` and `images/<filename>`.

```
BackupPayload
  formatVersion: Int = 1
  exportedAt: Instant
  hacks: List<BackupHackDto>          -- each with its entries, each with its six slots
  templates: List<BackupTemplateDto>
```

Rules, all lifted from the sibling app's hard-won ones:

- The payload carries **image file names only**, never absolute paths — a path
  from another device is meaningless. Restore resolves each name to a new
  local path.
- The **PokéAPI cache is never exported.** It is re-downloadable, it is the
  bulk of the data, and a stale cache restored onto a device is worse than no
  cache.
- Restore is a **full replace inside one transaction**, preserving every id
  and timestamp: delete all user rows, delete all local images, unpack, insert.
  Single-user app, no merge, no conflict resolution. The confirmation dialog
  says exactly that.
- Order matters on restore: hacks → entries → slots (foreign keys), then
  templates.
- Export always covers **everything**, ignoring any active UI filter. A
  backup that respects a filter is not a backup.
- New DTO fields added later must have defaults so older files still decode.
  `formatVersion` exists to reject a *newer* file with a clear message, not to
  branch parsing.

File I/O is SAF only — `ActivityResultContracts.CreateDocument` for export,
`OpenDocument` for import. Never write to external storage directly.

Failure handling: a malformed archive fails the whole import with the reason
named (which file, what was wrong) and **nothing written**. Images are
best-effort: a missing image inside an otherwise valid archive imports the
row with a null path and reports how many images were skipped.

### The Drive seam

`BackupRepository` produces and consumes a `BackupPayload` and knows nothing
about where bytes go; `LocalBackupManager` owns SAF. A v2 `DriveBackupManager`
is then a sibling of `LocalBackupManager`, not a change to either the
repository or the UI. Add the Settings row now:

> **Google Drive backup** · *Coming soon* — disabled, with a badge, per
> spec §5.

---

## 3. Statistics — explicitly not in this phase

The source spec has no statistics screen and this phase does not add one.
If it is wanted later it is a v2 item with its own spec: totals per hack,
most-used species across Halls of Fame, average playtime.

---

## 4. Tests

Plain JVM:

- `BackupPayloadTest` — full DTO ↔ domain round trip; a payload written
  without an optional field still decodes; a `formatVersion` from the future
  is rejected with a typed error.

Robolectric:

- `BackupArchiveTest` — build a zip, read it back, verify entry names and that
  images land in `images/`; a zip missing `data.json` fails cleanly; a zip
  with an image referenced by no row imports fine.
- `BackupRepositoryImplTest` — export → wipe → import restores identical rows,
  ids and timestamps included.

Manual: export to Downloads, uninstall/reinstall, import, everything back
including images; import a truncated file and confirm the database is
untouched.

---

## 5. Definition of done

- [ ] Export produces a zip that imports cleanly onto a fresh install.
- [ ] Import is all-or-nothing; a corrupt file changes nothing.
- [ ] Images survive the round trip.
- [ ] The Drive row is present, disabled, badged.
- [ ] Carousel, thumbnails and the zoomable screenshot viewer all work.
