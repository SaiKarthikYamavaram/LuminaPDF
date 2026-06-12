package com.lumina.pdf

import java.io.File

actual object FilePersistence {
    private val appDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".luminapdf")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val recentsDir: File by lazy {
        val dir = File(appDir, "recents")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val metadataFile: File by lazy {
        File(appDir, "metadata.txt")
    }

    actual fun saveRecentFile(id: String, bytes: ByteArray) {
        try {
            File(recentsDir, id).writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun loadRecentFile(id: String): ByteArray? {
        return try {
            val file = File(recentsDir, id)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun deleteRecentFile(id: String) {
        try {
            val file = File(recentsDir, id)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun saveMetadata(metadata: String) {
        try {
            metadataFile.writeText(metadata)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun loadMetadata(): String? {
        return try {
            if (metadataFile.exists()) metadataFile.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
