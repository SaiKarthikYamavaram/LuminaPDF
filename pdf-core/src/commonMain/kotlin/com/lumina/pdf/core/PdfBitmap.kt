package com.lumina.pdf.core

interface PdfBitmap {
    val width: Int
    val height: Int
    
    /**
     * Platform-specific implementation must clean up resources when closed.
     */
    fun close()
}
