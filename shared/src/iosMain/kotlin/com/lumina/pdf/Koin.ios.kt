package com.lumina.pdf

import io.kotzilla.generated.monitoring
import org.koin.core.KoinApplication

actual fun KoinApplication.applyKotzillaMonitoring() {
    monitoring()
}
