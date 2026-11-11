package com.lumina.pdf

// The metadata format is "|"-delimited with one row per line, so a delimiter inside a free-text
// field (a typed name, or a filename from the OS) would shift every column after it.
private fun String.metadataSafe() = replace('|', ' ').replace('\n', ' ').replace('\r', ' ')

// Format: id|name|lastOpened|size|isSample|inRecents|folder|byteCount
internal fun serializeMetadata(files: List<RecentFile>): String = buildString {
    files.forEach { file ->
        val sampleStr = if (file.isSample) "1" else "0"
        val recentsStr = if (file.inRecents) "1" else "0"
        append("${file.id}|${file.name.metadataSafe()}|${file.lastOpened}|${file.size}|$sampleStr|$recentsStr|${file.folder.metadataSafe()}|${file.byteCount}\n")
    }
}

object RecentFilesStore {
    // Writes metadata only. A document's bytes are written once, when it is imported.
    fun saveRecentFilesList(files: List<RecentFile>) {
        FilePersistence.saveMetadata(serializeMetadata(files))
    }

    fun loadRecentFilesList(): List<RecentFile> {
        val metadata = FilePersistence.loadMetadata() ?: return emptyList()
        val list = mutableListOf<RecentFile>()
        var migratedOlderRows = false
        metadata.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split('|')
            if (parts.size >= 5) {
                val id = parts[0]
                val name = parts[1]
                val lastOpened = parts[2]
                val size = parts[3]
                val isSample = parts[4] == "1"
                val inRecents = if (parts.size >= 6) parts[5] == "1" else true
                val folder = if (parts.size >= 7) parts[6] else ""
                // Rows written before this column existed get their size from the stored file, once.
                val byteCount = parts.getOrNull(7)?.toIntOrNull() ?: run {
                    migratedOlderRows = true
                    if (isSample) 0 else FilePersistence.loadRecentFile(id)?.size ?: 0
                }

                list.add(
                    RecentFile(
                        id = id,
                        name = name,
                        lastOpened = lastOpened,
                        size = size,
                        isSample = isSample,
                        byteCount = byteCount,
                        inRecents = inRecents,
                        folder = folder
                    )
                )
            }
        }
        if (migratedOlderRows) saveRecentFilesList(list)
        return list
    }
    
    fun deleteRecentFile(id: String) {
        FilePersistence.deleteRecentFile(id)
    }
}
