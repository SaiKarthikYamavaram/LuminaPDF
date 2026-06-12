package com.lumina.pdf

import kotlinx.browser.localStorage

actual object FilePersistence {
    actual fun saveRecentFile(id: String, bytes: ByteArray) {
        // Not supported on JS due to storage limits, metadata-only
    }

    actual fun loadRecentFile(id: String): ByteArray? {
        return null
    }

    actual fun deleteRecentFile(id: String) {
    }

    actual fun saveMetadata(metadata: String) {
        localStorage.setItem("metadata", metadata)
    }

    actual fun loadMetadata(): String? {
        return localStorage.getItem("metadata")
    }
}
