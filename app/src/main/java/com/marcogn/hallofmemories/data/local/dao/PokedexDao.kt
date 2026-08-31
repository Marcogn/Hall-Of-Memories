package com.marcogn.hallofmemories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.marcogn.hallofmemories.data.local.entity.PokeAbilityEntity
import com.marcogn.hallofmemories.data.local.entity.PokeCacheMetaEntity
import com.marcogn.hallofmemories.data.local.entity.PokeItemEntity
import com.marcogn.hallofmemories.data.local.entity.PokeMoveEntity
import com.marcogn.hallofmemories.data.local.entity.PokeNatureEntity
import com.marcogn.hallofmemories.data.local.entity.PokeSpeciesEntity
import kotlinx.coroutines.flow.Flow

/**
 * The PokéAPI cache. Every `searchXByPrefix`/`searchXByContains` pair is merged by the repository
 * (`data/repository/PokedexRepositoryImpl.kt`), not here — see
 * `docs/plan/phase-1-data-and-pokedex-sync.md` §1: "Keep the merging in the repository (pure,
 * testable), not in SQL."
 */
@Dao
interface PokedexDao {

    // --- Species ---

    @Query("DELETE FROM pokedex_species")
    suspend fun deleteAllSpecies()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecies(items: List<PokeSpeciesEntity>)

    @Transaction
    suspend fun replaceAllSpecies(items: List<PokeSpeciesEntity>) {
        deleteAllSpecies()
        insertSpecies(items)
    }

    @Query("UPDATE pokedex_species SET primaryType = :primaryType, secondaryType = :secondaryType WHERE id = :id")
    suspend fun updateSpeciesTypes(id: Int, primaryType: String?, secondaryType: String?)

    @Query("UPDATE pokedex_species SET generationIntroduced = :generation WHERE name = :name")
    suspend fun updateSpeciesGenerationByName(name: String, generation: Int)

    @Query("SELECT * FROM pokedex_species WHERE searchName LIKE :prefix || '%' ORDER BY LENGTH(name), id LIMIT :limit")
    suspend fun searchSpeciesByPrefix(prefix: String, limit: Int): List<PokeSpeciesEntity>

    @Query("SELECT * FROM pokedex_species WHERE searchName LIKE '%' || :query || '%' ORDER BY LENGTH(name), id LIMIT :limit")
    suspend fun searchSpeciesByContains(query: String, limit: Int): List<PokeSpeciesEntity>

    @Query("SELECT * FROM pokedex_species WHERE id = :id")
    suspend fun getSpeciesById(id: Int): PokeSpeciesEntity?

    @Query("SELECT COUNT(*) FROM pokedex_species")
    suspend fun countSpecies(): Int

    // --- Moves ---

    @Query("DELETE FROM pokedex_moves")
    suspend fun deleteAllMoves()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoves(items: List<PokeMoveEntity>)

    @Transaction
    suspend fun replaceAllMoves(items: List<PokeMoveEntity>) {
        deleteAllMoves()
        insertMoves(items)
    }

    @Query("UPDATE pokedex_moves SET type = :type WHERE id = :id")
    suspend fun updateMoveType(id: Int, type: String)

    @Query("SELECT * FROM pokedex_moves WHERE searchName LIKE :prefix || '%' ORDER BY LENGTH(name), id LIMIT :limit")
    suspend fun searchMovesByPrefix(prefix: String, limit: Int): List<PokeMoveEntity>

    @Query("SELECT * FROM pokedex_moves WHERE searchName LIKE '%' || :query || '%' ORDER BY LENGTH(name), id LIMIT :limit")
    suspend fun searchMovesByContains(query: String, limit: Int): List<PokeMoveEntity>

    @Query("SELECT COUNT(*) FROM pokedex_moves")
    suspend fun countMoves(): Int

    /** Exact lookup by normalized [searchName] — used only to colour a saved move's type chip; never a dependency of the saved slot itself (see the class doc). */
    @Query("SELECT * FROM pokedex_moves WHERE searchName = :searchName LIMIT 1")
    suspend fun getMoveBySearchName(searchName: String): PokeMoveEntity?

    // --- Natures ---

    @Query("DELETE FROM pokedex_natures")
    suspend fun deleteAllNatures()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNatures(items: List<PokeNatureEntity>)

    @Transaction
    suspend fun replaceAllNatures(items: List<PokeNatureEntity>) {
        deleteAllNatures()
        insertNatures(items)
    }

    @Query("SELECT * FROM pokedex_natures ORDER BY name")
    fun observeNatures(): Flow<List<PokeNatureEntity>>

    @Query("SELECT COUNT(*) FROM pokedex_natures")
    suspend fun countNatures(): Int

    // --- Abilities ---

    @Query("DELETE FROM pokedex_abilities")
    suspend fun deleteAllAbilities()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbilities(items: List<PokeAbilityEntity>)

    @Transaction
    suspend fun replaceAllAbilities(items: List<PokeAbilityEntity>) {
        deleteAllAbilities()
        insertAbilities(items)
    }

    @Query("SELECT * FROM pokedex_abilities ORDER BY name")
    fun observeAbilities(): Flow<List<PokeAbilityEntity>>

    @Query("SELECT COUNT(*) FROM pokedex_abilities")
    suspend fun countAbilities(): Int

    // --- Items ---

    @Query("DELETE FROM pokedex_items")
    suspend fun deleteAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PokeItemEntity>)

    @Transaction
    suspend fun replaceAllItems(items: List<PokeItemEntity>) {
        deleteAllItems()
        insertItems(items)
    }

    @Query("SELECT * FROM pokedex_items WHERE searchName LIKE :prefix || '%' ORDER BY LENGTH(name), id LIMIT :limit")
    suspend fun searchItemsByPrefix(prefix: String, limit: Int): List<PokeItemEntity>

    @Query("SELECT * FROM pokedex_items WHERE searchName LIKE '%' || :query || '%' ORDER BY LENGTH(name), id LIMIT :limit")
    suspend fun searchItemsByContains(query: String, limit: Int): List<PokeItemEntity>

    @Query("SELECT COUNT(*) FROM pokedex_items")
    suspend fun countItems(): Int

    // --- Sync metadata ---

    @Query("SELECT * FROM pokedex_cache_meta WHERE `key` = :key")
    suspend fun getMeta(key: String): PokeCacheMetaEntity?

    @Query("SELECT * FROM pokedex_cache_meta")
    fun observeAllMeta(): Flow<List<PokeCacheMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: PokeCacheMetaEntity)

    @Query("DELETE FROM pokedex_cache_meta")
    suspend fun deleteAllMeta()

    /**
     * Wipes every cache table by name, explicitly — never `clearAllTables()`, which would also
     * take the user's hacks/Halls of Fame/templates with it.
     */
    @Transaction
    suspend fun clearAllPokedexData() {
        deleteAllSpecies()
        deleteAllMoves()
        deleteAllNatures()
        deleteAllAbilities()
        deleteAllItems()
        deleteAllMeta()
    }
}
