# REVIEW.md

What a review of a Hall of Memories change must look at. Rules come from
`CLAUDE.md`, `docs/spec.md` and `docs/implementation-decisions.md`; each was
checked against the code when this file was written.

## What Important means here

A bug, lost or altered user data (hacks, Halls of Fame, templates, images,
backups), a broken migration, a release that can't install over the previous
one, or a break of a rule below. Everything else is a Nit.

## Always check

- `domain/` stays pure Kotlin: no `android.*` import.
- Room migrations are additive and numbered; never
  `fallbackToDestructiveMigration()`.
- Never `clearAllTables()`: cache invalidation names the PokéAPI cache tables
  (`PokedexDao`), or it takes the user's Halls of Fame with it.
- No `@Insert(onConflict = REPLACE)` as the edit path of a row with
  `ON DELETE CASCADE` children (hack → entries → slots): `HackDao.upsert()`
  dispatches to `@Insert`/`@Update`; a REPLACE elsewhere must rewrite every
  child in the same transaction.
- Always six slots per Hall of Fame; slots hold denormalized snapshots
  (species, nature, ability, item, moves), never references into the cache.
- `sourceTemplateId` has no foreign key.
- Sprite URLs are derived (`domain/sprite`), never stored.
- No legality validation; only the range checks of `domain/validation`.
- Backups never contain the PokéAPI cache; restore is a full replace in one
  transaction, ids and timestamps preserved.
- No hardcoded user-visible strings; every new key in both
  `values/strings.xml` (Italian) and `values-en/strings.xml`; enum labels via
  `displayName()` in `ui/common`, not on the enum.
- `MainActivity` stays an `AppCompatActivity`; screens with custom back logic
  have a `BackHandler`.
- `HttpURLConnection`: no hand-set `Accept-Encoding`; TheGamesDB keeps the
  desktop browser User-Agent. Never sync per-Pokémon PokéAPI records.
- Robolectric test classes carry `@Config(sdk = [26])`; test names are ASCII.
- No new dependency without an explicit request; nothing out of scope
  (`CLAUDE.md`, "What NOT to do").
- A user-visible change has a `CHANGELOG.md` `[Unreleased]` entry
  (`- **Summary.** detail`).

## Verification bar

Findings name `file:line` and a concrete path from a real caller or input to
the failure.

## Do not report

Style already consistent with the surrounding code, formatting, generated
Room code, `docs/plan/` historical text, missing tests for pure UI layout.

## Shape

Important first, then Nits; one line each with `file:line`, the rule or bug,
and the fix in a sentence.
