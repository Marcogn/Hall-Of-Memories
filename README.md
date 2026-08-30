# Hall of Memories

A native Android app for cataloguing the Hall of Fame of your playthroughs —
official games and ROM hacks alike. Every finished run becomes a permanent
record: the six-strong team with its nicknames, natures, abilities, held
items, IVs, EVs and moves, the trainer behind it, the playtime, and the
screenshot of the moment it happened.

Offline-first, single user, no account, no telemetry.

> **Status: planning.** No application code exists yet. The build is planned
> as seven phases in [`docs/plan/`](docs/plan/README.md); the specification
> that governs it is [`docs/spec.md`](docs/spec.md).

## What it does (v1, planned)

- **Hacks.** Catalogue each game or ROM hack you play, with its generation,
  its box art and its logo — fetched from TheGamesDB when the base game is
  catalogued there, picked from your gallery when it is not.
- **Halls of Fame.** Under each hack, one record per completed run: six
  Pokémon slots in full detail, the trainer's name and ID, the playtime, a
  screenshot, and the date you finished.
- **Reusable Pokémon.** Save any configured Pokémon as a template and drop it
  into a run in a different hack.
- **Generation-accurate sprites.** A Gen-3 hack shows Gen-3 sprites, shinies
  included, with a graceful fall back to modern artwork where a sprite never
  existed.
- **Your data stays yours.** A local zip backup you export and import
  yourself. Google Drive backup is planned for v2.

## Built with

Kotlin, Jetpack Compose, Material 3, Room, Hilt, Coil, DataStore, PokéAPI
(via the static PokeAPI/api-data mirror) and TheGamesDB.

## Documentation

| | |
|---|---|
| [`docs/spec.md`](docs/spec.md) | Functional specification |
| [`docs/plan/README.md`](docs/plan/README.md) | Phase-by-phase implementation plan |
| [`docs/plan/reference-pokeapi.md`](docs/plan/reference-pokeapi.md) | Measured PokéAPI + sprite reference |
| [`docs/implementation-decisions.md`](docs/implementation-decisions.md) | Why things are the way they are |
| [`docs/test-plan.md`](docs/test-plan.md) | Manual, on-device verification |
| [`CLAUDE.md`](CLAUDE.md) | Guide for AI coding agents |

## Disclaimer

Not affiliated with, endorsed by, or associated with Nintendo, Game Freak or
The Pokémon Company. Pokémon data is fetched at runtime from
[PokéAPI](https://pokeapi.co/); artwork for games is fetched from
[TheGamesDB](https://thegamesdb.net/). No game assets are bundled with this
app.
