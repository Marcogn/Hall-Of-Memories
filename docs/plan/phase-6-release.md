# Phase 6 — Signing, release pipeline, documentation

**Goal:** Hall of Memories can be cut as a signed, versioned, documented
release the same way ThePatientGamerHelper is.

**Depends on:** Phases 0–5 merged.

---

## 1. Keystore

Generate **once**, keep forever — a keystore regenerated per build changes the
app's signature and makes upgrades impossible on any device that already has
it installed.

```bash
keytool -genkeypair -v \
  -keystore hall-of-memories-release.keystore \
  -alias hall-of-memories \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

Document in `docs/release-signing.md`:

- where the file is kept (a password manager or an encrypted backup — **not**
  in the repository, and `*.keystore` in `.gitignore`);
- that losing it means never being able to update an installed build;
- how to produce the secrets:
  `base64 -w0 hall-of-memories-release.keystore` → `RELEASE_KEYSTORE_BASE64`;
- the four GitHub secrets: `RELEASE_KEYSTORE_BASE64`,
  `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`,
  plus `RELEASE_PUSH_TOKEN` (fine-grained PAT, Contents read/write on this
  repo) used by the release workflow's version-bump push;
- how to read the SHA-1 back out (`./gradlew signingReport`), noted as
  useful even without Drive, since any future Google API integration will
  need it.

The `app/build.gradle.kts` signing block that consumes these env vars was
already written in Phase 0.

---

## 2. Workflows

```
.github/workflows/android-ci.yml    (exists, Phase 0)
.github/workflows/build-apk.yml     new — manual signed release APK
.github/workflows/release.yml       new — the full release cut
```

Port both from ThePatientGamerHelper, dropping every `DRIVE_OAUTH_WEB_CLIENT_ID`
reference (this app has no build-time secret of that kind).

`build-apk.yml`: `workflow_dispatch` → checkout → JDK 17 → Gradle cache →
decode the keystore from `RELEASE_KEYSTORE_BASE64` into `$RUNNER_TEMP` →
print its size and SHA-256 (not its contents — this is how a truncated secret
is told apart from a wrong password) → `assembleRelease` with the four signing
env vars → `signingReport` → upload the APK artifact.

`release.yml`: `workflow_dispatch` with a hand-typed `version` input, and the
ordering that matters:

1. Validate: `x.y.z` shape; strictly greater than the current `versionName`;
   `CHANGELOG.md`'s `[Unreleased]` section non-empty. Fail before touching any
   file.
2. Rewrite `CHANGELOG.md` — rename `## [Unreleased]` to
   `## [x.y.z] - YYYY-MM-DD` (UTC) and leave a fresh empty `[Unreleased]`
   above it — and bump `versionCode` (+1) and `versionName`, as **local,
   uncommitted edits**.
3. Build and sign from those edits.
4. Publish the GitHub Release, whose body is the **extracted bold lead-ins**
   of the cut section plus a link back to `CHANGELOG.md`, never the section
   verbatim.
5. **Only after the release is published**, commit and push the bump to
   `main` using `RELEASE_PUSH_TOKEN`.

Step 5 last is the whole point: a signing failure then leaves `main`
untouched and the same version number can simply be re-run. The sibling
project learned this the hard way — an earlier version committed first and a
failed signing step burned a version number with no release behind it.

Refuse to overwrite an existing tag or release.

---

## 3. Documentation

- **`README.md`** — rewrite properly: what the app is, badges (latest release,
  licence, minSdk), a features section per shipped phase, screenshots
  (placeholders if none yet), build instructions, the TheGamesDB API key note,
  and a "not affiliated with Nintendo/Game Freak/The Pokémon Company"
  disclaimer. English.
- **`CHANGELOG.md`** — consolidate the `[Unreleased]` entries accumulated
  across phases into the convention: one top-level bullet per user-facing
  change, leading with a short bold summary, detail after it. The release
  workflow extracts exactly those lead-ins.
- **`LICENSE`** — MIT, same as the sibling projects (confirm with the user
  before committing a licence choice).
- **`docs/release-signing.md`** — §1 above.
- **`CLAUDE.md`** — final pass: every phase ticked, the "known gotchas"
  section updated with anything learned during Phases 0–5, and the
  changelog/release process section matching the workflow that actually
  shipped.
- **`docs/test-plan.md`** — a final full-app regression pass section.

---

## 4. Definition of done

- [ ] `Build APK` produces a signed APK that installs over a previous build.
- [ ] `Release` cuts a version end to end on a test version number.
- [ ] `main` is only bumped after a successful publish.
- [ ] README, CHANGELOG, LICENSE and signing docs are in place.

## 5. Pitfalls

- Never commit the keystore or its passwords.
- `versionCode` must increase on every published build or Android refuses the
  upgrade.
- The changelog rewrite is `awk`/`sed` over exact heading text — keep
  `## [Unreleased]` spelled exactly that way, always.
