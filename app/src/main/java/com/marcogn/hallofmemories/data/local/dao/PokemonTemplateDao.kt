package com.marcogn.hallofmemories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marcogn.hallofmemories.data.local.entity.PokemonTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonTemplateDao {

    @Query("SELECT * FROM pokemon_templates ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PokemonTemplateEntity>>

    @Query("SELECT * FROM pokemon_templates WHERE id = :id")
    suspend fun getById(id: String): PokemonTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: PokemonTemplateEntity)

    /** No cascade to worry about: `PokemonSlotEntity.sourceTemplateId` carries no foreign key. */
    @Query("DELETE FROM pokemon_templates WHERE id = :id")
    suspend fun deleteById(id: String)
}
