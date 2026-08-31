package com.marcogn.hallofmemories.domain.repository

import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import kotlinx.coroutines.flow.Flow

interface HackRepository {
    fun observeAll(): Flow<List<HackWithEntryCount>>
    fun observeById(id: String): Flow<Hack?>
    suspend fun upsert(hack: Hack)
    suspend fun deleteById(id: String)
}
