# Implementation plan — how to execute it

These files are written to be executed by a coding agent (Sonnet), one phase
per session, with no further design work required. Everything that could be
decided in advance has been decided; where a phase leaves something open it
says so explicitly and tells you what to do about it.

## Order

| Phase | File | Ships |
|---|---|---|
| 0 | [`phase-0-foundation.md`](phase-0-foundation.md) | Buildable, empty-but-real app: Gradle, Hilt, Compose, theme, i18n, navigation skeleton, CI |
| 1 | [`phase-1-data-and-pokedex-sync.md`](phase-1-data-and-pokedex-sync.md) | Room schema, PokéAPI sync engine, sprite resolver, settings screen |
| 2 | [`phase-2-hacks.md`](phase-2-hacks.md) | Hack CRUD, TheGamesDB search, box art / logo, home screen |
| 3 | [`phase-3-hall-of-fame.md`](phase-3-hall-of-fame.md) | Hall of Fame entry CRUD + the six-slot editor |
| 4 | [`phase-4-templates.md`](phase-4-templates.md) | Reusable Pokémon templates, cross-hack |
| 5 | [`phase-5-polish-and-backup.md`](phase-5-polish-and-backup.md) | Multi-entry previews, screenshots, local zip backup, Drive placeholder |
| 6 | [`phase-6-release.md`](phase-6-release.md) | Keystore, release pipeline, README, CHANGELOG |

Read before starting any phase: [`../../CLAUDE.md`](../../CLAUDE.md),
[`../spec.md`](../spec.md), this file, the phase file itself, and
[`reference-pokeapi.md`](reference-pokeapi.md) if the phase touches PokéAPI
or sprites.

**Do not start a phase before the previous one is merged.** Each phase assumes
the previous one's code exists and compiles.

## Working rules

1. **Stay inside the phase.** If you spot something worth doing that belongs
   to a later phase, write it down in that phase's file (a `> NOTE from
   phase N:` line) instead of implementing it.
2. **Copy the sibling app, don't invent.** `../../../ThePatientGamerHelper`
   (when checked out alongside) is the reference implementation for Gradle
   setup, Hilt modules, Room + Flow + `combine()` ViewModels, `ImageStorage`,
   SAF export, `HttpURLConnection` clients and the CI workflows. Matching its
   patterns is worth more than a marginally better idea.
3. **No new dependencies** beyond the pinned catalogue in Phase 0. If a phase
   seems to need one, stop and flag it in the PR description instead of adding
   it. This has held across ThePatientGamerHelper's eight phases; charting,
   HTTP clients and drag-to-reorder were all hand-rolled.
4. **No hardcoded user-visible strings.** Every one goes through
   `stringResource()` (Compose) or `context.getString()` (ViewModel, with
   `@ApplicationContext` injected), and lands in **both**
   `values/strings.xml` (Italian, default) and `values-en/strings.xml`
   (English) in the same commit.
5. **Room migrations are additive.** `fallbackToDestructiveMigration()` is
   banned outright. Phase 1 creates schema v1; anything after it that changes
   a table writes a numbered `MIGRATION_x_y` and bumps the version.
6. **Pure logic lives in `domain/`** with no Android imports, so it is
   unit-testable on the plain JVM: filtering, sprite URL resolution, PokéAPI
   response parsing, validation, backup DTOs. `data/` owns everything that
   touches Android or the network.
7. **Changelog as you go.** Every phase adds its entries to `CHANGELOG.md`'s
   `## [Unreleased]` section, in the convention described in `CLAUDE.md`
   (one bold lead-in per user-facing change) — the release workflow reads
   exactly those lead-ins.
8. **Update the docs the phase touches**: `docs/test-plan.md` gets a new
   section per phase, `docs/implementation-decisions.md` gets an entry for
   every non-obvious choice you had to make, `CLAUDE.md`'s phase-status list
   gets ticked.

## Definition of done, every phase

- [ ] Everything in the phase's "Deliverables" exists and compiles.
- [ ] Unit tests listed in the phase are written and pass.
- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug` is green — or, if
      the environment has no Android SDK (see below), the `Android CI`
      workflow run for the pushed branch is green.
- [ ] `CHANGELOG.md` `[Unreleased]` updated.
- [ ] `docs/test-plan.md` has this phase's manual-verification section.
- [ ] `CLAUDE.md` phase status updated.
- [ ] Committed and pushed to the working branch; draft PR opened or updated.

## Building in a sandboxed session

`./gradlew` needs both the Maven repositories (`dl.google.com`,
`repo1.maven.org`) **and** an Android SDK. In the planning session both repos
were reachable but `ANDROID_HOME` was unset and no SDK was installed. Check,
in this order, and do not burn time fighting it:

```bash
curl -sfo /dev/null -w '%{http_code}\n' https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/8.13.0/gradle-8.13.0.pom
echo "$ANDROID_HOME"; command -v sdkmanager
```

If there is no SDK and none can be installed, say so plainly in the PR,
push, and let the `Android CI` workflow do the verification — then read its
logs and fix forward. Never claim a build passed that you did not run.

## Verification you cannot automate

Some things have no meaningful test on this stack, exactly as in
ThePatientGamerHelper: anything requiring the real network (PokéAPI sync end
to end, TheGamesDB search), the photo picker, `AppCompatDelegate` language
switching, and real sprite rendering. Those go into `docs/test-plan.md` as
manual steps for the user to run on-device, and every real bug found that way
gets an entry in that file's "Known regressions".

## Commits and PRs

- Small, logical commits with imperative English messages
  (`Add PokedexSyncManager and its cache tables`).
- Branch: the one named in the session instructions.
- Push, then open a **draft** PR whose description lists what the phase
  delivered, what is deliberately not done yet, and anything you had to
  decide that the plan did not cover.
