package com.lumina.pdf.renderer

import android.graphics.Bitmap
import com.lumina.pdf.core.PdfBitmap

class AndroidPdfBitmap(val bitmap: Bitmap) : PdfBitmap {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    override fun close() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
