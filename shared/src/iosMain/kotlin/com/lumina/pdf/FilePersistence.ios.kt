package com.lumina.pdf

import platform.Foundation.*
import kotlinx.cinterop.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object FilePersistence {
    private val fileManager = NSFileManager.defaultManager

    private val recentsPath: String? by lazy {
        val paths = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentsURL = paths.firstOrNull() as? NSURL
        val recentsURL = documentsURL?.URLByAppendingPathComponent("recents")
        recentsURL?.path?.also { path ->
            if (!fileManager.fileExistsAtPath(path)) {
                fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
            }
        }
    }

    private val metadataPath: String? by lazy {
        val paths = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentsURL = paths.firstOrNull() as? NSURL
        val metadataURL = documentsURL?.URLByAppendingPathComponent("metadata.txt")
        metadataURL?.path
    }

    actual fun saveRecentFile(id: String, bytes: ByteArray) {
        val path = recentsPath?.let { "$it/$id" } ?: return
        val data = bytes.toNSData()
        data.writeToFile(path, atomically = true)
    }

    actual fun loadRecentFile(id: String): ByteArray? {
        val path = recentsPath?.let { "$it/$id" } ?: return null
        if (!fileManager.fileExistsAtPath(path)) return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    actual fun deleteRecentFile(id: String) {
        val path = recentsPath?.let { "$it/$id" } ?: return
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    actual fun saveMetadata(metadata: String) {
        val path = metadataPath ?: return
        val nsString = metadata as NSString
        nsString.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun loadMetadata(): String? {
        val path = metadataPath ?: return null
        if (!fileManager.fileExistsAtPath(path)) return null
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as String?
    }

    // Helpers to convert between ByteArray and NSData
    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), length = size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray {
        val bytes = this.bytes ?: return ByteArray(0)
        val length = this.length.toInt()
        val byteArray = ByteArray(length)
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, this.length)
        }
        return byteArray
    }
}
