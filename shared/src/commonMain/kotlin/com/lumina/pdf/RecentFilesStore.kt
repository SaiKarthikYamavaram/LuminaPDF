package com.lumina.pdf

// The metadata format is "|"-delimited with one row per line, so a delimiter inside a free-text
// field (a typed name, or a filename from the OS) would shift every column after it.
private fun String.metadataSafe() = replace('|', ' ').replace('\n', ' ').replace('\r', ' ')

// Format: id|name|lastOpened|size|isSample|inRecents|folder
internal fun serializeMetadata(files: List<RecentFile>): String = buildString {
    files.forEach { file ->
        val sampleStr = if (file.isSample) "1" else "0"
        val recentsStr = if (file.inRecents) "1" else "0"
        append("${file.id}|${file.name.metadataSafe()}|${file.lastOpened}|${file.size}|$sampleStr|$recentsStr|${file.folder.metadataSafe()}\n")
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
                
                // Load file bytes if not sample
                val bytes = if (isSample) {
                    null // sample bytes loaded statically from resources
                } else {
                    FilePersistence.loadRecentFile(id)
                }
                
                list.add(
                    RecentFile(
                        id = id,
                        name = name,
                        lastOpened = lastOpened,
                        size = size,
                        isSample = isSample,
                        fileBytes = bytes,
                        inRecents = inRecents,
                        folder = folder
                    )
                )
            }
        }
        return list
    }
    
    fun deleteRecentFile(id: String) {
        FilePersistence.deleteRecentFile(id)
    }
}
