package com.lumina.pdf

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

// Kotzilla only generates io.kotzilla.generated.monitoring() for Android, iOS and JVM
// (see doc.kotzilla.io) — js/wasmJs get a no-op actual so commonMain still compiles there.
expect fun KoinApplication.applyKotzillaMonitoring()

fun initKoin() {
    startKoin {
        // no modules yet
        applyKotzillaMonitoring()
    }
}
