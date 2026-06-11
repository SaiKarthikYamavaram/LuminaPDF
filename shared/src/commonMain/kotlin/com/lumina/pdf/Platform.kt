package com.lumina.pdf

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform