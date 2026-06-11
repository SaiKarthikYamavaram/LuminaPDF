package com.lumina.pdf

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class FilePicker actual constructor() {
    @Composable
    actual fun register(onFilePicked: (ByteArray, String) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: byteArrayOf()
                    
                    var fileName = "document.pdf"
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = c.getString(nameIndex)
                            }
                        }
                    }
                    onFilePicked(bytes, fileName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return { launcher.launch("application/pdf") }
    }
}
