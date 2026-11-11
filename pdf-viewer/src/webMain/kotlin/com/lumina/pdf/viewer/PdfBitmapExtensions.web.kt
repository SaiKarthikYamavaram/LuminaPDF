package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.RgbaPdfBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    require(this is RgbaPdfBitmap) { "Expected RgbaPdfBitmap, got ${this::class}" }
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    return Image.makeRaster(info, pixels, width * 4).toComposeImageBitmap()
}
