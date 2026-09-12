# Next steps

A concise backlog of work deliberately left open, each entry cited against
where it's documented today. This is not a phase plan — nothing here is
scheduled — it's a pointer to real, already-recorded gaps so they aren't
rediscovered from scratch.

## Google Drive backup (v2)

Deliberately out of scope for v1. `docs/spec.md`, "§5 ... v1 (in scope)"
section states: **"v2 (out of scope, spec to be written separately):
Google Drive backup."** — no v2 spec document exists yet; it is currently
only that one section inside `docs/spec.md` (also listed under "§8. Out of
scope for v1"). `docs/implementation-decisions.md`, "Google Drive is a
transport, deferred" records the seam already built to make this cheap
later: `BackupRepository` produces/consumes a `BackupPayload` with no
knowledge of where the bytes go, `LocalBackupManager` owns the SAF-based
local path, and a future `DriveBackupManager` would be "a sibling class,
not a change to the repository or the UI." v1 ships the disabled "Google
Drive backup" Settings row with a "Coming soon" badge as the visible half
of that seam (`docs/spec.md` §5, `README.md`'s "Local backup" feature
section).

## Duplicate hack detection/merge

Deliberately left open, not attempted. `docs/implementation-decisions.md`,
"Duplicate hacks are not detected or merged — deliberately left open":
`HackDao` has no uniqueness constraint on `name`, so adding "the same hack"
twice creates two independent rows. Fixing it for real needs both a
name-collision check and a merge/confirm UX ("a hack named X already
exists — open it instead?") that doesn't exist as a pattern in this app or
its sibling ThePatientGamerHelper yet, and the check would need to be
advisory rather than a hard block since two hacks can legitimately share a
display name (e.g. a hack and an unrelated remake).

## Published app screenshots in the README

`README.md`, "Screenshots" section currently reads: *"(coming soon — the
app has no published screenshots yet.)"* — still true as of this writing.

## Proposed improvements (approved by the user, 2026-09-12)

Not yet planned in any phase document — these are new proposals raised
during a cross-repo audit and approved for the backlog, not gaps found in
existing docs. Each still needs its own design pass before implementation.

- **Statistics screen.** Modeled on ThePatientGamerHelper's Phase 3
  (`domain/stats/LibraryStatisticsCalculator.kt`,
  `ui/stats/StatsScreen.kt`): species/generation distribution across saved
  Halls of Fame, shiny rate, maybe average level or completion counts by
  hack. This reverses a prior decision — `CLAUDE.md`'s "What NOT to do
  until explicitly requested" listed "A statistics screen" as excluded
  until this was explicitly revisited and reversed on 2026-09-12 (see
  `CLAUDE.md`'s note in that section).
- **"Favorite" flag on Hall of Fame entries.** A boolean field to filter/
  sort by, useful once the library grows past a handful of entries.
  Would need a Room migration (additive column, default `false`) and a
  filter/sort option alongside the existing ones in
  `domain/filter/`.

## Pokémon Showdown import/export — ship it in a release

`CHANGELOG.md`'s `[Unreleased]` section currently holds exactly one entry:
**"A Hall of Fame or template slot can now be filled from a pasted Pokémon
Showdown set."** (import via "Import from Showdown" in the slot editor,
plus the reverse "Copy as Showdown"). It has not yet been cut into a
published version — the latest published release is
[v1.0.0](https://github.com/marcogn/hall-of-memories/releases/tag/v1.0.0)
(2026-09-04), which predates this feature (see the commit history: the
Showdown work landed after the "Cut release 1.0.0" commit). Cutting a
release for it is a manual `Release` workflow dispatch per `CLAUDE.md`,
"Changelog and release process."
