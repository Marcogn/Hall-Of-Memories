package com.marcogn.hallofmemories.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "hall_of_fame_entries",
    foreignKeys = [
        ForeignKey(
            entity = HackEntity::class,
            parentColumns = ["id"],
            childColumns = ["hackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("hackId")],
)
data class HallOfFameEntryEntity(
    @PrimaryKey val id: String,
    val hackId: String,
    val playerName: String,
    val playerId: String,
    val playtimeText: String,
    val playtimeMinutes: Int?,
    val screenshotPath: String?,
    val insertedAt: Instant,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
