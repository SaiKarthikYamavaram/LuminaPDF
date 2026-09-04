package com.lumina.pdf

import org.koin.core.KoinApplication

actual fun KoinApplication.applyKotzillaMonitoring() {
    // Kotzilla doesn't support the js target yet.
}
