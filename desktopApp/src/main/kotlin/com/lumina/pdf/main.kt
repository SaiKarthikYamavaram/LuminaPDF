package com.lumina.pdf

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    initKoin()
    application {
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1280.dp, 800.dp)
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "LuminaPDF",
            state = windowState
        ) {
            App()
        }
    }
}
