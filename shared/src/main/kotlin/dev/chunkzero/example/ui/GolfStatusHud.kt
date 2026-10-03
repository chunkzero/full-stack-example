package dev.chunkzero.example.ui

import dev.oglass.window.HudScope
import dev.oglass.window.HudView
import net.kyori.adventure.text.Component

enum class StatusKind(
    internal val slot: String,
) {
    INFO("status_info"),
    GOOD("status_good"),
    BAD("status_bad"),
}

data class GolfStatus(
    val text: String,
    val kind: StatusKind = StatusKind.INFO,
)

/**
 * Round HUD. Shader HUDs draw each slot in its authored colour, so the status colour comes from
 * [GolfStatus.kind] and the power meter is a row of fixed-colour segment slots over a dim track.
 *
 * [power] is 0..1 while the meter is charging and null otherwise.
 */
class GolfStatusHud : HudView("golf") {
    var hole: Int by state(1)
    var holeName: String by state("")
    var par: Int by state(0)
    var stroke: Int by state(0)
    var power: Double? by state(null)
    var status: GolfStatus? by state(null)

    override fun HudScope.bind() {
        slot("hole") { holeText() }
        slot("par") { text("Par $par") }
        slot("stroke") { text(stroke.toString()) }
        for (kind in StatusKind.entries) {
            slot(kind.slot) {
                status?.takeIf { it.kind == kind }?.let { text(fit(it.text, STATUS_WIDTH, UiFont.SMALL_CAPS)) }
                    ?: text("")
            }
        }
        for (i in 0 until METER_SEGMENTS) {
            slot("power_$i") { if (i < filledSegments()) text("█") else text("") }
            slot("power_track_$i") { if (power != null && i >= filledSegments()) text("█") else text("") }
        }
    }

    private fun holeText(): Component {
        val prefix = "HOLE $hole"
        if (holeName.isEmpty()) return text(prefix)
        val separator = "  ·  "
        val budget = HOLE_WIDTH - UiFont.VANILLA.width(prefix + separator, bold = true)
        return text(prefix + separator + fit(holeName, budget, UiFont.VANILLA, bold = true))
    }

    private fun filledSegments(): Int = Math.round((power ?: return 0).coerceIn(0.0, 1.0) * METER_SEGMENTS).toInt()

    private companion object {
        const val METER_SEGMENTS = 12
        const val HOLE_WIDTH = 150
        const val STATUS_WIDTH = 132
    }
}
