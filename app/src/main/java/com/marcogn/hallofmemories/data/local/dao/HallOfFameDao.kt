package com.marcogn.hallofmemories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryWithSlotsRelation
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HallOfFameDao {

    @Transaction
    @Query("SELECT * FROM hall_of_fame_entries WHERE hackId = :hackId ORDER BY insertedAt DESC")
    fun observeByHack(hackId: String): Flow<List<HallOfFameEntryWithSlotsRelation>>

    @Transaction
    @Query("SELECT * FROM hall_of_fame_entries WHERE id = :entryId")
    fun observeById(entryId: String): Flow<HallOfFameEntryWithSlotsRelation?>

    @Query("SELECT COUNT(*) FROM hall_of_fame_entries WHERE hackId = :hackId")
    fun countByHack(hackId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: HallOfFameEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<PokemonSlotEntity>)

    @Query("DELETE FROM pokemon_slots WHERE entryId = :entryId")
    suspend fun deleteSlotsByEntry(entryId: String)

    /**
     * Upserts the entry, then replaces its slot rows (delete-then-insert keyed by
     * `(entryId, slotIndex)`) — always exactly six after this returns, empty ones included
     * (spec §2). One transaction: either both succeed or neither is visible.
     */
    @Transaction
    suspend fun saveEntryWithSlots(entry: HallOfFameEntryEntity, slots: List<PokemonSlotEntity>) {
        upsertEntry(entry)
        deleteSlotsByEntry(entry.id)
        insertSlots(slots)
    }

    @Query("DELETE FROM hall_of_fame_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)
}
