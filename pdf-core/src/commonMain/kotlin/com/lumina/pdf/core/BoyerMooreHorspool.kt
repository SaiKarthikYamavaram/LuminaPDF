package com.lumina.pdf.core

object BoyerMooreHorspool {
    fun buildShiftTable(pattern: String, ignoreCase: Boolean): Map<Char, Int> {
        val table = mutableMapOf<Char, Int>()
        val m = pattern.length
        if (m == 0) return table
        
        for (i in 0 until m - 1) {
            val c = if (ignoreCase) pattern[i].lowercaseChar() else pattern[i]
            table[c] = m - 1 - i
        }
        return table
    }

    fun search(text: String, pattern: String, ignoreCase: Boolean = true): List<Int> {
        val matches = mutableListOf<Int>()
        val n = text.length
        val m = pattern.length
        if (m == 0 || n < m) return matches

        val shiftTable = buildShiftTable(pattern, ignoreCase)
        val cleanPattern = if (ignoreCase) pattern.lowercase() else pattern

        var shift = 0
        while (shift <= n - m) {
            var j = m - 1
            while (j >= 0) {
                val textChar = text[shift + j]
                val cleanTextChar = if (ignoreCase) textChar.lowercaseChar() else textChar
                if (cleanPattern[j] != cleanTextChar) {
                    break
                }
                j--
            }
            if (j < 0) {
                matches.add(shift)
                val lastCharInTextWindow = text[shift + m - 1]
                val cleanLastChar = if (ignoreCase) lastCharInTextWindow.lowercaseChar() else lastCharInTextWindow
                shift += shiftTable[cleanLastChar] ?: m
            } else {
                val lastCharInTextWindow = text[shift + m - 1]
                val cleanLastChar = if (ignoreCase) lastCharInTextWindow.lowercaseChar() else lastCharInTextWindow
                shift += shiftTable[cleanLastChar] ?: m
            }
        }
        return matches
    }
}
