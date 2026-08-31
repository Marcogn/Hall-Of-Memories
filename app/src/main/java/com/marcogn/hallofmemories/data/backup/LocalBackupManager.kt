package com.marcogn.hallofmemories.data.backup

import android.content.Context
import android.net.Uri
import com.marcogn.hallofmemories.domain.repository.BackupImportResult
import com.marcogn.hallofmemories.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local, SAF-based backup/restore orchestration (spec §5): Room + `images/` <-> a zip archive <->
 * a user-picked destination. Never touches external storage directly —
 * `ActivityResultContracts.CreateDocument`/`OpenDocument` give the SAF `Uri`s this reads and
 * writes through the `ContentResolver`.
 */
@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val archiveBuilder: BackupArchiveBuilder,
    private val archiveReader: BackupArchiveReader,
) {
    suspend fun exportTo(destination: Uri) {
        val archive = archiveBuilder.build(backupRepository.exportPayload())
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(destination)?.use { it.write(archive) }
                ?: error("Impossibile scrivere sulla destinazione selezionata")
        }
    }

    /** @throws BackupArchiveMissingDataException / com.marcogn.hallofmemories.domain.backup.BackupFormatTooNewException — the caller shows a specific message for either; nothing is written to Room until the whole archive parses. */
    suspend fun importFrom(source: Uri): BackupImportResult {
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                ?: error("Impossibile leggere il file selezionato")
        }
        val content = archiveReader.read(bytes)
        return backupRepository.importPayload(content.payload, content.images)
    }
}
