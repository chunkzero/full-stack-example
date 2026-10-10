package com.chunkzero.example.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/** Formats a stroke difference as golf notation: "E", "+2" or "-1". */
fun formatScoreToPar(difference: Int): String =
    when {
        difference == 0 -> "E"
        difference > 0 -> "+$difference"
        else -> difference.toString()
    }

/** A round as "28 (-2)": the strokes in the slot's color, then the score to par in green under par or red over it. */
internal fun formatRound(
    strokes: Int,
    par: Int,
): Component {
    val difference = strokes - par
    val color: TextColor? =
        when {
            difference < 0 -> UiColors.GREEN
            difference > 0 -> UiColors.RED
            else -> null
        }
    return text("$strokes ").append(text("(${formatScoreToPar(difference)})", color))
}
