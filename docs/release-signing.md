# Release signing

A signed release build needs one keystore, generated **once** and kept
forever. Regenerating it later changes the app's signature and makes it
impossible to upgrade any device that already has a build installed from
the previous one — Android refuses the install as a different app.

## 1. Generate the keystore

```bash
keytool -genkeypair -v \
  -keystore hall-of-memories-release.keystore \
  -alias hall-of-memories \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

`keytool` prompts for a store password, a key password (can be the same as
the store password), and the certificate's distinguished-name fields (name,
org, city, etc. — these are public and cosmetic, put anything reasonable).
Note the passwords and the alias somewhere durable; they're needed for
every future signed build.

## 2. Store it safely

- Keep `hall-of-memories-release.keystore` in a password manager or an
  encrypted backup. **Never commit it to the repository** —
  `*.keystore`/`*.jks` are already in `.gitignore`, but that only stops an
  accidental `git add`, not a deliberate one.
- Losing this file (or forgetting its passwords) means every future
  release is signed with a *different* key, which Android treats as a
  different app — no installed copy can ever be upgraded again. There is
  no recovery from this short of publishing under a new `applicationId`.

## 3. Produce the GitHub secrets

The workflows (`build-apk.yml`, `release.yml`) read the keystore and its
credentials from repository secrets, never from a file in the repo:

```bash
base64 -w0 hall-of-memories-release.keystore
```

Copy that output into a repository secret named `RELEASE_KEYSTORE_BASE64`.
Add these secrets under the repo's **Settings → Secrets and variables →
Actions**:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | the base64 output above |
| `RELEASE_KEYSTORE_PASSWORD` | the store password from step 1 |
| `RELEASE_KEY_ALIAS` | `hall-of-memories` (or whatever alias was used) |
| `RELEASE_KEY_PASSWORD` | the key password from step 1 |
| `RELEASE_PUSH_TOKEN` | a fine-grained PAT scoped to this repo, Contents: Read and write — used only by `release.yml`'s final version-bump commit |

`app/build.gradle.kts`'s `signingConfigs { create("release") { ... } }`
block (written in Phase 0) reads `RELEASE_KEYSTORE_PATH`/
`RELEASE_KEYSTORE_PASSWORD`/`RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD` as
environment variables — the workflows decode the base64 secret into a
temporary file at `$RUNNER_TEMP/release.keystore` and pass its path as
`RELEASE_KEYSTORE_PATH`. A local `./gradlew assembleRelease` with none of
these set still succeeds — the release build type is simply left unsigned
in that case, Android's own default.

## 4. Read the SHA-1 back out

```bash
./gradlew signingReport
```

Useful for verifying the keystore is the one actually in use, and is the
starting point for registering this app with any Google API that checks a
signing certificate (none are used in v1, but any future integration —
e.g. if Google Drive backup ever needs `AuthorizationClient`-style
authorization — would need this exact value). `build-apk.yml` and
`release.yml` both print it after every signed build via the same
`signingReport` task, run against the decoded release keystore rather than
the default debug one.
