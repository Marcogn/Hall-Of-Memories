package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.data.local.dao.PokemonTemplateDao
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import com.marcogn.hallofmemories.domain.repository.PokemonTemplateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PokemonTemplateRepositoryImpl @Inject constructor(
    private val templateDao: PokemonTemplateDao,
) : PokemonTemplateRepository {

    override fun observeAll(): Flow<List<PokemonTemplate>> =
        templateDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): PokemonTemplate? = templateDao.getById(id)?.toDomain()

    override suspend fun upsert(template: PokemonTemplate) {
        templateDao.upsert(template.toEntity())
    }

    override suspend fun deleteById(id: String) {
        templateDao.deleteById(id)
    }
}
