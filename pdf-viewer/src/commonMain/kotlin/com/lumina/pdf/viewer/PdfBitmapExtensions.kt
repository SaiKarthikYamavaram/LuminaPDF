package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import com.lumina.pdf.core.PdfBitmap

expect fun PdfBitmap.asImageBitmap(): ImageBitmap
