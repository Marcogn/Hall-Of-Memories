package com.marcogn.hallofmemories.data.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * A `content://` URI under `cacheDir/camera/`, backed by the `FileProvider` declared in the
 * manifest, for [androidx.activity.result.contract.ActivityResultContracts.TakePicture] to write
 * into. The file lives in the cache (not `filesDir/images/`, [ImageStorage]'s own directory) —
 * it's a transient handoff to the system camera app, persisted for real only once
 * `ImageStorage.persist()` copies its bytes in, same as any gallery pick.
 */
fun Context.createCameraCaptureUri(): Uri {
    val dir = File(cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}
