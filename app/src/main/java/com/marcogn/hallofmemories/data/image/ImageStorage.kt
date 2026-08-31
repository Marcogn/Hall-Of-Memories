package com.marcogn.hallofmemories.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Longest edge a stored image is downsampled to — comfortably more than any on-screen use needs. */
private const val IMAGE_MAX_DIMENSION = 900
private const val IMAGE_JPEG_QUALITY = 85

enum class ImageFormat { JPEG, PNG }

/**
 * Persists a picked or downloaded image into the app's internal storage (`filesDir/images/`) so
 * it survives beyond the photo picker's transient permission or a search result's remote URL, and
 * exposes it as a plain absolute path. Shared by hack box art/logo (Phase 2) and Hall of Fame
 * entry screenshots (Phase 3) — the one `images/` directory, not a per-feature one.
 *
 * Ported from ThePatientGamerHelper's `ImageStorage`, generalised from "covers" to any image. Every
 * write downsamples and re-encodes from the first write, not retrofitted later: full-size
 * TheGamesDB box art bloated both the on-device footprint and every backup in the sibling app,
 * needing a retroactive fix.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    /** Persists a photo-picker selection. */
    suspend fun persist(sourceUri: Uri, format: ImageFormat = ImageFormat.JPEG): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: error("Unable to read the selected image")
        writeCompressed(bytes, format)
    }

    /**
     * Persists downloaded image bytes (e.g. from TheGamesDB). [format] defaults to JPEG; pass
     * [ImageFormat.PNG] for a logo — a JPEG re-encode would destroy the transparency a clear-logo
     * needs (spec §6).
     */
    suspend fun persistBytes(bytes: ByteArray, format: ImageFormat = ImageFormat.JPEG): String =
        withContext(Dispatchers.IO) { writeCompressed(bytes, format) }

    private fun writeCompressed(bytes: ByteArray, format: ImageFormat): String {
        val extension = if (format == ImageFormat.PNG) "png" else "jpg"
        val destination = File(imagesDir, "${UUID.randomUUID()}.$extension")
        val downsampled = decodeDownsampled(bytes)
        destination.outputStream().use { output ->
            if (downsampled != null) {
                val compressFormat = if (format == ImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                downsampled.compress(compressFormat, IMAGE_JPEG_QUALITY, output)
                downsampled.recycle()
            } else {
                // Not a decodable bitmap (or Robolectric's shadow decoder in tests) — keep the
                // original bytes rather than silently losing the image.
                output.write(bytes)
            }
        }
        return destination.absolutePath
    }

    private fun decodeDownsampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > IMAGE_MAX_DIMENSION || bounds.outHeight / sampleSize > IMAGE_MAX_DIMENSION) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** Copies an existing image to a new file with an independent lifecycle (used from Phase 4/5 on). */
    suspend fun duplicate(sourcePath: String): String = withContext(Dispatchers.IO) {
        val extension = File(sourcePath).extension.ifBlank { "jpg" }
        val destination = File(imagesDir, "${UUID.randomUUID()}.$extension")
        File(sourcePath).copyTo(destination, overwrite = true)
        destination.absolutePath
    }

    /** Best-effort: silently does nothing for a blank path or a file outside `images/`. */
    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        val file = File(path)
        if (file.exists() && file.parentFile == imagesDir) {
            file.delete()
        }
    }

    /** Every file currently in `images/` — used by the backup archive builder to include only images a saved row still references (Phase 5). */
    suspend fun listAll(): List<File> = withContext(Dispatchers.IO) {
        imagesDir.listFiles()?.toList() ?: emptyList()
    }

    /** Writes already-downsampled bytes under the exact [fileName] given (a backup restore, reusing the name the archive stored it under) rather than generating a new UUID. */
    suspend fun writeBytes(fileName: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val destination = File(imagesDir, fileName)
        destination.writeBytes(bytes)
        destination.absolutePath
    }

    /** Deletes every file in `images/` — used only by backup restore, which is about to write a full replacement set. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        imagesDir.listFiles()?.forEach { it.delete() }
    }
}
