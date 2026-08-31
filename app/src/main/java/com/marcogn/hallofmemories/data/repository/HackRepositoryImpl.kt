package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.data.local.dao.HackDao
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import com.marcogn.hallofmemories.domain.repository.HackRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HackRepositoryImpl @Inject constructor(
    private val hackDao: HackDao,
) : HackRepository {

    override fun observeAll(): Flow<List<HackWithEntryCount>> =
        hackDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Hack?> =
        hackDao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(hack: Hack) {
        hackDao.upsert(hack.toEntity())
    }

    override suspend fun deleteById(id: String) {
        hackDao.deleteById(id)
    }
}
