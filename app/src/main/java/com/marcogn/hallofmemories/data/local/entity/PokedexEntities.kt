package com.marcogn.hallofmemories.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * The PokéAPI cache tables. All five below are wiped and rebuilt together by
 * `PokedexDao.clearAllPokedexData()` and `PokedexSyncManager.forceResync()` — never through
 * `clearAllTables()`, which would take the user's Halls of Fame with it (see CLAUDE.md).
 */
@Entity(tableName = "pokedex_species", indices = [Index("searchName")])
data class PokeSpeciesEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
    val generationIntroduced: Int?,
    val primaryType: String?,
    val secondaryType: String?,
)

@Entity(tableName = "pokedex_moves", indices = [Index("searchName")])
data class PokeMoveEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
    val type: String?,
)

@Entity(tableName = "pokedex_natures", indices = [Index("searchName")])
data class PokeNatureEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
    val increasedStat: String?,
    val decreasedStat: String?,
)

@Entity(tableName = "pokedex_abilities", indices = [Index("searchName")])
data class PokeAbilityEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
)

@Entity(tableName = "pokedex_items", indices = [Index("searchName")])
data class PokeItemEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val displayName: String,
    val searchName: String,
)

/** One row per [com.marcogn.hallofmemories.domain.pokeapi.SyncStage], keyed by its [SyncStage.name]. */
@Entity(tableName = "pokedex_cache_meta")
data class PokeCacheMetaEntity(
    @PrimaryKey val key: String,
    val lastSyncedAt: Instant,
    val schemaVersion: Int,
    val itemCount: Int,
)
