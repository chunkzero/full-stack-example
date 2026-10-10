package com.chunkzero.example.ui

/** Formats a stroke difference as golf notation: "E", "+2" or "-1". */
fun formatScoreToPar(difference: Int): String =
    when {
        difference == 0 -> "E"
        difference > 0 -> "+$difference"
        else -> difference.toString()
    }
