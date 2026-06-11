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
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            try {
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
                
                // Render thumbnail for sample PDF
                try {
                    val factory = PdfDocumentFactory()
                    val doc = factory.open(bytes)
                    val bitmap = doc.renderPage(0, 180, 240)
                    val idx = recentFiles.indexOfFirst { it.id == "project_overview" }
                    if (idx != -1) {
                        recentFiles[idx] = sampleFile.copy(thumbnail = bitmap.asImageBitmap())
                    }
                    doc.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            recentFiles.add(RecentFile("mock_2", "Vendor_Agreement.pdf", "5 minutes ago", "4.8 MB"))
            recentFiles.add(RecentFile("mock_3", "Annual_Report_Q3.pdf", "3 minutes ago", "8.4 MB"))
            recentFiles.add(RecentFile("mock_4", "Product_Manual_v2.pdf", "5 minutes ago", "14.2 MB"))
            recentFiles.add(RecentFile("mock_5", "Team_Meeting_Agendas.pdf", "3 minutes ago", "2.1 MB"))
            recentFiles.add(RecentFile("mock_6", "Creative_Brief.pdf", "5 minutes ago", "1.8 MB"))
            recentFiles.add(RecentFile("mock_7", "Event_Schedule.pdf", "5 minutes ago", "3.6 MB"))
        }

        val filePicker = remember { FilePicker() }
        val pickFile = filePicker.register { bytes, name ->
            val newFile = RecentFile(
                id = name + "_" + (0..1000000).random().toString(),
                name = name,
                lastOpened = "Just now",
                size = formatSize(bytes.size),
                fileBytes = bytes
            )
            recentFiles.add(0, newFile)
            
            scope.launch {
                try {
                    val factory = PdfDocumentFactory()
                    currentDocument = factory.open(bytes)
                    currentDocumentName = name
                    
                    // Render thumbnail in background
                    val doc = factory.open(bytes)
                    val bitmap = doc.renderPage(0, 180, 240)
                    val idx = recentFiles.indexOfFirst { it.id == newFile.id }
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
                onClose = {
                    currentDocument?.close()
                    currentDocument = null
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            HomeScreen(
                recentFiles = recentFiles,
                onOpenFile = { recentFile ->
                    scope.launch {
                        try {
                            val bytesToOpen = recentFile.fileBytes ?: Res.readBytes("files/sample.pdf")
                            val factory = PdfDocumentFactory()
                            currentDocument = factory.open(bytesToOpen)
                            currentDocumentName = recentFile.name
                        } catch (e: Exception) {
                            e.printStackTrace()
                            error = e.message ?: "Error opening file"
                        }
                    }
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