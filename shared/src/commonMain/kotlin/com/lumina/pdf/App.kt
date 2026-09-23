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
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import kotlinx.coroutines.delay
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
    @Transient private val recentFiles: SnapshotStateList<RecentFile> = mutableStateListOf(),
    @Transient private val sampleFile: RecentFile? = null,
    @Transient private val onOpenFile: (RecentFile, Int?, Navigator) -> Unit = { _, _, _ -> },
    @Transient private val onRemoveFromRecents: (RecentFile) -> Unit = {},
    @Transient private val onDeleteDocument: (RecentFile) -> Unit = {},
    @Transient private val onEditDocument: (RecentFile, String, String) -> Unit = { _, _, _ -> },
    @Transient private val onTriggerFilePicker: () -> Unit = {}
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        HomeScreen(
            recentFiles = recentFiles,
            sampleFile = sampleFile,
            onOpenFile = { recentFile, page -> onOpenFile(recentFile, page, navigator) },
            onRemoveFromRecents = onRemoveFromRecents,
            onDeleteDocument = onDeleteDocument,
            onEditDocument = onEditDocument,
            onTriggerFilePicker = onTriggerFilePicker,
            modifier = Modifier.fillMaxSize()
        )
    }
}

class AppPdfViewerScreen(
    @Transient private val document: PdfDocument? = null,
    private val documentId: String,
    private val documentTitle: String,
    private val initialPage: Int?,
    @Transient private val onClose: () -> Unit = {},
    @Transient private val onExport: (() -> Unit)? = null
) : Screen {
    @Composable
    override fun Content() {
        val document = document ?: return
        PdfViewer(
            document = document,
            documentId = documentId,
            documentTitle = documentTitle,
            initialPage = initialPage,
            onClose = onClose,
            onExport = onExport,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
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
        var currentDocument by remember { mutableStateOf<PdfDocument?>(null) }
        var currentDocumentName by remember { mutableStateOf("") }
        var initialPageToOpen by remember { mutableStateOf<Int?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var sampleFile by remember { mutableStateOf<RecentFile?>(null) }
        val scope = rememberCoroutineScope()
        var navigatorRef by remember { mutableStateOf<Navigator?>(null) }
        // Guards onOpenFile against rapid repeat taps: opening involves file I/O
        // (PdfDocumentFactory.open, saveRecentFilesList) with no loading indicator in between, so
        // a user tapping again before the first tap's effect is visible was firing onOpenFile
        // dozens of times in a few seconds - each one opening its own PdfDocument and pushing its
        // own AppPdfViewerScreen, none of which ever got a chance to settle. Confirmed via
        // [TAP-DEBUG] logging: 21 invocations for one file in 4.5s of tapping.
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
            try {
                val loaded = RecentFilesStore.loadRecentFilesList()
                recentFiles.clear()
                recentFiles.addAll(loaded)

                // Asynchronously render thumbnails for all loaded files
                recentFiles.forEach { file ->
                    scope.launch {
                        try {
                            val bytes = file.fileBytes
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

            // Load the bundled sample PDF as an explicit, honestly-labeled "try it" offer —
            // never injected into recents/lastOpened as if it were something the user opened.
            try {
                sampleFile = RecentFile(
                    id = "sample_project_overview",
                    name = "Sample.pdf",
                    lastOpened = "",
                    size = "1.2 MB",
                    isSample = true,
                    fileBytes = Res.readBytes("files/sample.pdf")
                )
            } catch (e: Exception) {
                Logger.e(e) { "Failed to load bundled sample PDF" }
            }
        }

        val exportLauncher = io.github.vinceglb.filekit.compose.rememberFileSaverLauncher { }
        val exportFile: (ByteArray, String) -> Unit = { bytes, name ->
            exportLauncher.launch(bytes, name.removeSuffix(".pdf"), "pdf")
        }

        suspend fun openRecentFile(recentFile: RecentFile, page: Int?, navigator: Navigator, password: String?) {
            try {
                val bytesToOpen = recentFile.fileBytes
                if (bytesToOpen == null) {
                    error = "\"${recentFile.name}\" is no longer available. It may not have been saved on this device."
                    return
                }
                val factory = PdfDocumentFactory()
                currentDocument = factory.open(bytesToOpen, password)
                currentDocumentName = recentFile.id
                initialPageToOpen = page

                // Move the opened file to the top of the list, update lastOpened, and mark inRecents = true
                // (also covers the first time the sample file is opened, adding it honestly)
                val existing = recentFiles.find { it.id == recentFile.id }
                val updated = (existing ?: recentFile).copy(lastOpened = "Just now", inRecents = true)
                if (existing != null) {
                    recentFiles.remove(existing)
                }
                recentFiles.add(0, updated)
                RecentFilesStore.saveRecentFilesList(recentFiles)

                navigator.push(
                    AppPdfViewerScreen(
                        document = currentDocument!!,
                        documentId = recentFile.id,
                        documentTitle = recentFile.name,
                        initialPage = page,
                        onClose = {
                            currentDocument?.close()
                            currentDocument = null
                            initialPageToOpen = null
                            navigator.pop()
                        },
                        onExport = { exportFile(bytesToOpen, recentFile.name) }
                    )
                )
                pendingPasswordRetry = null
                passwordError = null
            } catch (e: PdfPasswordRequiredException) {
                passwordInput = ""
                passwordError = if (e.wrongPasswordTried) e.message else null
                pendingPasswordRetry = { pw -> openRecentFile(recentFile, page, navigator, pw) }
            } catch (e: PdfPasswordUnsupportedException) {
                error = e.message
            } catch (e: Exception) {
                Logger.e(e) { "Failed to open recent file: ${recentFile.id}" }
                error = e.message ?: "Error opening file"
            }
        }

        val onOpenFile: (RecentFile, Int?, Navigator) -> Unit = { recentFile, page, navigator ->
            if (isOpeningFile) {
                Logger.i { "[TAP-DEBUG] onOpenFile ignored (already opening): ${recentFile.name}" }
            } else {
                isOpeningFile = true
                Logger.i { "[TAP-DEBUG] onOpenFile invoked for ${recentFile.name}, bytes=${recentFile.fileBytes?.size}" }
                scope.launch {
                    try {
                        openRecentFile(recentFile, page, navigator, password = null)
                    } finally {
                        isOpeningFile = false
                    }
                }
            }
        }

        suspend fun openResolvedPickedFile(bytes: ByteArray, fileToOpen: RecentFile, password: String?) {
            try {
                val factory = PdfDocumentFactory()
                currentDocument = factory.open(bytes, password)
                currentDocumentName = fileToOpen.id
                initialPageToOpen = null

                val navigator = navigatorRef
                if (navigator != null) {
                    navigator.push(
                        AppPdfViewerScreen(
                            document = currentDocument!!,
                            documentId = fileToOpen.id,
                            documentTitle = fileToOpen.name,
                            initialPage = null,
                            onClose = {
                                currentDocument?.close()
                                currentDocument = null
                                initialPageToOpen = null
                                navigator.pop()
                            },
                            onExport = { exportFile(bytes, fileToOpen.name) }
                        )
                    )
                }

                // Render thumbnail in background
                val doc = factory.open(bytes, password)
                val bitmap = doc.renderPage(0, 180, 240)
                val idx = recentFiles.indexOfFirst { it.id == fileToOpen.id }
                if (idx != -1) {
                    recentFiles[idx] = recentFiles[idx].copy(thumbnail = bitmap.asImageBitmap())
                }
                doc.close()
                pendingPasswordRetry = null
                passwordError = null
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

            openResolvedPickedFile(bytes, fileToOpen, password = null)
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
                Navigator(
                    screen = AppHomeScreen(
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
                        },
                        onEditDocument = { recentFile, newName, newFolder ->
                            // RecentFilesStore's metadata format is "|"-delimited; a typed name could
                            // otherwise corrupt every row after it.
                            val sanitize = { text: String -> text.replace("|", " ").replace("\n", " ") }
                            val idx = recentFiles.indexOfFirst { it.id == recentFile.id }
                            if (idx != -1) {
                                recentFiles[idx] = recentFiles[idx].copy(name = sanitize(newName), folder = sanitize(newFolder))
                                RecentFilesStore.saveRecentFilesList(recentFiles)
                            }
                        },
                        onTriggerFilePicker = pickFile
                    )
                ) { navigator ->
                    navigatorRef = navigator
                    CurrentScreen()
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

internal fun formatSize(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024
    if (kb < 1024) return "$kb KB"
    val mb = kb / 1024
    return "${mb}.${(kb % 1024) / 100} MB"
}