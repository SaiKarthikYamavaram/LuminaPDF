package com.lumina.pdf

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import co.touchlab.kermit.Logger
import luminapdf.shared.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.ExperimentalResourceApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.CurrentScreen

class AppHomeScreen(
    private val recentFiles: SnapshotStateList<RecentFile>,
    private val onOpenFile: (RecentFile, Int?, Navigator) -> Unit,
    private val onRemoveFromRecents: (RecentFile) -> Unit,
    private val onDeleteDocument: (RecentFile) -> Unit,
    private val onTriggerFilePicker: () -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        HomeScreen(
            recentFiles = recentFiles,
            onOpenFile = { recentFile, page -> onOpenFile(recentFile, page, navigator) },
            onRemoveFromRecents = onRemoveFromRecents,
            onDeleteDocument = onDeleteDocument,
            onTriggerFilePicker = onTriggerFilePicker,
            modifier = Modifier.fillMaxSize()
        )
    }
}

class AppPdfViewerScreen(
    private val document: PdfDocument,
    private val documentId: String,
    private val initialPage: Int?,
    private val onClose: () -> Unit
) : Screen {
    @Composable
    override fun Content() {
        PdfViewer(
            document = document,
            documentId = documentId,
            initialPage = initialPage,
            onClose = onClose,
            modifier = Modifier.fillMaxSize()
        )
    }
}

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
        var navigatorRef by remember { mutableStateOf<Navigator?>(null) }

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
                            Logger.e(e) { "Failed to load background thumbnail for recent file" }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to load recent files list" }
            }
        }

        val onOpenFile: (RecentFile, Int?, Navigator) -> Unit = { recentFile, page, navigator ->
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
                    
                    navigator.push(
                        AppPdfViewerScreen(
                            document = currentDocument!!,
                            documentId = recentFile.id,
                            initialPage = page,
                            onClose = {
                                currentDocument?.close()
                                currentDocument = null
                                initialPageToOpen = null
                                navigator.pop()
                            }
                        )
                    )
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to open recent file: ${recentFile.id}" }
                    error = e.message ?: "Error opening file"
                }
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
                        
                        val navigator = navigatorRef
                        if (navigator != null) {
                            navigator.push(
                                AppPdfViewerScreen(
                                    document = currentDocument!!,
                                    documentId = fileToOpen.id,
                                    initialPage = null,
                                    onClose = {
                                        currentDocument?.close()
                                        currentDocument = null
                                        initialPageToOpen = null
                                        navigator.pop()
                                    }
                                )
                            )
                        }
                        
                        // Render thumbnail in background
                        val doc = factory.open(bytes)
                        val bitmap = doc.renderPage(0, 180, 240)
                        val idx = recentFiles.indexOfFirst { it.id == fileToOpen.id }
                        if (idx != -1) {
                            recentFiles[idx] = recentFiles[idx].copy(thumbnail = bitmap.asImageBitmap())
                        }
                        doc.close()
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to open picked file: ${platformFile.name}" }
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

        Navigator(
            screen = AppHomeScreen(
                recentFiles = recentFiles,
                onOpenFile = onOpenFile,
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
                onTriggerFilePicker = pickFile
            )
        ) { navigator ->
            navigatorRef = navigator
            CurrentScreen()
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