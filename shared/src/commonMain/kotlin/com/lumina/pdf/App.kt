package com.lumina.pdf

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.pdf.viewer.theme.luminaColorScheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfPasswordRequiredException
import com.lumina.pdf.core.PdfPasswordUnsupportedException
import com.lumina.pdf.renderer.PdfDocumentFactory
import com.lumina.pdf.viewer.AppLogoMark
import com.lumina.pdf.viewer.PdfViewer
import com.lumina.pdf.viewer.asImageBitmap
import kotlinx.coroutines.Dispatchers
import com.lumina.pdf.viewer.db.getCurrentTimeMillis
import com.lumina.pdf.viewer.db.rememberPdfDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger
import luminapdf.shared.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.ExperimentalResourceApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

// The document currently shown in the viewer. Two screens (home, viewer) don't need a
// navigation library: App() shows the viewer while this is non-null, home otherwise.
private class OpenDocument(
    val document: PdfDocument,
    val id: String,
    val title: String,
    val initialPage: Int?,
    val bytes: ByteArray
)

@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = luminaColorScheme(darkTheme)

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
        var openDocument by remember { mutableStateOf<OpenDocument?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var sampleFile by remember { mutableStateOf<RecentFile?>(null) }
        var recentsLoaded by remember { mutableStateOf(false) }
        val db = rememberPdfDatabase()
        val scope = rememberCoroutineScope()
        // Guards onOpenFile against rapid repeat taps: opening involves file I/O
        // (PdfDocumentFactory.open, saveRecentFilesList) with no loading indicator in between, so
        // a user tapping again before the first tap's effect is visible was firing onOpenFile
        // dozens of times in a few seconds - each one opening its own PdfDocument and pushing its
        // own viewer screen, none of which ever got a chance to settle.
        var isOpeningFile by remember { mutableStateOf(false) }

        // Password-protected PDF handling: when opening a document throws
        // PdfPasswordRequiredException, pendingPasswordRetry holds a closure that re-attempts
        // the exact same open with a newly entered password, so the dialog doesn't need to know
        // whether the file came from Recents or a fresh pick/drop.
        var pendingPasswordRetry by remember { mutableStateOf<(suspend (String) -> Unit)?>(null) }
        var passwordInput by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }

        // Fixed-duration splash: cold start otherwise jumps straight to an empty Recents list
        // before the disk read below resolves. Time-based rather than gated on that read, so a
        // slow disk never leaves the splash on screen indefinitely.
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(900)
            showSplash = false
        }

        LaunchedEffect(Unit) {
            // The bundled sample PDF is an explicit, honestly-labeled "try it" offer — never
            // injected into recents/lastOpened as if it were something the user opened.
            sampleFile = RecentFile(
                id = "sample_project_overview",
                name = "Sample.pdf",
                lastOpened = 0L,
                size = "1.2 MB",
                isSample = true
            )

            try {
                val loaded = withContext(Dispatchers.Default) { RecentFilesStore.loadRecentFilesList() }
                recentFiles.clear()
                recentFiles.addAll(loaded)
            } catch (e: Exception) {
                Logger.e(e) { "Failed to load recent files list" }
            }
            recentsLoaded = true

            // One at a time, so at most one PDF is held in memory while thumbnails render.
            // ponytail: re-rendered on every launch. Cache thumbnails on disk if startup work shows up.
            recentFiles.toList().forEach { file ->
                val thumbnail = readDocumentBytes(file)?.let { renderThumbnail(it) } ?: return@forEach
                val idx = recentFiles.indexOfFirst { it.id == file.id }
                if (idx != -1) {
                    recentFiles[idx] = recentFiles[idx].copy(thumbnail = thumbnail)
                }
            }
        }

        val exportLauncher = io.github.vinceglb.filekit.compose.rememberFileSaverLauncher { }
        val exportFile: (ByteArray, String) -> Unit = { bytes, name ->
            exportLauncher.launch(bytes, name.removeSuffix(".pdf"), "pdf")
        }

        val closeViewer: () -> Unit = {
            openDocument?.document?.close()
            openDocument = null
        }
        // Closes whatever was open before (e.g. a PDF dropped onto the viewer), so a replaced
        // document never leaks its native handle.
        val showViewer: (OpenDocument) -> Unit = { doc ->
            openDocument?.document?.close()
            openDocument = doc
        }

        suspend fun openRecentFile(recentFile: RecentFile, page: Int?, password: String?) {
            try {
                val bytesToOpen = readDocumentBytes(recentFile)
                if (bytesToOpen == null) {
                    error = "\"${recentFile.name}\" is no longer available. It may not have been saved on this device."
                    return
                }
                val document = PdfDocumentFactory().open(bytesToOpen, password)

                // Move the opened file to the top of the list, update lastOpened, and mark inRecents = true
                // (also covers the first time the sample file is opened, adding it honestly)
                val existing = recentFiles.find { it.id == recentFile.id }
                val updated = (existing ?: recentFile).copy(lastOpened = getCurrentTimeMillis(), inRecents = true)
                if (existing != null) {
                    recentFiles.remove(existing)
                }
                recentFiles.add(0, updated)
                RecentFilesStore.saveRecentFilesList(recentFiles)

                showViewer(OpenDocument(document, recentFile.id, recentFile.name, page, bytesToOpen))
                pendingPasswordRetry = null
                passwordError = null
            } catch (e: PdfPasswordRequiredException) {
                passwordInput = ""
                passwordError = if (e.wrongPasswordTried) e.message else null
                pendingPasswordRetry = { pw -> openRecentFile(recentFile, page, pw) }
            } catch (e: PdfPasswordUnsupportedException) {
                error = e.message
            } catch (e: Exception) {
                Logger.e(e) { "Failed to open recent file: ${recentFile.id}" }
                error = e.message ?: "Error opening file"
            }
        }

        val onOpenFile: (RecentFile, Int?) -> Unit = { recentFile, page ->
            if (!isOpeningFile) {
                isOpeningFile = true
                scope.launch {
                    try {
                        openRecentFile(recentFile, page, password = null)
                    } finally {
                        isOpeningFile = false
                    }
                }
            }
        }

        suspend fun openResolvedPickedFile(bytes: ByteArray, fileToOpen: RecentFile, password: String?) {
            try {
                val factory = PdfDocumentFactory()
                showViewer(OpenDocument(factory.open(bytes, password), fileToOpen.id, fileToOpen.name, null, bytes))
                pendingPasswordRetry = null
                passwordError = null

                val thumbnail = renderThumbnail(bytes, password)
                val idx = recentFiles.indexOfFirst { it.id == fileToOpen.id }
                if (idx != -1 && thumbnail != null) {
                    recentFiles[idx] = recentFiles[idx].copy(thumbnail = thumbnail)
                }
            } catch (e: PdfPasswordRequiredException) {
                passwordInput = ""
                passwordError = if (e.wrongPasswordTried) e.message else null
                pendingPasswordRetry = { pw -> openResolvedPickedFile(bytes, fileToOpen, pw) }
            } catch (e: PdfPasswordUnsupportedException) {
                error = e.message
            } catch (e: Exception) {
                Logger.e(e) { "Failed to open file: ${fileToOpen.name}" }
                error = e.message ?: "Error opening file"
            }
        }

        // Shared by the file picker and drag-and-drop: adds/updates a recent entry for the
        // given bytes, opens it in the viewer, and renders its thumbnail in the background.
        suspend fun openPickedFile(bytes: ByteArray, name: String) {
            // Checked before anything is stored, so a non-PDF never lands in the library.
            if (!looksLikePdf(bytes)) {
                error = "\"$name\" is not a valid PDF."
                return
            }
            val sizeFormatted = formatSize(bytes.size)

            val snapshot = recentFiles.toList()
            val existing = withContext(Dispatchers.Default) {
                findStoredDocument(snapshot, name, bytes, FilePersistence::loadRecentFile)
            }
            val fileToOpen = if (existing != null) {
                val updated = existing.copy(
                    lastOpened = getCurrentTimeMillis(),
                    byteCount = bytes.size,
                    inRecents = true
                )
                recentFiles.remove(existing)
                recentFiles.add(0, updated)
                updated
            } else {
                val newFile = RecentFile(
                    id = newDocumentId(bytes),
                    name = name,
                    lastOpened = getCurrentTimeMillis(),
                    size = sizeFormatted,
                    byteCount = bytes.size
                )
                recentFiles.add(0, newFile)
                newFile
            }
            withContext(Dispatchers.Default) { FilePersistence.saveRecentFile(fileToOpen.id, bytes) }
            RecentFilesStore.saveRecentFilesList(recentFiles)

            openResolvedPickedFile(bytes, fileToOpen, password = null)
        }

        // Waits for the stored list: importing earlier would save a list holding only this file
        // over everything already on disk.
        LaunchedEffect(recentsLoaded) {
            if (!recentsLoaded) return@LaunchedEffect
            snapshotFlow { ExternalDocument.pending }.filterNotNull().collect { load ->
                ExternalDocument.pending = null
                val loaded = withContext(Dispatchers.Default) { load() }
                if (loaded == null) {
                    error = "That file could not be opened."
                } else {
                    openPickedFile(loaded.second, loaded.first)
                }
            }
        }

        val pickFileLauncher = io.github.vinceglb.filekit.compose.rememberFilePickerLauncher(
            type = io.github.vinceglb.filekit.core.PickerType.File(extensions = listOf("pdf")),
            title = "Select PDF File"
        ) { platformFile ->
            if (platformFile != null) {
                scope.launch {
                    openPickedFile(platformFile.readBytes(), platformFile.name)
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

        if (pendingPasswordRetry != null) {
            AlertDialog(
                onDismissRequest = {
                    pendingPasswordRetry = null
                    passwordInput = ""
                    passwordError = null
                },
                title = { Text("Password required") },
                text = {
                    Column {
                        Text("This PDF is password protected. Enter the password to open it.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = null
                            },
                            label = { Text("Password") },
                            singleLine = true,
                            isError = passwordError != null,
                            supportingText = passwordError?.let { msg -> { Text(msg) } },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val retry = pendingPasswordRetry
                        if (retry != null) {
                            scope.launch { retry(passwordInput) }
                        }
                    }) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingPasswordRetry = null
                        passwordInput = ""
                        passwordError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        val dragAndDropTarget = remember {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    scope.launch {
                        val dropped = extractDroppedPdfBytes(event)
                        if (dropped != null) {
                            val (name, bytes) = dropped
                            openPickedFile(bytes, name)
                        }
                    }
                    return true
                }
            }
        }

        // Keeps the home screen's tab, search, sort and scroll while the viewer replaces it.
        val homeStateHolder = rememberSaveableStateHolder()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dragAndDropTarget)
        ) {
            Crossfade(targetState = showSplash, animationSpec = tween(300)) { splashVisible ->
                if (splashVisible) {
                    SplashScreen()
                    return@Crossfade
                }
                val doc = openDocument
                if (doc != null) {
                    // System back (Android) leaves the viewer, as the navigation stack did.
                    NavigationBackHandler(
                        state = rememberNavigationEventState(NavigationEventInfo.None),
                        onBackCompleted = closeViewer
                    )
                    key(doc) {
                        PdfViewer(
                            document = doc.document,
                            documentId = doc.id,
                            documentTitle = doc.title,
                            initialPage = doc.initialPage,
                            onClose = closeViewer,
                            onExport = { exportFile(doc.bytes, doc.title) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else homeStateHolder.SaveableStateProvider("home") {
                    HomeScreen(
                        recentFiles = recentFiles,
                        sampleFile = sampleFile,
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
                            db.deleteDocumentData(recentFile.id)
                        },
                        onEditDocument = { recentFile, newName, newFolder ->
                            val idx = recentFiles.indexOfFirst { it.id == recentFile.id }
                            if (idx != -1) {
                                recentFiles[idx] = recentFiles[idx].copy(name = newName, folder = newFolder)
                                RecentFilesStore.saveRecentFilesList(recentFiles)
                            }
                        },
                        onTriggerFilePicker = pickFile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogoMark(modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "LuminaPDF",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            )
        }
    }
}

// Same document means same bytes, so only same-sized candidates get read from disk. An entry whose
// stored copy went missing has nothing to compare against, so it falls back to name and displayed
// size, which lets a re-pick restore it.
internal fun findStoredDocument(
    files: List<RecentFile>,
    name: String,
    bytes: ByteArray,
    loadStored: (id: String) -> ByteArray?
): RecentFile? {
    val stored = files.filter { !it.isSample }
    return stored.find { it.byteCount == bytes.size && loadStored(it.id)?.contentEquals(bytes) == true }
        ?: stored.find { it.name == name && it.size == formatSize(bytes.size) && loadStored(it.id) == null }
}

private suspend fun readDocumentBytes(file: RecentFile): ByteArray? = try {
    if (file.isSample) Res.readBytes("files/sample.pdf")
    else withContext(Dispatchers.Default) { FilePersistence.loadRecentFile(file.id) }
} catch (e: Exception) {
    Logger.e(e) { "Failed to read document: ${file.id}" }
    null
}

private suspend fun renderThumbnail(bytes: ByteArray, password: String? = null): ImageBitmap? = try {
    val doc = PdfDocumentFactory().open(bytes, password)
    try {
        val width = 180
        doc.renderPage(0, width, (width * doc.pageAspectRatio(0)).toInt().coerceAtLeast(1)).asImageBitmap()
    } finally {
        doc.close()
    }
} catch (e: Exception) {
    Logger.e(e) { "Failed to render document thumbnail" }
    null
}

// Opaque on purpose: the id becomes a filename and a metadata column, so no part of it may come
// from a user-controlled filename.
// ponytail: 32-bit hash, two different PDFs of the exact same size could collide. Use SHA-256 if that ever shows up.
internal fun newDocumentId(bytes: ByteArray): String = "doc_${bytes.size}_${bytes.contentHashCode()}"

// The spec lets a PDF put a few junk bytes before its header, and readers tolerate up to 1024.
internal fun looksLikePdf(bytes: ByteArray): Boolean =
    bytes.copyOf(minOf(bytes.size, 1024)).decodeToString().contains("%PDF-")

internal fun formatSize(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024
    if (kb < 1024) return "$kb KB"
    val mb = kb / 1024
    return "${mb}.${(kb % 1024) * 10 / 1024} MB"
}