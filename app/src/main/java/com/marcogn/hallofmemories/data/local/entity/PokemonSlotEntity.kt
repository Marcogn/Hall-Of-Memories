package com.marcogn.hallofmemories.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.marcogn.hallofmemories.domain.model.PokemonGender

/**
 * `speciesId == null` means the slot is empty; the row still exists — six rows always exist per
 * entry (spec §2), which is what the unique index on `(entryId, slotIndex)` enforces.
 * [sourceTemplateId] is deliberately a plain column, not a [ForeignKey]: deleting a template must
 * never cascade into, or block deleting, a saved Hall of Fame.
 */
@Entity(
    tableName = "pokemon_slots",
    foreignKeys = [
        ForeignKey(
            entity = HallOfFameEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("entryId"),
        Index(value = ["entryId", "slotIndex"], unique = true),
    ],
)
data class PokemonSlotEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val slotIndex: Int,
    val speciesId: Int?,
    val speciesName: String?,
    val nickname: String?,
    val gender: PokemonGender,
    val level: Int?,
    val nature: String?,
    val ability: String?,
    val isShiny: Boolean,
    val heldItem: String?,
    val ivHp: Int?,
    val ivAtk: Int?,
    val ivDef: Int?,
    val ivSpAtk: Int?,
    val ivSpDef: Int?,
    val ivSpe: Int?,
    val evHp: Int?,
    val evAtk: Int?,
    val evDef: Int?,
    val evSpAtk: Int?,
    val evSpDef: Int?,
    val evSpe: Int?,
    val move1: String?,
    val move2: String?,
    val move3: String?,
    val move4: String?,
    val sourceTemplateId: String?,
)
