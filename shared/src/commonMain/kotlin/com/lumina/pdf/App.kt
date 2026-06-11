package com.lumina.pdf

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.renderer.PdfDocumentFactory
import com.lumina.pdf.viewer.PdfViewer
import luminapdf.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme {
        var document by remember { mutableStateOf<PdfDocument?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                val bytes = Res.readBytes("files/sample.pdf")
                val factory = PdfDocumentFactory()
                document = factory.open(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
                error = e.message ?: "Unknown error loading PDF"
            }
        }

        if (error != null) {
            Text("Error: $error")
        } else if (document == null) {
            Text("Loading PDF...")
        } else {
            PdfViewer(
                document = document!!,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}