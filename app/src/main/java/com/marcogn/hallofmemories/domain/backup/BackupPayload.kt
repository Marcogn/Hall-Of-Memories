package com.marcogn.hallofmemories.domain.backup

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** The current format this build writes and the newest one it can read — see [BackupFormatTooNewException]. */
const val CURRENT_BACKUP_FORMAT_VERSION = 1

/** Thrown when a backup's `formatVersion` is newer than this build understands — a clear, typed rejection rather than a parse crash or silent data loss. */
class BackupFormatTooNewException(val fileVersion: Int) :
    Exception("Backup format $fileVersion is newer than this app supports (max $CURRENT_BACKUP_FORMAT_VERSION)")

/**
 * The local backup format (spec §5): a single zip with this as `data.json` plus every referenced
 * image under `images/`. Image fields carry a **bare file name** only, never an absolute path — a
 * path from another device/install is meaningless; [data.backup.LocalBackupManager] resolves each
 * name to a freshly written local path at restore time. The PokéAPI cache is never included here —
 * it is re-downloadable, not user data.
 */
@Serializable
data class BackupPayload(
    val formatVersion: Int = CURRENT_BACKUP_FORMAT_VERSION,
    val exportedAt: String,
    val hacks: List<BackupHackDto> = emptyList(),
    val templates: List<BackupTemplateDto> = emptyList(),
)

@Serializable
data class BackupSlotDto(
    val id: String,
    val slotIndex: Int,
    val speciesId: Int?,
    val speciesName: String?,
    val nickname: String?,
    val gender: String,
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

@Serializable
data class BackupEntryDto(
    val id: String,
    val playerName: String,
    val playerId: String,
    val playtimeText: String,
    val playtimeMinutes: Int?,
    val screenshotFileName: String?,
    val insertedAt: String,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val slots: List<BackupSlotDto>,
)

@Serializable
data class BackupHackDto(
    val id: String,
    val name: String,
    val generation: String,
    val baseGameTitle: String?,
    val boxArtFileName: String?,
    val boxArtUrl: String?,
    val logoFileName: String?,
    val logoUrl: String?,
    val theGamesDbId: Long?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val entries: List<BackupEntryDto>,
)

@Serializable
data class BackupTemplateDto(
    val id: String,
    val label: String,
    val speciesId: Int?,
    val speciesName: String?,
    val nickname: String?,
    val gender: String,
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
    val createdAt: String,
    val updatedAt: String,
)

private fun String.fileNameOf(): String? = substringAfterLast('/')

fun PokemonSlot.toBackupDto(): BackupSlotDto = BackupSlotDto(
    id = id, slotIndex = slotIndex, speciesId = speciesId, speciesName = speciesName, nickname = nickname,
    gender = gender.name, level = level, nature = nature, ability = ability, isShiny = isShiny, heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    sourceTemplateId = sourceTemplateId,
)

fun BackupSlotDto.toDomain(entryId: String): PokemonSlot = PokemonSlot(
    id = id, entryId = entryId, slotIndex = slotIndex, speciesId = speciesId, speciesName = speciesName,
    nickname = nickname, gender = PokemonGender.valueOf(gender), level = level, nature = nature, ability = ability,
    isShiny = isShiny, heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    sourceTemplateId = sourceTemplateId,
)

fun HallOfFameEntryWithSlots.toBackupDto(): BackupEntryDto = BackupEntryDto(
    id = entry.id,
    playerName = entry.playerName,
    playerId = entry.playerId,
    playtimeText = entry.playtimeText,
    playtimeMinutes = entry.playtimeMinutes,
    screenshotFileName = entry.screenshotPath?.fileNameOf(),
    insertedAt = entry.insertedAt.toString(),
    notes = entry.notes,
    createdAt = entry.createdAt.toString(),
    updatedAt = entry.updatedAt.toString(),
    slots = slots.map { it.toBackupDto() },
)

/** [resolvedScreenshotPath] is the absolute path already written on this device by the caller — see [BackupSlotDto.toDomain]'s sibling for slots. */
fun BackupEntryDto.toDomain(hackId: String, resolvedScreenshotPath: String?): HallOfFameEntryWithSlots {
    val entry = HallOfFameEntry(
        id = id,
        hackId = hackId,
        playerName = playerName,
        playerId = playerId,
        playtimeText = playtimeText,
        playtimeMinutes = playtimeMinutes,
        screenshotPath = resolvedScreenshotPath,
        insertedAt = Instant.parse(insertedAt),
        notes = notes,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )
    return HallOfFameEntryWithSlots(entry = entry, slots = slots.map { it.toDomain(entryId = id) })
}

fun Hack.toBackupDto(entries: List<HallOfFameEntryWithSlots>): BackupHackDto = BackupHackDto(
    id = id,
    name = name,
    generation = generation.name,
    baseGameTitle = baseGameTitle,
    boxArtFileName = boxArtPath?.fileNameOf(),
    boxArtUrl = boxArtUrl,
    logoFileName = logoPath?.fileNameOf(),
    logoUrl = logoUrl,
    theGamesDbId = theGamesDbId,
    notes = notes,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    entries = entries.map { it.toBackupDto() },
)

/** [resolvedBoxArtPath]/[resolvedLogoPath] are absolute paths already written on this device by the caller. */
fun BackupHackDto.toDomain(resolvedBoxArtPath: String?, resolvedLogoPath: String?): Hack = Hack(
    id = id,
    name = name,
    generation = GameGeneration.valueOf(generation),
    baseGameTitle = baseGameTitle,
    boxArtPath = resolvedBoxArtPath,
    boxArtUrl = boxArtUrl,
    logoPath = resolvedLogoPath,
    logoUrl = logoUrl,
    theGamesDbId = theGamesDbId,
    notes = notes,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

fun PokemonTemplate.toBackupDto(): BackupTemplateDto = BackupTemplateDto(
    id = id, label = label, speciesId = speciesId, speciesName = speciesName, nickname = nickname,
    gender = gender.name, level = level, nature = nature, ability = ability, isShiny = isShiny, heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    createdAt = createdAt.toString(), updatedAt = updatedAt.toString(),
)

fun BackupTemplateDto.toDomain(): PokemonTemplate = PokemonTemplate(
    id = id, label = label, speciesId = speciesId, speciesName = speciesName, nickname = nickname,
    gender = PokemonGender.valueOf(gender), level = level, nature = nature, ability = ability, isShiny = isShiny,
    heldItem = heldItem,
    ivHp = ivHp, ivAtk = ivAtk, ivDef = ivDef, ivSpAtk = ivSpAtk, ivSpDef = ivSpDef, ivSpe = ivSpe,
    evHp = evHp, evAtk = evAtk, evDef = evDef, evSpAtk = evSpAtk, evSpDef = evSpDef, evSpe = evSpe,
    move1 = move1, move2 = move2, move3 = move3, move4 = move4,
    createdAt = Instant.parse(createdAt), updatedAt = Instant.parse(updatedAt),
)

private val backupJson = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun BackupPayload.toJson(): String = backupJson.encodeToString(this)

/** @throws BackupFormatTooNewException if the decoded payload's `formatVersion` is newer than this build understands. */
fun String.toBackupPayload(): BackupPayload {
    val payload = backupJson.decodeFromString<BackupPayload>(this)
    if (payload.formatVersion > CURRENT_BACKUP_FORMAT_VERSION) throw BackupFormatTooNewException(payload.formatVersion)
    return payload
}
