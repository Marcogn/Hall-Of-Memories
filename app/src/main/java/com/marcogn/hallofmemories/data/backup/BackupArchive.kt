package com.marcogn.hallofmemories.data.backup

import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.domain.backup.BackupPayload
import com.marcogn.hallofmemories.domain.backup.toBackupPayload
import com.marcogn.hallofmemories.domain.backup.toJson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DATA_ENTRY = "data.json"
private const val IMAGES_PREFIX = "images/"

/**
 * A single archive holds `data.json` (every hack, its entries and slots, and every template) plus
 * only the images the payload actually references, under `images/`. `ImageStorage.listAll()` can
 * hold files not referenced by anything current (e.g. a superseded artwork pick whose original
 * hasn't been cleaned up yet); pulling in every file unconditionally would make the archive grow
 * with everything ever downloaded, not with the library's actual size.
 */
@Singleton
class BackupArchiveBuilder @Inject constructor(
    private val imageStorage: ImageStorage,
) {
    suspend fun build(payload: BackupPayload): ByteArray = withContext(Dispatchers.IO) {
        val referencedFileNames = buildSet {
            payload.hacks.forEach { hack ->
                hack.boxArtFileName?.let(::add)
                hack.logoFileName?.let(::add)
                hack.entries.forEach { entry -> entry.screenshotFileName?.let(::add) }
            }
        }
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(payload.toJson().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            imageStorage.listAll()
                .filter { file -> file.name in referencedFileNames }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry("$IMAGES_PREFIX${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        buffer.toByteArray()
    }
}

data class BackupArchiveContent(val payload: BackupPayload, val images: Map<String, ByteArray>)

class BackupArchiveMissingDataException : Exception("Backup non valido: manca data.json nell'archivio")

@Singleton
class BackupArchiveReader @Inject constructor() {
    /** @throws BackupArchiveMissingDataException if the zip has no `data.json` entry. @throws com.marcogn.hallofmemories.domain.backup.BackupFormatTooNewException if its formatVersion is newer than this build supports. */
    suspend fun read(bytes: ByteArray): BackupArchiveContent = withContext(Dispatchers.IO) {
        var payload: BackupPayload? = null
        val images = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val content = zip.readBytes()
                when {
                    entry.name == DATA_ENTRY -> payload = content.toString(Charsets.UTF_8).toBackupPayload()
                    entry.name.startsWith(IMAGES_PREFIX) -> images[entry.name.removePrefix(IMAGES_PREFIX)] = content
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        BackupArchiveContent(payload = payload ?: throw BackupArchiveMissingDataException(), images = images)
    }
}
