package com.lumina.pdf

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// A PDF handed to the app from outside: "Open with" on Android, a file argument on desktop.
// Holds a loader rather than bytes, so App() does the read off the main thread.
object ExternalDocument {
    internal var pending by mutableStateOf<(suspend () -> Pair<String, ByteArray>?)?>(null)

    fun open(load: suspend () -> Pair<String, ByteArray>?) {
        pending = load
    }
}
