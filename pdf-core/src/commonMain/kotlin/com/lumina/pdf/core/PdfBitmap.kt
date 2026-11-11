package com.lumina.pdf.core

interface PdfBitmap {
    val width: Int
    val height: Int
    
    /**
     * Platform-specific implementation must clean up resources when closed.
     */
    fun close()
}

// Premultiplied RGBA pixels, for platforms that hand back raw memory (web, iOS).
class RgbaPdfBitmap(
    override val width: Int,
    override val height: Int,
    val pixels: ByteArray
) : PdfBitmap {
    override fun close() {
        // Plain heap memory.
    }
}
