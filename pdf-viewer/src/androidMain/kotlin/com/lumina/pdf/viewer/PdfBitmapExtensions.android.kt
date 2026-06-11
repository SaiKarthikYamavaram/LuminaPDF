package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.renderer.AndroidPdfBitmap

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    return if (this is AndroidPdfBitmap) {
        this.bitmap.asImageBitmap()
    } else {
        throw IllegalArgumentException("Expected AndroidPdfBitmap, got ${this::class}")
    }
}
