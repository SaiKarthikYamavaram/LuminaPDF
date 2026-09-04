package com.lumina.pdf

import org.koin.core.context.startKoin

// Kotzilla's monitoring() call goes inside this block once the app is registered
// on Kotzilla and shared/kotzilla.json is in place (see doc.kotzilla.io).
fun initKoin() {
    startKoin {
        // no modules yet
    }
}
