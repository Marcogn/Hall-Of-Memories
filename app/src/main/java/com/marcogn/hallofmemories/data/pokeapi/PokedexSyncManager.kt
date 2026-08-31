package com.marcogn.hallofmemories.data.pokeapi

import android.util.Log
import com.marcogn.hallofmemories.data.local.dao.PokedexDao
import com.marcogn.hallofmemories.data.local.entity.PokeAbilityEntity
import com.marcogn.hallofmemories.data.local.entity.PokeCacheMetaEntity
import com.marcogn.hallofmemories.data.local.entity.PokeItemEntity
import com.marcogn.hallofmemories.data.local.entity.PokeMoveEntity
import com.marcogn.hallofmemories.data.local.entity.PokeNatureEntity
import com.marcogn.hallofmemories.data.local.entity.PokeSpeciesEntity
import com.marcogn.hallofmemories.di.ApplicationScope
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.domain.pokeapi.POKEDEX_SCHEMA_VERSION
import com.marcogn.hallofmemories.domain.pokeapi.SyncStage
import com.marcogn.hallofmemories.domain.pokeapi.parseGenerationDetail
import com.marcogn.hallofmemories.domain.pokeapi.parseIndex
import com.marcogn.hallofmemories.domain.pokeapi.parseNatureDetail
import com.marcogn.hallofmemories.domain.pokeapi.parseTypeDetail
import com.marcogn.hallofmemories.domain.pokeapi.prettify
import com.marcogn.hallofmemories.domain.pokeapi.searchKey
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val TAG = "PokedexSyncManager"
private const val TYPE_FETCH_CONCURRENCY = 6

/**
 * Orchestrates the PokéAPI sync (see `docs/plan/reference-pokeapi.md` for the exact stages and
 * their measured request counts). Runs on an application-scoped coroutine so it survives
 * navigation and configuration changes — no WorkManager, which would be a new dependency for a
 * one-shot foreground task (see `docs/implementation-decisions.md`).
 *
 * [SyncStage.TYPES] updates rows [SyncStage.SPECIES]/[SyncStage.MOVES] must already have
 * inserted, so those two run first — see the `SyncStage` enum's declared order, which this class
 * follows as-is (`SyncStage.entries`).
 */
