package com.lumina.pdf.core

data class OutlineItem(
    val title: String,
    val pageNumber: Int,
    val children: List<OutlineItem> = emptyList()
)
