package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfBitmap
import java.awt.image.BufferedImage

class JvmPdfBitmap(val image: BufferedImage) : PdfBitmap {
    override val width: Int get() = image.width
    override val height: Int get() = image.height

    override fun close() {
        // BufferedImage does not require explicit release
    }
}