@Singleton
class PokedexSyncManager @Inject constructor(
    private val client: PokeApiClient,
    private val pokedexDao: PokedexDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    /** No-op (returns immediately) when a sync is already running or every stage is fresh. */
    fun startIfNeeded() {
        scope.launch { runSync(forceAll = false) }
    }

    /** Wipes every pokédex cache table and meta row, then re-runs every stage. */
    fun forceResync() {
        scope.launch { runSync(forceAll = true) }
    }

    suspend fun isComplete(): Boolean = SyncStage.entries.all { isStageFresh(it) }

    private suspend fun isStageFresh(stage: SyncStage): Boolean {
        val meta = pokedexDao.getMeta(stage.name) ?: return false
        return meta.schemaVersion == POKEDEX_SCHEMA_VERSION
    }

    private suspend fun runSync(forceAll: Boolean) {
        if (!mutex.tryLock()) return
        try {
            if (forceAll) {
                pokedexDao.clearAllPokedexData()
            }
            val stagesToRun = SyncStage.entries.filter { forceAll || !isStageFresh(it) }
            if (stagesToRun.isEmpty()) {
                _state.value = SyncState.Success(Instant.now())
                return
            }
            for (stage in stagesToRun) {
                _state.value = SyncState.Running(stage, done = 0, total = stage.requestCount)
                val itemCount = runStage(stage)
                pokedexDao.upsertMeta(
                    PokeCacheMetaEntity(
                        key = stage.name,
                        lastSyncedAt = Instant.now(),
                        schemaVersion = POKEDEX_SCHEMA_VERSION,
                        itemCount = itemCount,
                    ),
                )
            }
            _state.value = SyncState.Success(Instant.now())
        } catch (e: Exception) {
            // A generic message here is exactly the mistake ThePatientGamerHelper's TheGamesDB
            // search made and had to fix later (see CLAUDE.md) — the real cause goes in the state.
            Log.w(TAG, "Pokédex sync failed at stage", e)
            _state.value = SyncState.Failed(e.message ?: e.toString())
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun runStage(stage: SyncStage): Int {
        val onProgress: (Int) -> Unit = { done -> _state.value = SyncState.Running(stage, done, stage.requestCount) }
        return when (stage) {
            SyncStage.SPECIES -> syncSpecies(onProgress)
            SyncStage.MOVES -> syncMoves(onProgress)
            SyncStage.TYPES -> syncTypes(onProgress)
            SyncStage.NATURES -> syncNatures(onProgress)
            SyncStage.ABILITIES -> syncAbilities(onProgress)
            SyncStage.ITEMS -> syncItems(onProgress)
            SyncStage.GENERATIONS -> syncGenerations(onProgress)
        }
    }

    private suspend fun syncSpecies(onProgress: (Int) -> Unit): Int {
        val entries = parseIndex(client.getJson("pokemon/index.json"))
        onProgress(1)
        val entities = entries.map { entry ->
            PokeSpeciesEntity(
                id = entry.id,
                name = entry.name,
                displayName = prettify(entry.name),
                searchName = searchKey(entry.name),
                generationIntroduced = null,
                primaryType = null,
                secondaryType = null,
            )
        }
        pokedexDao.replaceAllSpecies(entities)
        return entities.size
    }

    private suspend fun syncMoves(onProgress: (Int) -> Unit): Int {
        val entries = parseIndex(client.getJson("move/index.json"))
        onProgress(1)
        val entities = entries.map { entry ->
            PokeMoveEntity(
                id = entry.id,
                name = entry.name,
                displayName = prettify(entry.name),
                searchName = searchKey(entry.name),
                type = null,
            )
        }
        pokedexDao.replaceAllMoves(entities)
        return entities.size
    }

    private suspend fun syncTypes(onProgress: (Int) -> Unit): Int {
        val paths = (1..18).map { "type/$it/index.json" }
        val details = fetchWithProgress(paths, TYPE_FETCH_CONCURRENCY, onProgress).map { parseTypeDetail(it) }

        // A dual-type species appears in two different type/{id} responses (once per slot), so
        // the two slots are merged across details, not read from a single response.
        val speciesTypes = mutableMapOf<Int, Pair<String?, String?>>()
        val moveTypes = mutableMapOf<Int, String>()
        for (detail in details) {
            for ((speciesId, slot) in detail.speciesSlots) {
                val current = speciesTypes.getOrDefault(speciesId, null to null)
                speciesTypes[speciesId] = when (slot) {
                    1 -> detail.typeName to current.second
                    2 -> current.first to detail.typeName
                    else -> current
                }
            }
            for (moveId in detail.moveIds) {
                moveTypes[moveId] = detail.typeName
            }
        }

        speciesTypes.forEach { (speciesId, types) -> pokedexDao.updateSpeciesTypes(speciesId, types.first, types.second) }
        moveTypes.forEach { (moveId, type) -> pokedexDao.updateMoveType(moveId, type) }
        return speciesTypes.size
    }

    private suspend fun syncNatures(onProgress: (Int) -> Unit): Int {
        val indexEntries = parseIndex(client.getJson("nature/index.json"))
        onProgress(1)
        val detailPaths = indexEntries.map { "nature/${it.id}/index.json" }
        val natures = fetchWithProgress(detailPaths, TYPE_FETCH_CONCURRENCY) { done -> onProgress(1 + done) }
            .map { parseNatureDetail(it) }
        val entities = natures.map { nature ->
            PokeNatureEntity(
                id = nature.id,
                name = nature.name,
                displayName = nature.displayName,
                searchName = nature.searchName,
                increasedStat = nature.increasedStat,
                decreasedStat = nature.decreasedStat,
            )
        }
        pokedexDao.replaceAllNatures(entities)
        return entities.size
    }

    private suspend fun syncAbilities(onProgress: (Int) -> Unit): Int {
        val entries = parseIndex(client.getJson("ability/index.json"))
        onProgress(1)
        val entities = entries.map { entry ->
            PokeAbilityEntity(id = entry.id, name = entry.name, displayName = prettify(entry.name), searchName = searchKey(entry.name))
        }
        pokedexDao.replaceAllAbilities(entities)
        return entities.size
    }

    private suspend fun syncItems(onProgress: (Int) -> Unit): Int {
        val entries = parseIndex(client.getJson("item/index.json"))
        onProgress(1)
        val entities = entries.map { entry ->
            PokeItemEntity(id = entry.id, name = entry.name, displayName = prettify(entry.name), searchName = searchKey(entry.name))
        }
        pokedexDao.replaceAllItems(entities)
        return entities.size
    }

    private suspend fun syncGenerations(onProgress: (Int) -> Unit): Int {
        val paths = (1..9).map { "generation/$it/index.json" }
        val details = fetchWithProgress(paths, TYPE_FETCH_CONCURRENCY, onProgress).map { parseGenerationDetail(it) }
        var updated = 0
        for (detail in details) {
            for (speciesName in detail.speciesNames) {
                pokedexDao.updateSpeciesGenerationByName(speciesName, detail.number)
                updated++
            }
        }
        return updated
    }

    private suspend fun fetchWithProgress(
        paths: List<String>,
        concurrency: Int,
        onProgress: (Int) -> Unit,
    ): List<String> = coroutineScope {
        val semaphore = Semaphore(concurrency)
        val completed = AtomicInteger(0)
        paths.map { path ->
            async {
                semaphore.withPermit { client.getJson(path) }.also { onProgress(completed.incrementAndGet()) }
            }
        }.awaitAll()
    }
}
