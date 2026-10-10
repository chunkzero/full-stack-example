package com.chunkzero.example.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/** The colors views pick at runtime; text without one keeps the color its slot is authored with. */
internal object UiColors {
    val GOLD: TextColor = TextColor.color(0xffd75e)
    val GREEN: TextColor = TextColor.color(0x6fdc8c)
    val RED: TextColor = TextColor.color(0xff6b6b)
    val MUTED: TextColor = TextColor.color(0xa9d9b5)
    val WHITE: TextColor = TextColor.color(0xffffff)

    /** Under and over par, written on the scorecard's paper. */
    val INK_GREEN: TextColor = TextColor.color(0x1f7a3a)
    val INK_RED: TextColor = TextColor.color(0xb3261e)
}

internal fun text(
    value: String,
    color: TextColor? = null,
): Component = Component.text(value, color)
