@file:OptIn(UnsafeWasmMemoryApi::class)

package com.lumina.pdf.renderer

import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

// Copied through Wasm linear memory: crossing the JS boundary once per byte is far too slow for
// page-sized pixel buffers.
@JsFun("(address, size) => new Int8Array(wasmExports.memory.buffer, address, size).slice()")
private external fun copyFromWasmMemory(address: Int, size: Int): JsAny

@JsFun("(source, address) => new Int8Array(wasmExports.memory.buffer, address, source.length).set(source)")
private external fun copyToWasmMemory(source: JsAny, address: Int)

@JsFun("(source) => source.length")
private external fun jsLength(source: JsAny): Int

internal actual fun ByteArray.toJsBytes(): JsAny = withScopedMemoryAllocator { allocator ->
    val pointer = allocator.allocate(size)
    forEachIndexed { index, byte -> (pointer + index).storeByte(byte) }
    copyFromWasmMemory(pointer.address.toInt(), size)
}

internal actual fun JsAny.toKotlinBytes(): ByteArray = withScopedMemoryAllocator { allocator ->
    val size = jsLength(this)
    val pointer = allocator.allocate(size)
    copyToWasmMemory(this, pointer.address.toInt())
    ByteArray(size) { (pointer + it).loadByte() }
}
