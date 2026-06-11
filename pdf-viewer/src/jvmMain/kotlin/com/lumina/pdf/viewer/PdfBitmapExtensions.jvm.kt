package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.renderer.JvmPdfBitmap

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    return if (this is JvmPdfBitmap) {
        this.image.toComposeImageBitmap()
    } else {
        throw IllegalArgumentException("Expected JvmPdfBitmap, got ${this::class}")
    }
}
