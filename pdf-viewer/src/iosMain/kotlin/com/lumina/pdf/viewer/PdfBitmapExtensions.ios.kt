package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import com.lumina.pdf.core.PdfBitmap

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    throw NotImplementedError("iOS rendering not yet implemented")
}
