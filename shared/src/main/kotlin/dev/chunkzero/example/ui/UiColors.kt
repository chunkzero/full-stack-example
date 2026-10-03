package dev.chunkzero.example.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

internal object UiColors {
    val GOLD: TextColor = TextColor.color(0xffd75e)
    val GREEN: TextColor = TextColor.color(0x6fdc8c)
    val RED: TextColor = TextColor.color(0xff6b6b)
    val MUTED: TextColor = TextColor.color(0xa9d9b5)
    val WHITE: TextColor = TextColor.color(0xffffff)
    val DIM: TextColor = TextColor.color(0x5f7a67)
}

internal fun text(
    value: String,
    color: TextColor = UiColors.WHITE,
): Component = Component.text(value, color)

internal fun fit(
    value: String,
    maxChars: Int,
): String = if (value.length <= maxChars) value else value.take(maxChars - 1) + "."
