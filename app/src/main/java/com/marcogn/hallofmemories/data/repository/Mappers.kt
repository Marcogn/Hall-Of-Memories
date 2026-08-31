package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HackWithEntryCountRow
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryWithSlotsRelation
import com.marcogn.hallofmemories.data.local.entity.PokeAbilityEntity
import com.marcogn.hallofmemories.data.local.entity.PokeItemEntity
import com.marcogn.hallofmemories.data.local.entity.PokeMoveEntity
import com.marcogn.hallofmemories.data.local.entity.PokeNatureEntity
import com.marcogn.hallofmemories.data.local.entity.PokeSpeciesEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonTemplateEntity
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HackWithEntryCount
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokedexAbility
import com.marcogn.hallofmemories.domain.model.PokedexItem
import com.marcogn.hallofmemories.domain.model.PokedexMove
import com.marcogn.hallofmemories.domain.model.PokedexNature
import com.marcogn.hallofmemories.domain.model.PokedexSpecies
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.domain.model.PokemonTemplate

// --- Hack ---

fun HackEntity.toDomain(): Hack = Hack(
    id = id,
    name = name,
    generation = generation,
    baseGameTitle = baseGameTitle,
    boxArtPath = boxArtPath,
    boxArtUrl = boxArtUrl,
    logoPath = logoPath,
    logoUrl = logoUrl,
    theGamesDbId = theGamesDbId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Hack.toEntity(): HackEntity = HackEntity(
    id = id,
    name = name,
    generation = generation,
    baseGameTitle = baseGameTitle,
    boxArtPath = boxArtPath,
    boxArtUrl = boxArtUrl,
    logoPath = logoPath,
    logoUrl = logoUrl,
    theGamesDbId = theGamesDbId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun HackWithEntryCountRow.toDomain(): HackWithEntryCount = HackWithEntryCount(
    hack = hack.toDomain(),
    entryCount = entryCount,
)

// --- HallOfFameEntry / PokemonSlot ---

fun HallOfFameEntryEntity.toDomain(): HallOfFameEntry = HallOfFameEntry(
    id = id,
    hackId = hackId,
    playerName = playerName,
    playerId = playerId,
    playtimeText = playtimeText,
    playtimeMinutes = playtimeMinutes,
    screenshotPath = screenshotPath,
    insertedAt = insertedAt,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun HallOfFameEntry.toEntity(): HallOfFameEntryEntity = HallOfFameEntryEntity(
    id = id,
    hackId = hackId,
    playerName = playerName,
    playerId = playerId,
    playtimeText = playtimeText,
    playtimeMinutes = playtimeMinutes,
    screenshotPath = screenshotPath,
    insertedAt = insertedAt,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PokemonSlotEntity.toDomain(): PokemonSlot = PokemonSlot(
    id = id,
    entryId = entryId,
    slotIndex = slotIndex,
    speciesId = speciesId,
    speciesName = speciesName,
    nickname = nickname,
    gender = gender,
    level = level,
    nature = nature,
    ability = ability,
    isShiny = isShiny,
    heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    sourceTemplateId = sourceTemplateId,
)

fun PokemonSlot.toEntity(): PokemonSlotEntity = PokemonSlotEntity(
    id = id,
    entryId = entryId,
    slotIndex = slotIndex,
    speciesId = speciesId,
    speciesName = speciesName,
    nickname = nickname,
    gender = gender,
    level = level,
    nature = nature,
    ability = ability,
    isShiny = isShiny,
    heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    sourceTemplateId = sourceTemplateId,
)

fun HallOfFameEntryWithSlotsRelation.toDomain(): HallOfFameEntryWithSlots = HallOfFameEntryWithSlots(
    entry = entry.toDomain(),
    // @Relation does not guarantee child ordering — sorted explicitly here.
    slots = slots.sortedBy { it.slotIndex }.map { it.toDomain() },
)

// --- PokemonTemplate ---

fun PokemonTemplateEntity.toDomain(): PokemonTemplate = PokemonTemplate(
    id = id,
    label = label,
    speciesId = speciesId,
    speciesName = speciesName,
    nickname = nickname,
    gender = gender,
    level = level,
    nature = nature,
    ability = ability,
    isShiny = isShiny,
    heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PokemonTemplate.toEntity(): PokemonTemplateEntity = PokemonTemplateEntity(
    id = id,
    label = label,
    speciesId = speciesId,
    speciesName = speciesName,
    nickname = nickname,
    gender = gender,
    level = level,
    nature = nature,
    ability = ability,
    isShiny = isShiny,
    heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- Pokédex cache ---

fun PokeSpeciesEntity.toDomain(): PokedexSpecies = PokedexSpecies(
    id = id,
    name = name,
    displayName = displayName,
    searchName = searchName,
    generationIntroduced = generationIntroduced,
    primaryType = primaryType,
    secondaryType = secondaryType,
)

fun PokeMoveEntity.toDomain(): PokedexMove = PokedexMove(
    id = id,
    name = name,
    displayName = displayName,
    searchName = searchName,
    type = type,
)

fun PokeNatureEntity.toDomain(): PokedexNature = PokedexNature(
    id = id,
    name = name,
    displayName = displayName,
    searchName = searchName,
    increasedStat = increasedStat,
    decreasedStat = decreasedStat,
)

fun PokeAbilityEntity.toDomain(): PokedexAbility = PokedexAbility(
    id = id,
    name = name,
    displayName = displayName,
    searchName = searchName,
)

fun PokeItemEntity.toDomain(): PokedexItem = PokedexItem(
    id = id,
    name = name,
    displayName = displayName,
    searchName = searchName,
)
