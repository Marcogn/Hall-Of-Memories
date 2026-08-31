package com.marcogn.hallofmemories.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marcogn.hallofmemories.data.local.dao.HackDao
import com.marcogn.hallofmemories.data.local.dao.HallOfFameDao
import com.marcogn.hallofmemories.data.local.dao.PokedexDao
import com.marcogn.hallofmemories.data.local.dao.PokemonTemplateDao
import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.PokeAbilityEntity
import com.marcogn.hallofmemories.data.local.entity.PokeCacheMetaEntity
import com.marcogn.hallofmemories.data.local.entity.PokeItemEntity
import com.marcogn.hallofmemories.data.local.entity.PokeMoveEntity
import com.marcogn.hallofmemories.data.local.entity.PokeNatureEntity
import com.marcogn.hallofmemories.data.local.entity.PokeSpeciesEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonTemplateEntity

@Database(
    entities = [
        HackEntity::class,
        HallOfFameEntryEntity::class,
        PokemonSlotEntity::class,
        PokemonTemplateEntity::class,
        PokeSpeciesEntity::class,
        PokeMoveEntity::class,
        PokeNatureEntity::class,
        PokeAbilityEntity::class,
        PokeItemEntity::class,
        PokeCacheMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HallOfMemoriesDatabase : RoomDatabase() {
    abstract fun hackDao(): HackDao
    abstract fun hallOfFameDao(): HallOfFameDao
    abstract fun pokemonTemplateDao(): PokemonTemplateDao
    abstract fun pokedexDao(): PokedexDao

    companion object {
        const val DATABASE_NAME = "hall_of_memories.db"
    }
}
