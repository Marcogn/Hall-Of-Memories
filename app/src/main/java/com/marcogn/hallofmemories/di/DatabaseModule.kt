package com.marcogn.hallofmemories.di

import android.content.Context
import androidx.room.Room
import com.marcogn.hallofmemories.data.local.HallOfMemoriesDatabase
import com.marcogn.hallofmemories.data.local.dao.HackDao
import com.marcogn.hallofmemories.data.local.dao.HallOfFameDao
import com.marcogn.hallofmemories.data.local.dao.PokedexDao
import com.marcogn.hallofmemories.data.local.dao.PokemonTemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // No fallbackToDestructiveMigration(), ever — the app holds data that cannot be re-created.
    // Schema v1 is the first version; a future schema change adds a numbered MIGRATION_x_y here.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HallOfMemoriesDatabase =
        Room.databaseBuilder(
            context,
            HallOfMemoriesDatabase::class.java,
            HallOfMemoriesDatabase.DATABASE_NAME,
        ).build()

    @Provides
    fun provideHackDao(database: HallOfMemoriesDatabase): HackDao = database.hackDao()

    @Provides
    fun provideHallOfFameDao(database: HallOfMemoriesDatabase): HallOfFameDao = database.hallOfFameDao()

    @Provides
    fun providePokemonTemplateDao(database: HallOfMemoriesDatabase): PokemonTemplateDao =
        database.pokemonTemplateDao()

    @Provides
    fun providePokedexDao(database: HallOfMemoriesDatabase): PokedexDao = database.pokedexDao()
}
