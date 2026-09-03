package com.marcogn.hallofmemories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HackWithEntryCountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface HackDao {

    @Query(
        """
        SELECT hacks.*, COUNT(hall_of_fame_entries.id) AS entryCount
        FROM hacks
        LEFT JOIN hall_of_fame_entries ON hall_of_fame_entries.hackId = hacks.id
        GROUP BY hacks.id
        ORDER BY hacks.createdAt DESC
        """,
    )
    fun observeAll(): Flow<List<HackWithEntryCountRow>>

    @Query("SELECT * FROM hacks WHERE id = :id")
    fun observeById(id: String): Flow<HackEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM hacks WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(hack: HackEntity)

    @Update
    suspend fun update(hack: HackEntity)

    /**
     * A real `INSERT ... OR REPLACE` here would delete-then-reinsert an existing row, which
     * cascades through `hall_of_fame_entries`'s `ON DELETE CASCADE` and wipes every Hall of Fame
     * entry (and their slots) under this hack on every edit — a real bug found on-device. `update`
     * never deletes the row, so no cascade fires.
     */
    @Transaction
    suspend fun upsert(hack: HackEntity) {
        if (exists(hack.id)) update(hack) else insert(hack)
    }

    @Query("DELETE FROM hacks WHERE id = :id")
    suspend fun deleteById(id: String)
}
