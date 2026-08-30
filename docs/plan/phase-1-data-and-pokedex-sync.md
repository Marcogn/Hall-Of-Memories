# Phase 1 — Room schema, PokéAPI sync, sprites

**Goal:** the app owns its complete database schema, downloads the PokéAPI
reference data once (~57 requests, ~2.5 MB), can invalidate and re-download
it, and can turn `(species, generation, shiny)` into a sprite URL.

**Depends on:** Phase 0.
**Read first:** [`reference-pokeapi.md`](reference-pokeapi.md) — it is the
contract for everything in §3 and §4 here, with measured sizes and verified
URLs. Do not re-derive it.

---

## 1. Room schema v1 — all of it, now

Create every table in one schema version, including the ones only used from
Phase 3 onward. Adding tables later means migrations; there is no reason to
pay that here.

```
data/local/HallOfMemoriesDatabase.kt   @Database(version = 1, exportSchema = true)
data/local/Converters.kt               Instant <-> Long (epochMilli), enums <-> String
data/local/entity/HackEntity.kt
data/local/entity/HallOfFameEntryEntity.kt
data/local/entity/PokemonSlotEntity.kt
data/local/entity/PokemonTemplateEntity.kt
data/local/entity/PokedexEntities.kt   PokeSpecies/Move/Nature/Ability/Item/CacheMeta
data/local/entity/HackWithEntries.kt   @Embedded/@Relation projections
data/local/dao/HackDao.kt
data/local/dao/HallOfFameDao.kt
data/local/dao/PokemonTemplateDao.kt
data/local/dao/PokedexDao.kt
di/DatabaseModule.kt                   @Provides the database + every DAO
```

`DATABASE_NAME = "hall_of_memories.db"`. `Room.databaseBuilder(...)` with **no**
`fallbackToDestructiveMigration()`, ever. Keep the generated schema JSON under
`app/schemas/` and commit it — it is what makes future migrations verifiable.

Columns are exactly as listed in [`../spec.md`](../spec.md) §2. Details that
matter:

- Ids for user data are `String` UUIDs (`UUID.randomUUID().toString()`),
  generated in the repository, never by Room. Pokédex cache ids are the
  PokéAPI `Int` ids.
- `HallOfFameEntryEntity.hackId`: `ForeignKey(onDelete = CASCADE)` + `@Index`.
- `PokemonSlotEntity.entryId`: same, plus a **unique index on
  `(entryId, slotIndex)`**.
- `PokemonSlotEntity.sourceTemplateId`: plain nullable column, **no
  ForeignKey** — deleting a template must never cascade into saved teams.
- `Instant` stored as `Long` epoch millis; enums (`GameGeneration`,
  `PokemonGender`) as their `name`.
- Every DAO read returns `Flow<...>`; every multi-table write is a
  `@Transaction suspend fun`.

### DAO surface needed by later phases (write it now)

`HackDao`: `observeAll(): Flow<List<HackWithEntryCount>>`,
`observeById(id): Flow<HackEntity?>`, `upsert`, `deleteById`.
`HallOfFameDao`: `observeByHack(hackId): Flow<List<HallOfFameEntryWithSlots>>`,
`observeById(entryId): Flow<HallOfFameEntryWithSlots?>`, `upsertEntry`,
`upsertSlots`, `deleteEntry`, `countByHack(hackId): Flow<Int>`.
`PokemonTemplateDao`: `observeAll()`, `getById`, `upsert`, `deleteById`.
`PokedexDao`: per cache table — `replaceAll(items)` as
`@Transaction { deleteAll(); insertAll(items) }`, `searchSpecies(query, limit)`,
`searchMoves`, `observeNatures()`, `observeAbilities()`, `searchItems`,
`getById`, plus `getMeta(key)` / `upsertMeta` / `clearAllPokedexData()`.

Autocomplete queries use the normalized column:
`WHERE searchName LIKE :prefix || '%' ORDER BY LENGTH(name), id LIMIT :limit`,
with a second `LIKE '%' || :q || '%'` pass merged in when the prefix pass
returns few rows. Keep the merging in the repository (pure, testable), not in
SQL.

---

## 2. Domain models and repositories

