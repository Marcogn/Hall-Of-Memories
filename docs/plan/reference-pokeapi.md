# Reference — PokéAPI mirror and sprite URLs

Everything in this file was **measured against the live mirror on 2026-08-30**
while planning, not recalled. Sizes are uncompressed bytes as reported by
`curl`; HTTP status codes are real responses. Treat this file as the contract
Phase 1 implements against, and re-measure before changing any of it.

## 1. Data source

```
BASE = https://raw.githubusercontent.com/PokeAPI/api-data/master/data/api/v2
```

The static PokeAPI/api-data mirror, the same source CoverDex uses. No API key,
no documented rate limit, plain files behind GitHub's CDN.

**Gotcha carried over from CoverDex:** the mirror serves *numeric-ID paths
only*, each ending in `/index.json`. `BASE/pokemon/bulbasaur/index.json` is a
404 — every lookup resolves a name to an id through that resource's
`index.json` first.

Every index file has the shape:

```json
{ "count": 1302, "results": [ { "name": "charizard", "url": "/api/v2/pokemon/6/" } ] }
```

The id is the last numeric path segment of `url` (`extractIdFromUrl`, ported
from CoverDex as a pure helper).

## 2. What the sync actually downloads

| Stage | Requests | Path(s) | Measured size | Yields |
|---|---|---|---|---|
| `SPECIES` | 1 | `pokemon/index.json` | 104 KB | 1302 entries: id + kebab name (forms included) |
| `TYPES` | 18 | `type/{1..18}/index.json` | ~28 KB each, ~500 KB | `pokemon[]` (with `slot`) → per-species type 1/2; `moves[]` → per-move type |
| `MOVES` | 1 | `move/index.json` | 70 KB | 937 move names |
| `NATURES` | 1 + 25 | `nature/index.json`, `nature/{id}/index.json` | 1.8 KB + ~2.5 KB each | names + `increased_stat` / `decreased_stat` |
| `ABILITIES` | 1 | `ability/index.json` | 29 KB | 367 ability names |
| `ITEMS` | 1 | `item/index.json` | 167 KB | ~2180 item names (held-item suggestions) |
| `GENERATIONS` | 9 | `generation/{1..9}/index.json` | ~28 KB each, ~250 KB | `pokemon_species[]` → introduction generation per species |
| **Total** | **~57** | | **~2.5 MB** | |

### The decision this table encodes

A per-Pokémon detail record (`pokemon/6/index.json`) is **532 KB** — measured,
not estimated — mostly its `moves` array. Fetching all 1302 of them, the way
CoverDex does in a browser, is roughly **650 MB and 1300 requests** on a phone,
on first launch, before the user can do anything. That is not acceptable for
this app, and it is unnecessary here:

- **Types** are only needed for display; the 18 `type/{id}` records give the
  complete species→types and move→type mappings for 500 KB total.
- **Sprites** need no payload at all — see §4, they are pure URL construction.
- **Species detail** (`pokemon-species/{id}`, 41 KB each) is not fetched:
  nothing in this app needs capture rate, egg groups or flavour text.

If a future feature genuinely needs per-Pokémon detail, fetch it lazily for
the one species being displayed and cache the result — never as a sync stage.

### Response shapes worth writing down

`type/{id}/index.json` (keys: `id`, `name`, `damage_relations`,
`past_damage_relations`, `game_indices`, `generation`, `move_damage_class`,
`names`, `pokemon`, `moves`, `sprites`):

```json
"pokemon": [ { "slot": 1, "pokemon": { "name": "charmander", "url": "/api/v2/pokemon/4/" } } ],
"moves":   [ { "name": "fire-punch", "url": "/api/v2/move/7/" } ]
```

`generation/{id}/index.json` carries `pokemon_species: [{name, url}]`.

`nature/{id}/index.json` carries `increased_stat` / `decreased_stat`, each
either `null` or `{name, url}` — **explicitly null for the five neutral
natures**. See the kotlinx.serialization gotcha in §5.

## 3. Names

Mirror names are kebab-case (`charizard-mega-x`, `fire-punch`). Store both:

- `name` — the raw kebab name, the stable identity.
- `displayName` — `prettify(name)`: split on `-`, capitalize each part, join
  with spaces (ported from CoverDex).
- `searchName` — lowercase, non-alphanumerics stripped, for the autocomplete
  `LIKE` query so that "mrmime", "mr mime" and "mr-mime" all match.

## 4. Sprite URLs — verified table

```
SPRITES = https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon
```

A sprite URL is `SPRITES/<variant path>/[shiny/]<pokemonId>.png`. No sprite
URL is stored in the database — it is derived from `(speciesId, generation,
isShiny)` by a pure function.

