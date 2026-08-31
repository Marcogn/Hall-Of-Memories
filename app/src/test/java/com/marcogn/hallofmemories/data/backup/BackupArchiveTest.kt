package com.marcogn.hallofmemories.data.backup

import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.domain.backup.BackupHackDto
import com.marcogn.hallofmemories.domain.backup.BackupPayload
import com.marcogn.hallofmemories.domain.backup.toJson
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class BackupArchiveTest {

    private val imageStorage = ImageStorage(ApplicationProvider.getApplicationContext())

    @Test
    fun `build then read round-trips the payload and only the images it references`() = runTest {
        val referencedBytes = byteArrayOf(1, 2, 3)
        val orphanBytes = byteArrayOf(4, 5, 6)
        imageStorage.persistBytes(referencedBytes) // gets a random UUID name — read it back to reference it below
        val referencedName = imageStorage.listAll().first().name
        imageStorage.persistBytes(orphanBytes) // never referenced by the payload — must be excluded from the archive

        val payload = BackupPayload(
            exportedAt = "2026-01-01T00:00:00Z",
            hacks = listOf(
                BackupHackDto(
                    id = "hack-1", name = "Test Hack", generation = "RSE", baseGameTitle = null,
                    boxArtFileName = referencedName, boxArtUrl = null, logoFileName = null, logoUrl = null,
                    theGamesDbId = null, notes = null, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
                    entries = emptyList(),
                ),
            ),
        )

        val archive = BackupArchiveBuilder(imageStorage).build(payload)
        val content = BackupArchiveReader().read(archive)

        assertEquals(payload, content.payload)
        assertEquals(setOf(referencedName), content.images.keys)
    }

    @Test
    fun `a zip missing data json fails cleanly`() = runTest {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry("images/stray.jpg"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }

        try {
            BackupArchiveReader().read(buffer.toByteArray())
            fail("Expected BackupArchiveMissingDataException")
        } catch (e: BackupArchiveMissingDataException) {
            // expected
        }
    }

    @Test
    fun `the reader extracts every image entry regardless of whether the payload references it`() = runTest {
        val payload = BackupPayload(exportedAt = "2026-01-01T00:00:00Z")
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(payload.toJson().toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("images/orphan.jpg"))
            zip.write(byteArrayOf(9, 9, 9))
            zip.closeEntry()
        }

        val content = BackupArchiveReader().read(buffer.toByteArray())

        assertTrue(content.images.containsKey("orphan.jpg"))
        assertArrayEquals(byteArrayOf(9, 9, 9), content.images["orphan.jpg"])
    }
}
