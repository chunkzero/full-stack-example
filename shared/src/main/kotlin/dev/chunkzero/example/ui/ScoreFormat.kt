package dev.chunkzero.example.ui

import net.kyori.adventure.text.Component

/** Formats a stroke difference as golf notation: "E", "+2" or "-1". */
fun formatScoreToPar(difference: Int): String =
    when {
        difference == 0 -> "E"
        difference > 0 -> "+$difference"
        else -> difference.toString()
    }

internal fun scoreToParComponent(difference: Int): Component =
    text(
        formatScoreToPar(difference),
        when {
            difference < 0 -> UiColors.GREEN
            difference > 0 -> UiColors.RED
            else -> UiColors.WHITE
        },
    )