| Variant | Path under `SPRITES` | Shiny subdir | Verified |
|---|---|---|---|
| RED_BLUE | `versions/generation-i/red-blue` | **no** | 200 / shiny 404 |
| YELLOW | `versions/generation-i/yellow` | no | 200 |
| GOLD | `versions/generation-ii/gold` | yes | 200 |
| SILVER | `versions/generation-ii/silver` | yes | 200 |
| CRYSTAL | `versions/generation-ii/crystal` | yes | 200 |
| RUBY_SAPPHIRE | `versions/generation-iii/ruby-sapphire` | yes | 200 |
| EMERALD | `versions/generation-iii/emerald` | yes | 200 |
| FIRERED_LEAFGREEN | `versions/generation-iii/firered-leafgreen` | yes | 200 |
| DIAMOND_PEARL | `versions/generation-iv/diamond-pearl` | yes | 200 |
| PLATINUM | `versions/generation-iv/platinum` | yes | 200 |
| HEARTGOLD_SOULSILVER | `versions/generation-iv/heartgold-soulsilver` | yes | 200 |
| BLACK_WHITE | `versions/generation-v/black-white` | yes | 200 |
| X_Y | `versions/generation-vi/x-y` | yes | 200 |
| OMEGARUBY_ALPHASAPPHIRE | `versions/generation-vi/omegaruby-alphasapphire` | yes | 200 |
| ULTRA_SUN_ULTRA_MOON | `versions/generation-vii/ultra-sun-ultra-moon` | yes | 200 |
| HOME | `other/home` | yes | 200 |
| OFFICIAL_ARTWORK | `other/official-artwork` | yes | 200 |
| DEFAULT | *(none — `SPRITES/<id>.png`)* | yes (`shiny/<id>.png`) | 200 |

Also verified to exist but **not used**: `versions/generation-vii/icons` and
`versions/generation-viii/icons` (no shiny variants — 404), and
`other/showdown/<id>.gif` (animated).

**There is no `versions/generation-ix` directory** (404). Generation IX (SV)
therefore maps to `HOME`, not to a pixel set.

Form ids work in the flat directories: `SPRITES/10034.png` → 200.

### Generation → variant

| `GameGeneration` | Variant |
|---|---|
| RB | RED_BLUE |
| GSC | CRYSTAL |
| RSE | EMERALD |
| FRLG | FIRERED_LEAFGREEN |
| DPPT | PLATINUM |
| HGSS | HEARTGOLD_SOULSILVER |
| BW | BLACK_WHITE |
| XY | X_Y |
| ORAS | OMEGARUBY_ALPHASAPPHIRE |
| SM, USUM | ULTRA_SUN_ULTRA_MOON |
| SWSH, SV | HOME |
| OTHER | OFFICIAL_ARTWORK |

### Fallback chain

A Gen-3 hack whose team contains a Gen-9 species has no Gen-3 sprite —
`versions/generation-iii/emerald/906.png` is a **verified 404**, while
`other/home/906.png` is a 200. Shiny in a generation with no shiny sprites
(Gen I) is the same situation.

`resolveSpriteCandidates(...)` therefore returns an **ordered list**, and the
UI walks it:

1. The generation variant (shiny subdir only if that variant supports shiny) —
   skipped entirely when `PokeSpecies.generationIntroduced` is known and is
   later than the hack's generation, which removes most 404s before a request
   is ever made.
2. The generation variant, non-shiny, when step 1 was the shiny form.
3. `other/home` (shiny subdir if shiny).
4. `other/official-artwork` (shiny subdir if shiny).
5. `SPRITES/<id>.png` (flat default, shiny subdir if shiny).

With `alwaysUseLatestSprites` on, the list starts at step 3.

Coil has no built-in "try the next URL on 404", so the candidate list is
consumed by a small stateful composable that advances an index in
`onError` — the resolver itself stays pure and unit-tested.

## 5. Client gotchas

- **`HttpURLConnection` and gzip.** Left alone, it sends `Accept-Encoding:
  gzip` and transparently decompresses. Set that header by hand and you get
  raw gzip bytes and must inflate them yourself. Do not set it.
- **kotlinx.serialization and explicit `null`.** A default value covers a
  *missing* key, never an explicit `"field": null`. `increased_stat` on a
  neutral nature is exactly that case. Declare nullable types and configure
  `Json { ignoreUnknownKeys = true; coerceInputValues = true }`.
- **`Json.encodeToString(value)`** without
  `import kotlinx.serialization.encodeToString` binds to the wrong overload
  and fails with a misleading type error. Import it explicitly.
- **Partial writes.** A stage either commits fully inside one Room
  transaction or writes nothing. A half-written cache that reports itself as
  synced is worse than no cache.
- **Cache schema version.** `POKEDEX_SCHEMA_VERSION` is compared against every
  `PokeCacheMeta` row; a mismatch means "absent", triggering a silent
  re-sync — never a crash on a missing column's worth of data. Bump it
  whenever a cache table's shape changes.
