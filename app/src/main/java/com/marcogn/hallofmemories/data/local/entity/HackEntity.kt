package com.marcogn.hallofmemories.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marcogn.hallofmemories.domain.model.GameGeneration
import java.time.Instant

@Entity(tableName = "hacks")
data class HackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val generation: GameGeneration,
    val baseGameTitle: String?,
    val boxArtPath: String?,
    val boxArtUrl: String?,
    val logoPath: String?,
    val logoUrl: String?,
    val theGamesDbId: Long?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
