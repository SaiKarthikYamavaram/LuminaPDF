package com.lumina.pdf.renderer

import android.graphics.Bitmap
import com.lumina.pdf.core.PdfBitmap

class AndroidPdfBitmap(val bitmap: Bitmap) : PdfBitmap {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    // Deliberately NOT calling bitmap.recycle() here. recycle() frees the native pixel buffer
    // immediately and synchronously, but Compose's RenderThread can still be mid-draw on a
    // display list recorded from an earlier frame that references this same bitmap - the gap
    // between "cache/state says this is evicted" and "the render thread has actually finished
    // with it" can't be closed by tighter bookkeeping on the state side (tried: pinning cache
    // entries still in use, deduping in-flight renders for the same tile - the crash persisted
    // through both). On API 24+, a software Bitmap's memory is accounted against the regular
    // Java heap and reclaimed by GC once nothing references it, same as any other object, which
    // cannot happen while it's still on screen - so simply not recycling removes this whole crash
    // class by construction, at the cost of memory being freed on GC's schedule instead of
    // immediately.
    override fun close() {}
}
