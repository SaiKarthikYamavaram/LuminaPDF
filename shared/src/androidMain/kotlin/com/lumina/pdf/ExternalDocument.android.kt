package com.lumina.pdf

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import co.touchlab.kermit.Logger

fun handleViewIntent(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_VIEW) return
    val uri = intent.data ?: return
    // MainActivity is exported, so any app can send any URI. A file:// URI could point into this
    // app's private storage, so only content URIs are accepted.
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) return

    val resolver = context.applicationContext.contentResolver
    ExternalDocument.open {
        try {
            val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            bytes?.let { (displayName ?: "Document.pdf") to it }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to read document from another app" }
            null
        } catch (e: OutOfMemoryError) {
            Logger.e(e) { "Document from another app is too large to read" }
            null
        }
    }
}
