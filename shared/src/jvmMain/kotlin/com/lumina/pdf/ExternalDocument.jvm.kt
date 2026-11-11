package com.lumina.pdf

import co.touchlab.kermit.Logger
import java.awt.Desktop
import java.io.File

private fun openFile(file: File) {
    ExternalDocument.open {
        try {
            file.name to file.readBytes()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to read document: ${file.path}" }
            null
        }
    }
}

// Windows and Linux pass an associated file as a program argument. macOS sends an event instead.
fun handleDesktopFileOpen(args: Array<String>) {
    args.firstOrNull { it.endsWith(".pdf", ignoreCase = true) }?.let { openFile(File(it)) }

    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
        Desktop.getDesktop().setOpenFileHandler { event -> event.files.firstOrNull()?.let(::openFile) }
    }
}
