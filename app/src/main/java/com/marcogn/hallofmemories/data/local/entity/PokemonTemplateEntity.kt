package com.marcogn.hallofmemories.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marcogn.hallofmemories.domain.model.PokemonGender
import java.time.Instant

/** Reusable "saved Pokémon" pool, independent of any hack or Hall of Fame entry (spec §2). */
@Entity(tableName = "pokemon_templates")
data class PokemonTemplateEntity(
    @PrimaryKey val id: String,
    val label: String,
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
    val createdAt: Instant,
    val updatedAt: Instant,
)
