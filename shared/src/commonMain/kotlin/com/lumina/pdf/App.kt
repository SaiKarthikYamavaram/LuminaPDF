package com.lumina.pdf

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.renderer.PdfDocumentFactory
import com.lumina.pdf.viewer.PdfViewer
import com.lumina.pdf.viewer.asImageBitmap
import kotlinx.coroutines.launch
import luminapdf.shared.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    val interFontFamily = FontFamily(
        Font(Res.font.Inter_Regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.Inter_Medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.Inter_Bold, FontWeight.Bold, FontStyle.Normal)
    )

    val typography = Typography(
        displayLarge = TextStyle(fontFamily = interFontFamily),
        displayMedium = TextStyle(fontFamily = interFontFamily),
        displaySmall = TextStyle(fontFamily = interFontFamily),
        headlineLarge = TextStyle(fontFamily = interFontFamily),
        headlineMedium = TextStyle(fontFamily = interFontFamily),
        headlineSmall = TextStyle(fontFamily = interFontFamily),
        titleLarge = TextStyle(fontFamily = interFontFamily),
        titleMedium = TextStyle(fontFamily = interFontFamily),
        titleSmall = TextStyle(fontFamily = interFontFamily),
        bodyLarge = TextStyle(fontFamily = interFontFamily),
        bodyMedium = TextStyle(fontFamily = interFontFamily),
        bodySmall = TextStyle(fontFamily = interFontFamily),
        labelLarge = TextStyle(fontFamily = interFontFamily),
        labelMedium = TextStyle(fontFamily = interFontFamily),
        labelSmall = TextStyle(fontFamily = interFontFamily)
    )

    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        val recentFiles = remember { mutableStateListOf<RecentFile>() }
        var currentDocument by remember { mutableStateOf<PdfDocument?>(null) }
        var currentDocumentName by remember { mutableStateOf("") }
        var initialPageToOpen by remember { mutableStateOf<Int?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            try {
                val loaded = RecentFilesStore.loadRecentFilesList()
                recentFiles.clear()
                if (loaded.isEmpty()) {
                    val bytes = Res.readBytes("files/sample.pdf")
                    val sampleFile = RecentFile(
                        id = "project_overview",
                        name = "Project_Overview.pdf",
                        lastOpened = "2 minutes ago",
                        size = "1.2 MB",
                        isSample = true,
                        fileBytes = bytes
                    )
                    recentFiles.add(sampleFile)
                    RecentFilesStore.saveRecentFilesList(recentFiles)
                } else {
                    recentFiles.addAll(loaded)
                }
                
                // Asynchronously render thumbnails for all loaded files
                recentFiles.forEach { file ->
                    scope.launch {
                        try {
                            val bytes = file.fileBytes ?: if (file.isSample) Res.readBytes("files/sample.pdf") else null
                            if (bytes != null) {
                                val factory = PdfDocumentFactory()
                                val doc = factory.open(bytes)
                                val bitmap = doc.renderPage(0, 180, 240)
                                val idx = recentFiles.indexOfFirst { it.id == file.id }
                                if (idx != -1) {
                                    recentFiles[idx] = recentFiles[idx].copy(thumbnail = bitmap.asImageBitmap())
                                }
                                doc.close()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val pickFileLauncher = io.github.vinceglb.filekit.compose.rememberFilePickerLauncher(
            type = io.github.vinceglb.filekit.core.PickerType.File(extensions = listOf("pdf")),
            title = "Select PDF File"
        ) { platformFile ->
            if (platformFile != null) {
                scope.launch {
                    try {
                        val bytes = platformFile.readBytes()
                        val name = platformFile.name
                        val sizeFormatted = formatSize(bytes.size)
                        
                        // Check if file already exists in recents (same name and size)
                        val existing = recentFiles.find { it.name == name && it.size == sizeFormatted }
                        val fileToOpen = if (existing != null) {
                            val updated = existing.copy(
                                lastOpened = "Just now",
                                fileBytes = bytes,
                                inRecents = true
                            )
                            recentFiles.remove(existing)
                            recentFiles.add(0, updated)
                            updated
                        } else {
                            val newFile = RecentFile(
                                id = name + "_" + bytes.size,
                                name = name,
                                lastOpened = "Just now",
                                size = sizeFormatted,
                                fileBytes = bytes
                            )
                            recentFiles.add(0, newFile)
                            newFile
                        }
                        RecentFilesStore.saveRecentFilesList(recentFiles)
                        
                        val factory = PdfDocumentFactory()
                        currentDocument = factory.open(bytes)
                        currentDocumentName = fileToOpen.id
                        initialPageToOpen = null
                        
                        // Render thumbnail in background
                        val doc = factory.open(bytes)
                        val bitmap = doc.renderPage(0, 180, 240)
                        val idx = recentFiles.indexOfFirst { it.id == fileToOpen.id }
                        if (idx != -1) {
                            recentFiles[idx] = recentFiles[idx].copy(thumbnail = bitmap.asImageBitmap())
                        }
                        doc.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        error = e.message ?: "Error opening file"
                    }
                }
            }
        }
        val pickFile: () -> Unit = { pickFileLauncher.launch() }

        if (error != null) {
            AlertDialog(
                onDismissRequest = { error = null },
                title = { Text("Error") },
                text = { Text(error ?: "") },
                confirmButton = {
                    TextButton(onClick = { error = null }) {
                        Text("OK")
                    }
                }
            )
        }

        if (currentDocument != null) {
            PdfViewer(
                document = currentDocument!!,
                documentId = currentDocumentName,
                initialPage = initialPageToOpen,
                onClose = {
                    currentDocument?.close()
                    currentDocument = null
                    initialPageToOpen = null
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            HomeScreen(
                recentFiles = recentFiles,
                onOpenFile = { recentFile, page ->
                    scope.launch {
                        try {
                            val bytesToOpen = recentFile.fileBytes ?: Res.readBytes("files/sample.pdf")
                            val factory = PdfDocumentFactory()
                            currentDocument = factory.open(bytesToOpen)
                            currentDocumentName = recentFile.id
                            initialPageToOpen = page
                            
                            // Move the opened file to the top of the list, update lastOpened, and mark inRecents = true
                            val existing = recentFiles.find { it.id == recentFile.id }
                            if (existing != null) {
                                val updated = existing.copy(lastOpened = "Just now", inRecents = true)
                                recentFiles.remove(existing)
                                recentFiles.add(0, updated)
                                RecentFilesStore.saveRecentFilesList(recentFiles)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            error = e.message ?: "Error opening file"
                        }
                    }
                },
                onRemoveFromRecents = { recentFile ->
                    val idx = recentFiles.indexOfFirst { it.id == recentFile.id }
                    if (idx != -1) {
                        recentFiles[idx] = recentFiles[idx].copy(inRecents = false)
                        RecentFilesStore.saveRecentFilesList(recentFiles)
                    }
                },
                onDeleteDocument = { recentFile ->
                    recentFiles.removeAll { it.id == recentFile.id }
                    RecentFilesStore.deleteRecentFile(recentFile.id)
                    RecentFilesStore.saveRecentFilesList(recentFiles)
                },
                onTriggerFilePicker = pickFile,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun formatSize(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024
    if (kb < 1024) return "$kb KB"
    val mb = kb / 1024
    return "${mb}.${(kb % 1024) / 100} MB"
}