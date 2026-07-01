package com.lumina.pdf.core

data class CharacterBox(
    val char: Char,
    val pageIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
