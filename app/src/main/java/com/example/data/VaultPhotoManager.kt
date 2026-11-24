package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies photos selected via the zero-permission Android Photo Picker into
 * app-private internal storage so thumbnails stay accessible across restarts.
 */
object VaultPhotoManager {
    private const val PHOTOS_DIR = "vault_photos"

    fun persistPickedPhoto(context: Context, sourceUri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, PHOTOS_DIR).apply {
                if (!exists()) mkdirs()
            }
            val extension = when (context.contentResolver.getType(sourceUri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val destFile = File(dir, "ammo_${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(destFile).toString()
        }.getOrNull()
    }

    fun deleteManagedPhoto(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        runCatching {
            val parsed = Uri.parse(uriString)
            if (parsed.scheme == "file") {
                val path = parsed.path ?: return
                val file = File(path)
                if (file.exists() && file.parentFile?.name == PHOTOS_DIR) {
                    file.delete()
                }
            }
        }
    }
}
