package com.marcogn.hallofmemories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonTemplateEntity

/**
 * Restore-only: the full-replace transaction a local backup import needs, spanning tables that
 * otherwise each have their own DAO. Deleting every hack cascades to its entries and slots
 * (`ON DELETE CASCADE`, see `PokemonSlotEntity`/`HallOfFameEntryEntity`); templates have no FK to
 * anything and are cleared separately. Never touches `pokedex_*` tables — the cache is never part
 * of a backup (spec §5).
 */
@Dao
interface BackupDao {

    @Query("DELETE FROM hacks")
    suspend fun deleteAllHacks()

    @Query("DELETE FROM pokemon_templates")
    suspend fun deleteAllTemplates()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHacks(hacks: List<HackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<HallOfFameEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<PokemonSlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<PokemonTemplateEntity>)

    /** Single-user, no merge: every existing hack/entry/slot/template is gone before the restored ones land, all inside one transaction. */
    @Transaction
    suspend fun replaceAll(
        hacks: List<HackEntity>,
        entries: List<HallOfFameEntryEntity>,
        slots: List<PokemonSlotEntity>,
        templates: List<PokemonTemplateEntity>,
    ) {
        deleteAllHacks()
        deleteAllTemplates()
        insertHacks(hacks)
        insertEntries(entries)
        insertSlots(slots)
        insertTemplates(templates)
    }
}
