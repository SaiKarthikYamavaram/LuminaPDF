package com.lumina.pdf

import java.io.File
import co.touchlab.kermit.Logger

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
            Logger.e(e) { "Failed to save recent file: $id" }
        }
    }

    actual fun loadRecentFile(id: String): ByteArray? {
        return try {
            val file = File(recentsDir, id)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load recent file: $id" }
            null
        }
    }

    actual fun deleteRecentFile(id: String) {
        try {
            val file = File(recentsDir, id)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to delete recent file: $id" }
        }
    }

    actual fun saveMetadata(metadata: String) {
        try {
            metadataFile.writeText(metadata)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to save metadata" }
        }
    }

    actual fun loadMetadata(): String? {
        return try {
            if (metadataFile.exists()) metadataFile.readText() else null
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load metadata" }
            null
        }
    }
}