```
domain/model/Hack.kt, HallOfFameEntry.kt, PokemonSlot.kt, PokemonTemplate.kt
domain/model/GameGeneration.kt, PokemonGender.kt, ThemeMode.kt (exists)
domain/model/PokedexModels.kt      PokedexSpecies, PokedexMove, PokedexNature, ...
domain/model/SyncState.kt          Idle | Running(stage, done, total) | Success(at) | Failed(msg)
domain/repository/HackRepository.kt, HallOfFameRepository.kt,
                  PokemonTemplateRepository.kt, PokedexRepository.kt   (interfaces)
data/repository/*Impl.kt + Mappers.kt
di/RepositoryModule.kt             @Binds each interface to its Impl, @Singleton
```

Domain models are pure Kotlin, no Android imports, no Room annotations.
`Mappers.kt` holds entity↔domain extension functions and is unit-tested.

`GameGeneration` carries its sprite variant:

```kotlin
enum class GameGeneration(val spriteVariant: SpriteVariant) {
    RB(SpriteVariant.RED_BLUE), GSC(SpriteVariant.CRYSTAL), RSE(SpriteVariant.EMERALD),
    FRLG(SpriteVariant.FIRERED_LEAFGREEN), DPPT(SpriteVariant.PLATINUM),
    HGSS(SpriteVariant.HEARTGOLD_SOULSILVER), BW(SpriteVariant.BLACK_WHITE),
    XY(SpriteVariant.X_Y), ORAS(SpriteVariant.OMEGARUBY_ALPHASAPPHIRE),
    SM(SpriteVariant.ULTRA_SUN_ULTRA_MOON), USUM(SpriteVariant.ULTRA_SUN_ULTRA_MOON),
    SWSH(SpriteVariant.HOME), SV(SpriteVariant.HOME), OTHER(SpriteVariant.OFFICIAL_ARTWORK);

    /** PokéAPI generation number this game belongs to, for sprite availability checks. */
    val generationNumber: Int
}
```

Its user-facing label is **not** on the enum: add
`ui/common/GameGenerationDisplay.kt` with a `@Composable fun GameGeneration.displayName(): String`
resolving a string resource, the same split ThePatientGamerHelper uses for
`ReviewStatus`.

---

## 3. Sprite resolution — pure, tested

```
domain/sprite/SpriteVariant.kt        enum(path: String?, supportsShiny: Boolean)
domain/sprite/SpriteUrlResolver.kt    pure functions, no Android imports
ui/common/PokemonSprite.kt            Coil composable that walks the candidate list
```

`SpriteVariant` is the verified table in `reference-pokeapi.md` §4 —
transcribe it exactly, including `RED_BLUE.supportsShiny = false` and the
absence of any generation-ix entry.

```kotlin
object SpriteUrlResolver {
    const val SPRITES_BASE =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    fun url(speciesId: Int, variant: SpriteVariant, shiny: Boolean): String

    /**
     * Ordered candidates, most specific first. The UI tries them in order.
     * [speciesGeneration] is PokedexSpecies.generationIntroduced when known:
     * a species introduced after the hack's generation has no sprite in that
     * generation's directory (verified 404), so its variant is skipped
     * before a request is ever made.
     */
    fun candidates(
        speciesId: Int,
        generation: GameGeneration,
        shiny: Boolean,
        alwaysUseLatest: Boolean,
        speciesGeneration: Int?,
    ): List<String>
}
```

Order, per `reference-pokeapi.md` §4 "Fallback chain": generation variant
(shiny only if supported and requested) → same variant non-shiny → `other/home`
→ `other/official-artwork` → flat default. `alwaysUseLatest` starts the list at
`other/home`. The list is never empty and never contains duplicates.

`PokemonSprite(speciesId, generation, shiny, alwaysUseLatest, speciesGeneration, modifier)`
holds `var index by remember(candidates) { mutableIntStateOf(0) }`, renders
`AsyncImage(model = candidates[index], onError = { if (index < candidates.lastIndex) index++ })`,
and falls back to a Pokéball-silhouette placeholder when the list is
exhausted. Coil has no built-in fallback chain — this composable is it.

**Unit tests (`SpriteUrlResolverTest`, plain JVM):** Gen-3 hack + Gen-3
species → emerald path; Gen-3 hack + species 906 → the emerald candidate is
absent and `other/home` is first; shiny in `RB` → non-shiny red-blue URL,
never a `red-blue/shiny/` one; `alwaysUseLatest` ignores the generation
entirely; every returned string starts with `SPRITES_BASE` and ends `.png`;
no duplicates.

---

## 4. The PokéAPI client and sync engine

