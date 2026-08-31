package com.marcogn.hallofmemories.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class ImageStorageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val imageStorage = ImageStorage(context)

    @Test
    fun `persistBytes downsamples an oversized image to the target dimension`() = runTest {
        val path = imageStorage.persistBytes(solidColorBytes(width = 2000, height = 3000, format = Bitmap.CompressFormat.JPEG))

        val (width, height) = decodeDimensions(File(path).readBytes())
        assertTrue("width $width should be downsampled", width <= 900)
        assertTrue("height $height should be downsampled", height <= 900)
    }

    @Test
    fun `persistBytes with PNG format writes a file decodable as PNG, not re-encoded as JPEG`() = runTest {
        val path = imageStorage.persistBytes(
            solidColorBytes(width = 200, height = 200, format = Bitmap.CompressFormat.PNG),
            format = ImageFormat.PNG,
        )

        assertTrue("expected a .png file, got $path", path.endsWith(".png"))
        val bytes = File(path).readBytes()
        // PNG magic number: 0x89 'P' 'N' 'G' '\r' '\n' 0x1A '\n'
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('N'.code.toByte(), bytes[2])
        assertEquals('G'.code.toByte(), bytes[3])
    }

    @Test
    fun `persist reads a gallery-picked content uri`() = runTest {
        val bytes = solidColorBytes(width = 100, height = 100, format = Bitmap.CompressFormat.JPEG)
        val sourceFile = File(context.cacheDir, "picked.jpg").apply { writeBytes(bytes) }

        val path = imageStorage.persist(sourceFile.toUri())

        assertTrue(File(path).exists())
        assertTrue(path.endsWith(".jpg"))
    }

    @Test
    fun `duplicate copies the file to a new path, independent of the original`() = runTest {
        val original = imageStorage.persistBytes(solidColorBytes(100, 100, Bitmap.CompressFormat.JPEG))

        val duplicate = imageStorage.duplicate(original)

        assertNotEquals(original, duplicate)
        assertTrue(File(duplicate).exists())
        imageStorage.delete(original)
        assertTrue("the duplicate must survive deleting the original", File(duplicate).exists())
    }

    @Test
    fun `delete removes the file`() = runTest {
        val path = imageStorage.persistBytes(solidColorBytes(100, 100, Bitmap.CompressFormat.JPEG))
        assertTrue(File(path).exists())

        imageStorage.delete(path)

        assertFalse(File(path).exists())
    }

    @Test
    fun `delete on a blank or null path is a no-op, not a crash`() = runTest {
        imageStorage.delete(null)
        imageStorage.delete("")
    }

    private fun solidColorBytes(width: Int, height: Int, format: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val out = ByteArrayOutputStream()
        bitmap.compress(format, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun decodeDimensions(bytes: ByteArray): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth to options.outHeight
    }
}
