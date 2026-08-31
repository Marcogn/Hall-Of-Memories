package com.marcogn.hallofmemories.data.repository

import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.data.local.dao.BackupDao
import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.domain.backup.BackupPayload
import com.marcogn.hallofmemories.domain.backup.toBackupDto
import com.marcogn.hallofmemories.domain.backup.toDomain
import com.marcogn.hallofmemories.domain.repository.BackupImportResult
import com.marcogn.hallofmemories.domain.repository.BackupRepository
import com.marcogn.hallofmemories.domain.repository.HackRepository
import com.marcogn.hallofmemories.domain.repository.HallOfFameRepository
import com.marcogn.hallofmemories.domain.repository.PokemonTemplateRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val hackRepository: HackRepository,
    private val hallOfFameRepository: HallOfFameRepository,
    private val templateRepository: PokemonTemplateRepository,
    private val imageStorage: ImageStorage,
    private val backupDao: BackupDao,
) : BackupRepository {

    override suspend fun exportPayload(): BackupPayload {
        val hacks = hackRepository.observeAll().first().map { it.hack }
        val hackDtos = hacks.map { hack ->
            val entries = hallOfFameRepository.observeByHack(hack.id).first()
            hack.toBackupDto(entries)
        }
        val templates = templateRepository.observeAll().first().map { it.toBackupDto() }
        return BackupPayload(exportedAt = Instant.now().toString(), hacks = hackDtos, templates = templates)
    }

    override suspend fun importPayload(payload: BackupPayload, images: Map<String, ByteArray>): BackupImportResult {
        var imagesSkipped = 0

        suspend fun resolveImage(fileName: String?): String? {
            if (fileName == null) return null
            val bytes = images[fileName] ?: run { imagesSkipped++; return null }
            return imageStorage.writeBytes(fileName, bytes)
        }

        // Full replace (spec §5): every existing image is gone before the restored ones are written back.
        imageStorage.clearAll()

        val hackEntities = mutableListOf<HackEntity>()
        val entryEntities = mutableListOf<HallOfFameEntryEntity>()
        val slotEntities = mutableListOf<PokemonSlotEntity>()

        for (hackDto in payload.hacks) {
            val boxArtPath = resolveImage(hackDto.boxArtFileName)
            val logoPath = resolveImage(hackDto.logoFileName)
            hackEntities += hackDto.toDomain(resolvedBoxArtPath = boxArtPath, resolvedLogoPath = logoPath).toEntity()

            for (entryDto in hackDto.entries) {
                val screenshotPath = resolveImage(entryDto.screenshotFileName)
                val entryWithSlots = entryDto.toDomain(hackId = hackDto.id, resolvedScreenshotPath = screenshotPath)
                entryEntities += entryWithSlots.entry.toEntity()
                slotEntities += entryWithSlots.slots.map { it.toEntity() }
            }
        }

        val templateEntities = payload.templates.map { it.toDomain().toEntity() }

        backupDao.replaceAll(hackEntities, entryEntities, slotEntities, templateEntities)

        return BackupImportResult(
            hacksImported = hackEntities.size,
            entriesImported = entryEntities.size,
            templatesImported = templateEntities.size,
            imagesSkipped = imagesSkipped,
        )
    }
}
