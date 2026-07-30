package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.renderer.AndroidPdfBitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lumina.pdf.core.DiskCache
import java.io.ByteArrayOutputStream
import java.io.File

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    return if (this is AndroidPdfBitmap) {
        this.bitmap.asImageBitmap()
    } else {
        throw IllegalArgumentException("Expected AndroidPdfBitmap, got ${this::class}")
    }
}

actual fun PdfBitmap.toByteArray(): ByteArray? {
    val bmp = (this as? AndroidPdfBitmap)?.bitmap ?: return null
    // If it's a HARDWARE bitmap, we can't compress/read pixels directly.
    // So we copy it to a software ARGB_8888 bitmap first.
    val softwareBmp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
        bmp.config == Bitmap.Config.HARDWARE) {
        bmp.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bmp
    }
    
    val bos = ByteArrayOutputStream()
    softwareBmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
    
    if (softwareBmp != bmp) {
        softwareBmp.recycle()
    }
    return bos.toByteArray()
}

actual fun decodeByteArrayToPdfBitmap(bytes: ByteArray): PdfBitmap? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return AndroidPdfBitmap(bitmap)
}

class AndroidDiskCache(context: Context) : DiskCache {
    private val cacheDir = File(context.cacheDir, "lumina-pdf-cache").apply { mkdirs() }

    override suspend fun get(key: String): ByteArray? {
        val file = File(cacheDir, key)
        return if (file.exists()) file.readBytes() else null
    }

    override suspend fun put(key: String, data: ByteArray) {
        val file = File(cacheDir, key)
        file.writeBytes(data)
    }

    override suspend fun remove(key: String) {
        val file = File(cacheDir, key)
        if (file.exists()) file.delete()
    }

    override suspend fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}

@Composable
actual fun rememberDiskCache(): DiskCache {
    val context = LocalContext.current
    return remember(context) { AndroidDiskCache(context) }
}
