# Phase 7 — In-app updates and an Info screen

**Goal:** Hall of Memories checks this repository's GitHub Releases at startup (at a frequency the
user picks), shows what is new, downloads the APK on request and hands it to the system installer;
an Info screen shows the installed version, the disclaimers and "made with ❤ by marcogn".

**Depends on:** Phases 0–6 merged, and **ThePatientGamerHelper Phase 9 merged and checked on the
device** — that is the pilot this phase copies (rule 2, "Copy the sibling app"). The shared design,
the checked facts and their sources are in its plan:
[`ThePatientGamerHelper/docs/plan-phase-9-updates.md`](https://github.com/Marcogn/ThePatientGamerHelper/blob/main/docs/plan-phase-9-updates.md).

**Model:** Sonnet (a port of a reviewed design; the expensive decisions were made in the pilot's Opus
stage).

Requested by the author on 2026-10-11 for all six of their apps; decisions in
`docs/implementation-decisions.md`, "In-app updates and Info screen, planned".

---

## 1. Update package (copied from the pilot)

- `domain/update/` (pure, JVM tests copied with it): `AppVersion`, `ReleaseInfo` parser,
  APK asset picker, release-body cleanup, `UpdateFrequency` + `UpdateCheckPolicy.isDue`.
- `data/update/`: `GitHubReleaseClient` (`HttpURLConnection` like `PokeApiClient`: no
  `Accept-Encoding`), `ApkDownloader` (SHA-256 against the asset `digest`), `ApkVerifier` (package,
  higher `versionCode`, same signing certificate), `ApkInstaller` (`PackageInstaller` session, system
  confirmation, "install unknown apps" round trip), `UpdatePreferences`, `UpdateManager`.
- Adaptations to this app:
  - owner/repo `Marcogn/Hall-Of-Memories` as `BuildConfig` fields;
  - the startup check runs on the existing `@ApplicationScope` scope (`di/CoroutinesModule.kt`);
  - preferences in SharedPreferences like the pilot (decided at implementation, see
    `docs/implementation-decisions.md`, "Phase 7 as built");
  - the asset is picked by its `.apk` suffix: the kit renamed it to `Hall-Of-Memories-x.y.z.apk`
    (decisions, "Release asset renamed"), and nothing may depend on the name.
- Manifest: `REQUEST_INSTALL_PACKAGES`. The existing FileProvider is not needed (session API) and
  stays as it is. **No new dependency** (rule 3): everything is platform API. No WorkManager either:
  startup check only.

## 2. Update UI

- Dialog on a newer release: version, the Release body headlines, "Full changelog", Download /
  Not now; progress with Cancel; then the system installer. Errors with their reason.
- Settings, new row in the Appearance/Language area or its own "Updates" section with
  `SettingsSection`: frequency Never / Every start / Daily / Weekly (**default Weekly**).
- "Not now" closes; the next due check asks again. Debug builds never check automatically.

## 3. Info screen

- `ui/about/AboutScreen`, `Destination.About`, an entry in the drawer under Settings, with the
  `BackHandler` the drawer screens need (Known gotchas).
- Content: app name, `BuildConfig.VERSION_NAME` (no version code, as in the pilot's 1.1.2), "Check for updates" with its
  result, "Fatto con il ❤️ da Marcogn" / "Made with ❤️ from Marcogn" (the pilot's wording after its 1.1.1), GitHub link, MIT licence.
- Disclaimers (wording reviewed by the author in the PR): unofficial fan-made app, not affiliated with
  or endorsed by Nintendo, Game Freak, Creatures or The Pokémon Company; Pokémon and the species
  names are their trademarks; species, move and ability data from PokéAPI, sprites from its sprite
  repository; box art and logos from TheGamesDB, which this app is not affiliated with; ROM hacks
  belong to their authors.
- Strings in both `values/` and `values-en/` (rule 4).

## 4. Done when

The Definition of done in `README.md`, and:
- JVM tests of `domain/update` (copied, still green) and Robolectric tests for the dialog/ViewModel
  state mapping and the frequency preference.
- `./gradlew lint testDebugUnitTest assembleDebug` green; CI green.
- `docs/test-plan.md` "Phase 7" section; CHANGELOG `[Unreleased]` entries; README "Updates" and
  "Info"; `CLAUDE.md` phase ticked.

## 5. Device checks

Install an older signed release, start: dialog with the newer version; download; "install unknown
apps" the first time; system installer; the app reopens on the new version. Airplane mode at startup:
nothing shown, no crash. "Never" never asks at startup, "Check now" still works. Info shows the
installed version, both languages, dark theme.

## 6. Out of scope

Silent install, pre-releases, background checks, a licences screen for third-party libraries, Google
Drive backup (v2).

## Open questions

- Disclaimer wording (proposed above).
- Release number for the build that carries this (proposed: 1.2.0).

## Risks

Same as the pilot's: GitHub API or asset changes (the parser fails closed), `digest` missing (size +
signature check), OEM differences in the unknown-apps screen.
