package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.renderer.JvmPdfBitmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lumina.pdf.core.DiskCache
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    return if (this is JvmPdfBitmap) {
        this.image.toComposeImageBitmap()
    } else {
        throw IllegalArgumentException("Expected JvmPdfBitmap, got ${this::class}")
    }
}

actual fun PdfBitmap.toByteArray(): ByteArray? {
    val image = (this as? JvmPdfBitmap)?.image ?: return null
    val bos = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", bos)
    return bos.toByteArray()
}

actual fun decodeByteArrayToPdfBitmap(bytes: ByteArray): PdfBitmap? {
    val bis = ByteArrayInputStream(bytes)
    val image = ImageIO.read(bis) ?: return null
    return JvmPdfBitmap(image)
}

class JvmDiskCache : DiskCache {
    private val cacheDir = File(System.getProperty("java.io.tmpdir"), "lumina-pdf-cache").apply { mkdirs() }

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
    return remember { JvmDiskCache() }
}
