package com.marcogn.hallofmemories.domain.repository

import com.marcogn.hallofmemories.domain.backup.BackupPayload

/** Row counts from a completed import — [imagesSkipped] is a missing-file count, never a failure (spec §5: images are best-effort). */
data class BackupImportResult(
    val hacksImported: Int,
    val entriesImported: Int,
    val templatesImported: Int,
    val imagesSkipped: Int,
)

/**
 * Produces and consumes a [BackupPayload] with no knowledge of where the bytes go — the Drive
 * seam for v2 (spec §5): a future `DriveBackupManager` becomes a sibling of
 * `data/backup/LocalBackupManager.kt`, not a change to this interface or its implementation.
 */
interface BackupRepository {
    /** Always covers everything — hacks, their entries and slots, and every template — ignoring any active UI filter. */
    suspend fun exportPayload(): BackupPayload

    /**
     * Full replace inside one transaction: every existing hack/entry/slot/template is gone before
     * the restored ones land, ids and timestamps preserved. [images] maps a backup-relative file
     * name to its bytes, already read from the archive; a name referenced by the payload but
     * missing from [images] restores that row with a null image path instead of failing the import.
     */
    suspend fun importPayload(payload: BackupPayload, images: Map<String, ByteArray>): BackupImportResult
}
