package com.marcogn.hallofmemories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hack: HackEntity)

    @Query("DELETE FROM hacks WHERE id = :id")
    suspend fun deleteById(id: String)
}
