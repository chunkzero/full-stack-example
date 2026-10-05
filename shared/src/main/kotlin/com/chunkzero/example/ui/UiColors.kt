package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.WindowFonts
import com.chunkzero.window.manifest.FontMetricsEntry
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

/** The fonts the menus draw text with, measured from the generated font metrics. */
internal enum class UiFont(
    private val metrics: FontMetricsEntry,
) {
    VANILLA(FontMetricsEntry(WindowFonts.advances, boldAdvance = 1)),
    SMALL_CAPS(
        WindowFonts.metrics.entries
            .first { "small_caps/" in it.key }
            .value,
    ),
    ;

    /** Rendered width in GUI pixels. */
    fun width(
        value: String,
        bold: Boolean = false,
    ): Int = value.sumOf { (metrics.advances[it.toString()] ?: DEFAULT_ADVANCE) + if (bold) metrics.boldAdvance else 0 }
}

/** Truncates [value] with a trailing "." so it renders within [maxWidth] pixels. */
internal fun fit(
    value: String,
    maxWidth: Int,
    font: UiFont,
    bold: Boolean = false,
): String {
    if (font.width(value, bold) <= maxWidth) return value
    var result = value
    while (result.isNotEmpty() && font.width("$result.", bold) > maxWidth) result = result.dropLast(1)
    return "$result."
}

/**
 * Splits [value] over at most two lines of [maxWidth] pixels, breaking at the last space that lets the first line fit.
 * The second line is null when one line is enough, and truncated when two are not.
 */
internal fun wrap(
    value: String,
    maxWidth: Int,
    font: UiFont,
): Pair<String, String?> {
    if (font.width(value) <= maxWidth) return value to null
    val words = value.split(' ')
    for (count in words.size - 1 downTo 1) {
        val first = words.take(count).joinToString(" ")
        if (font.width(first) <= maxWidth) return first to fit(words.drop(count).joinToString(" "), maxWidth, font)
    }
    return fit(value, maxWidth, font) to null
}

private const val DEFAULT_ADVANCE = 6
