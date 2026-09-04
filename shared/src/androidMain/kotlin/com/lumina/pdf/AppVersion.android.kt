package com.lumina.pdf

actual fun appVersionLabel(): String {
    val context = AndroidContext.applicationContext ?: return ""
    return try {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        "v$versionName"
    } catch (e: Exception) {
        ""
    }
}
