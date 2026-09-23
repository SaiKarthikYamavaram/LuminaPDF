package com.lumina.pdf

object RecentFilesStore {
    fun saveRecentFilesList(files: List<RecentFile>) {
        val sb = StringBuilder()
        files.forEach { file ->
            // Format: id|name|lastOpened|size|isSample|inRecents|folder
            val sampleStr = if (file.isSample) "1" else "0"
            val recentsStr = if (file.inRecents) "1" else "0"
            sb.append("${file.id}|${file.name}|${file.lastOpened}|${file.size}|$sampleStr|$recentsStr|${file.folder}\n")
            
            // Save file bytes if they are loaded and it's not a sample
            if (!file.isSample && file.fileBytes != null) {
                FilePersistence.saveRecentFile(file.id, file.fileBytes)
            }
        }
        FilePersistence.saveMetadata(sb.toString())
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
