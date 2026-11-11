package com.lumina.pdf

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

// Kotzilla only generates io.kotzilla.generated.monitoring() for Android, iOS and JVM
// (see doc.kotzilla.io) — js/wasmJs get a no-op actual so commonMain still compiles there.
expect fun KoinApplication.applyKotzillaMonitoring()

// initKoin() runs from MainActivity.onCreate() rather than an Application class, so it can
// re-run in an already-warm process (e.g. the user backs all the way out, then relaunches from
// the icon before the process dies) — guard against starting Koin twice in that case, which
// otherwise throws KoinApplicationAlreadyStartedException.
fun initKoin() {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        // no modules yet
        applyKotzillaMonitoring()
    }
}