```
data/pokeapi/PokeApiClient.kt          HttpURLConnection GETs against the mirror
data/pokeapi/PokedexSyncManager.kt     @Singleton, orchestrates the stages
domain/pokeapi/PokeApiParsing.kt       pure parsers over JSON strings (no Android)
domain/pokeapi/SyncStage.kt            enum SPECIES, TYPES, MOVES, NATURES, ABILITIES,
                                       ITEMS, GENERATIONS
domain/pokeapi/PokedexSchema.kt        const POKEDEX_SCHEMA_VERSION = 1
```

### Client

Same shape as ThePatientGamerHelper's `TheGamesDbApiClient`: no Retrofit, no
Ktor. `HttpURLConnection`, `connectTimeout = 10_000`, `readTimeout = 15_000`,
`Accept: application/json`, a realistic desktop `User-Agent`,
`withContext(Dispatchers.IO)`. **Do not set `Accept-Encoding`** — the default
gzip handling is transparent; setting it by hand hands you raw gzip bytes.
One retry per request after 500 ms; a second failure throws.

`suspend fun getJson(path: String): String` is the whole public surface, plus
`suspend fun getJsonBatch(paths: List<String>, concurrency: Int = 6): List<String>`
using a `Semaphore` — 18 type records and 25 natures do not need CoverDex's
batch-of-50 machinery, but they should not be sequential either.

### Parsers — pure, and where the real logic lives

`PokeApiParsing.kt` takes JSON **strings** and returns domain lists. It has no
Android and no network imports, so every parsing rule is unit-testable against
trimmed fixtures:

```kotlin
fun parseIndex(json: String): List<IndexEntry>                       // {name, id}
fun parseTypeDetail(json: String): TypeDetail                        // typeName, speciesSlots, moveIds
fun parseNatureDetail(json: String): PokedexNature
fun parseGenerationDetail(json: String): GenerationDetail            // number, speciesNames
fun extractIdFromUrl(url: String?): Int?                             // "/api/v2/pokemon/6/" -> 6
fun prettify(kebabName: String): String                              // "mr-mime" -> "Mr Mime"
fun searchKey(name: String): String                                  // "mr-mime" -> "mrmime"
```

`Json { ignoreUnknownKeys = true; coerceInputValues = true }`, and nullable
types for anything the mirror can return as an explicit `null`
(`increased_stat` / `decreased_stat` on the five neutral natures — this is the
exact bug class that bit ThePatientGamerHelper twice).

Fixtures live in `app/src/test/resources/pokeapi/` and are **hand-trimmed**
(a couple of entries each). Never commit a real 530 KB record.

### Stages

Each stage: fetch → parse → `replaceAll` in one Room transaction → write its
`PokeCacheMeta` row (`key = stage.name`, `lastSyncedAt`, `schemaVersion =
POKEDEX_SCHEMA_VERSION`, `itemCount`). A stage that throws leaves both its
table and its meta row untouched.

1. **SPECIES** — `pokemon/index.json` → `PokeSpecies(id, name, displayName,
   searchName)`, types and generation left null for now.
2. **TYPES** — `type/{1..18}/index.json`. Build `speciesId → (slot → typeName)`
   from `pokemon[]` and `moveId → typeName` from `moves[]`, then **update**
   the already-inserted species/move rows (`UPDATE ... SET primaryType = ...`),
   so this stage depends on SPECIES and MOVES having run. Order the stage list
   accordingly: `SPECIES, MOVES, TYPES, NATURES, ABILITIES, ITEMS, GENERATIONS`.
3. **MOVES** — `move/index.json` → `PokeMove(id, name, displayName, searchName)`,
   type filled in by TYPES.
4. **NATURES** — index, then the 25 details, concurrency 6.
5. **ABILITIES** — index only. Ability descriptions are not fetched; nothing
   in v1 shows them.
6. **ITEMS** — index only, for held-item suggestions.
7. **GENERATIONS** — `generation/{1..9}/index.json` → species-name → generation
   number, applied as an `UPDATE` on `PokeSpecies` **matched by exact name**.
   Alternate forms (`deoxys-attack`, ids > 10000) do not appear in
   `pokemon_species` and keep `generationIntroduced = null`; that is correct
   and expected — the sprite resolver treats null as "unknown, don't skip",
   and the composable's `onError` chain covers those cases. Do not invent a
   prefix-matching heuristic for them.

### `PokedexSyncManager`

