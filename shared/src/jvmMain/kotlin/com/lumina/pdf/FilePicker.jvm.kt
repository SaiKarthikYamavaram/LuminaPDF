package com.lumina.pdf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class FilePicker actual constructor() {
    @Composable
    actual fun register(onFilePicked: (ByteArray, String) -> Unit): () -> Unit {
        val scope = rememberCoroutineScope()
        return {
            scope.launch {
                withContext(Dispatchers.IO) {
                    var pickedFile: File? = null
                    var pickedName: String? = null
                    
                    try {
                        SwingUtilities.invokeAndWait {
                            val frame = Frame()
                            val dialog = FileDialog(frame, "Select PDF File", FileDialog.LOAD).apply {
                                file = "*.pdf"
                                setFilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
                            }
                            dialog.isVisible = true
                            if (dialog.file != null) {
                                pickedFile = File(dialog.directory, dialog.file)
                                pickedName = dialog.file
                            }
                            frame.dispose()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    pickedFile?.let { file ->
                        try {
                            val bytes = file.readBytes()
                            onFilePicked(bytes, pickedName ?: file.name)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
