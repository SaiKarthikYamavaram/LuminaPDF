package com.lumina.pdf

import java.io.File

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
            e.printStackTrace()
        }
    }

    actual fun loadRecentFile(id: String): ByteArray? {
        return try {
            recentsDir?.let { dir ->
                val file = File(dir, id)
                if (file.exists()) file.readBytes() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
        }
    }

    actual fun saveMetadata(metadata: String) {
        try {
            metadataFile?.writeText(metadata)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun loadMetadata(): String? {
        return try {
            metadataFile?.let { file ->
                if (file.exists()) file.readText() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
