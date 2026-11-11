package com.lumina.pdf

import android.content.Context
import com.lumina.pdf.renderer.initPdfRenderer

object AndroidContext {
    var applicationContext: Context? = null
        set(value) {
            field = value
            value?.let(::initPdfRenderer)
        }
}
