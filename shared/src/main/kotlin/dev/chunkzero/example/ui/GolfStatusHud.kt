package dev.chunkzero.example.ui

import dev.chunkzero.example.ui.generated.GolfHud
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/** Round HUD. [power] is 0..1 while the meter is charging and null otherwise. */
class GolfStatusHud : GolfHud() {
    var hole: Int by state(1)
    var holeName: String by state("")
    var par: Int by state(0)
    var stroke: Int by state(0)
    var power: Double? by state(null)
    var status: Component? by state(null)

    override fun hole(): Component =
        text(if (holeName.isEmpty()) "HOLE $hole" else "HOLE $hole  ·  ${fit(holeName, 18)}", UiColors.GOLD)

    override fun par(): Component = text("PAR $par")

    override fun stroke(): Component = text(stroke.toString())

    override fun status(): Component = status ?: text("")

    override fun power(): Component {
        val charge = power ?: return text("")
        val filled = Math.round(charge.coerceIn(0.0, 1.0) * METER_SEGMENTS).toInt()
        return segments(filled, METER_SEGMENTS) { meterColor(it.toDouble() / (METER_SEGMENTS - 1)) }
    }

    private fun meterColor(position: Double): TextColor =
        if (position < 0.5) {
            TextColor.lerp((position * 2).toFloat(), UiColors.GREEN, YELLOW)
        } else {
            TextColor.lerp(((position - 0.5) * 2).toFloat(), YELLOW, UiColors.RED)
        }

    private companion object {
        const val METER_SEGMENTS = 12
        val YELLOW: TextColor = TextColor.color(0xffe14d)
    }
}