```kotlin
@Singleton
class PokedexSyncManager @Inject constructor(
    private val client: PokeApiClient,
    private val pokedexDao: PokedexDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val state: StateFlow<SyncState>
    fun startIfNeeded()          // no-op when every stage is fresh
    fun forceResync()            // clears cache tables + meta, then runs every stage
    suspend fun isComplete(): Boolean
}
```

- A `Mutex` guarantees one sync at a time; a second call while running is a
  no-op that returns the same `state`.
- The job runs on an **application-scoped** `CoroutineScope`
  (`SupervisorJob() + Dispatchers.IO`, provided by a new
  `di/CoroutinesModule.kt` with an `@ApplicationScope` qualifier) so the sync
  survives navigation and configuration changes. **No WorkManager** — that
  would be a new dependency for a one-shot foreground task.
- **Resumable:** `startIfNeeded()` runs only the stages whose `PokeCacheMeta`
  row is missing or whose `schemaVersion` differs. An interrupted first
  launch resumes where it stopped.
- `state` drives the UI: `Running(stage, done, total)` where `total` is the
  request count of the current stage.
- Failure: `Failed(message)` with the underlying exception's message appended
  and a `Log.w(TAG, ...)`, never a silently generic string — the exact fix
  ThePatientGamerHelper needed in its Phase 7 to make failures diagnosable.

### Where the sync is triggered

`HomeScreen` observes `PokedexRepository.syncState`. On first composition it
calls `startIfNeeded()`. While the pokédex is incomplete the Home screen shows
a non-blocking banner with the stage and progress; the app stays usable (a
hack can be created without species data, only slot editing needs it). On
`Failed`, the banner offers "Retry". The slot editor (Phase 3) shows an
explicit "Pokédex data not downloaded yet" state rather than an empty
autocomplete.

> Deliberate difference from CoverDex, which blocks its whole UI behind a
> `LoadingScreen` during the first fetch. It can afford to; this sync is 2.5 MB
> and the app has plenty to do without it.

---

## 5. Settings — the data section

Add to `SettingsScreen`:

- **Pokédex data**: per-stage last-sync timestamp and row count (read from
  `PokeCacheMeta`), a "Invalidate and re-download" button behind a
  confirmation dialog whose text states explicitly that hacks, Halls of Fame
  and templates are **not** affected, and live progress while running.
- **Always use the latest sprites** toggle, persisted in the same DataStore as
  the theme (`data/settings/SpritePreferences.kt` or an added key on the
  existing preferences class — one DataStore file, `settings_prefs`).

---

## 6. Tests

Plain JVM:

- `PokeApiParsingTest` — index parsing; `extractIdFromUrl` on the real URL
  shape and on garbage; `prettify`/`searchKey`; a type detail producing both
  maps with correct slots; a **neutral nature with explicit null stats**
  (the regression test for the serialization gotcha); malformed JSON throwing
  rather than returning empty.
- `SpriteUrlResolverTest` — as listed in §3.
- `MappersTest` — entity↔domain round-trip for all four user entities,
  including nullable IV/EV columns.
- `PlaytimeParsingTest` — `"42:17"` → 2537 minutes, `"5"` → 300, `""` → null,
  `"forever"` → null, `"100:00"` → 6000. (`domain/model/Playtime.kt`,
  written now because the entity column exists now.)

Robolectric:

- `PokedexDaoTest` — `replaceAll` is atomic and idempotent; `searchSpecies`
  prefix + contains ordering; meta round-trip.
- `HallOfFameDaoTest` — deleting a hack cascades to entries and slots;
  deleting a template does **not** touch `sourceTemplateId` rows.

Not testable here, goes in `docs/test-plan.md`: the real sync against the
network, and sprite rendering.

---

## 7. Definition of done

- [ ] Schema v1 created, `app/schemas/1.json` committed.
- [ ] A fresh install downloads the pokédex and Settings shows seven fresh
      stage rows.
- [ ] Killing the app mid-sync and reopening resumes rather than restarting.
- [ ] "Invalidate and re-download" empties and refills the cache, and a hack
      created beforehand is still there afterwards.
- [ ] All unit tests pass; CI green.

## 8. Pitfalls

- Do not fetch `pokemon/{id}/index.json`. 532 KB each. The whole reason §4 is
  shaped the way it is.
- Do not store sprite URLs in the database. They are derived.
- Do not let a cache wipe reach user tables — `clearAllPokedexData()` must
  name its tables explicitly, never `clearAllTables()`.
- `Json.encodeToString(x)` needs `import kotlinx.serialization.encodeToString`
  or it binds to the wrong overload.
