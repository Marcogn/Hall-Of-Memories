package com.marcogn.hallofmemories.domain.repository

import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import kotlinx.coroutines.flow.Flow

interface PokemonTemplateRepository {
    fun observeAll(): Flow<List<PokemonTemplate>>
    suspend fun getById(id: String): PokemonTemplate?
    suspend fun upsert(template: PokemonTemplate)
    suspend fun deleteById(id: String)
}
