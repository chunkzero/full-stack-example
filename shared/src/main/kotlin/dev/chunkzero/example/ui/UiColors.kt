package dev.chunkzero.example.ui

import dev.chunkzero.example.ui.generated.WindowFonts
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

/** Rendered width in GUI pixels, from the generated font metrics. Bold adds one pixel per glyph. */
internal fun textWidth(
    value: String,
    bold: Boolean = false,
): Int = value.sumOf { (WindowFonts.advances[it.toString()] ?: DEFAULT_ADVANCE) + if (bold) 1 else 0 }

/** Truncates [value] with a trailing "." so it renders within [maxWidth] pixels. */
internal fun fit(
    value: String,
    maxWidth: Int,
    bold: Boolean = false,
): String {
    if (textWidth(value, bold) <= maxWidth) return value
    var result = value
    while (result.isNotEmpty() && textWidth("$result.", bold) > maxWidth) result = result.dropLast(1)
    return "$result."
}

private const val DEFAULT_ADVANCE = 6
