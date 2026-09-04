package com.lumina.pdf

import platform.Foundation.NSBundle

actual fun appVersionLabel(): String {
    val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
    return version?.let { "v$it" } ?: ""
}
