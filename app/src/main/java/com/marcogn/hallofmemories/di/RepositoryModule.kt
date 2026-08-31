package com.marcogn.hallofmemories.di

import com.marcogn.hallofmemories.data.repository.HackRepositoryImpl
import com.marcogn.hallofmemories.data.repository.HallOfFameRepositoryImpl
import com.marcogn.hallofmemories.data.repository.PokedexRepositoryImpl
import com.marcogn.hallofmemories.data.repository.PokemonTemplateRepositoryImpl
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import com.marcogn.hallofmemories.domain.repository.PokemonTemplateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHackRepository(impl: HackRepositoryImpl): HackRepository

    @Binds
    @Singleton
    abstract fun bindHallOfFameRepository(impl: HallOfFameRepositoryImpl): HallOfFameRepository

    @Binds
    @Singleton
    abstract fun bindPokemonTemplateRepository(impl: PokemonTemplateRepositoryImpl): PokemonTemplateRepository

    @Binds
    @Singleton
    abstract fun bindPokedexRepository(impl: PokedexRepositoryImpl): PokedexRepository
}
