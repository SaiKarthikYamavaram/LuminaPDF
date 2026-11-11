package com.lumina.pdf

import java.io.File
import co.touchlab.kermit.Logger

actual object FilePersistence {
    private val context get() = AndroidContext.applicationContext

    private val recentsDir: File? by lazy {
        val dir = context?.let { File(it.filesDir, "recents") }
        if (dir != null && !dir.exists()) dir.mkdirs()
        dir
    }

    private val metadataFile: File? by lazy {
        context?.let { File(it.filesDir, "metadata.txt") }
    }

    actual fun saveRecentFile(id: String, bytes: ByteArray) {
        try {
            recentsDir?.let { dir ->
                File(dir, id).writeBytes(bytes)
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to save recent file: $id" }
        }
    }

    actual fun loadRecentFile(id: String): ByteArray? {
        return try {
            recentsDir?.let { dir ->
                val file = File(dir, id)
                if (file.exists()) file.readBytes() else null
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load recent file: $id" }
            null
        }
    }

    actual fun deleteRecentFile(id: String) {
        try {
            recentsDir?.let { dir ->
                val file = File(dir, id)
                if (file.exists()) file.delete()
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to delete recent file: $id" }
        }
    }

    actual fun saveMetadata(metadata: String) {
        try {
            metadataFile?.let { file ->
                // Temp file plus rename, so a crash mid-write can't leave a truncated list.
                val temp = File(file.path + ".tmp")
                temp.writeText(metadata)
                check(temp.renameTo(file)) { "Could not replace ${file.name}" }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to save metadata" }
        }
    }

    actual fun loadMetadata(): String? {
        return try {
            metadataFile?.let { file ->
                if (file.exists()) file.readText() else null
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load metadata" }
            null
        }
    }
}
