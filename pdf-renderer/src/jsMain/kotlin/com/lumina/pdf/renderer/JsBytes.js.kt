package com.lumina.pdf.renderer

// On the JS target a ByteArray already is an Int8Array.
internal actual fun ByteArray.toJsBytes(): JsAny = unsafeCast<JsAny>()

internal actual fun JsAny.toKotlinBytes(): ByteArray = unsafeCast<ByteArray>()
