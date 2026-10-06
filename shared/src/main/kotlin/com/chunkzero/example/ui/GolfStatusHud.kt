package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.WindowHudDefinitions
import com.chunkzero.window.HudScope
import com.chunkzero.window.HudView
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

/** What the golf HUD shows, read on every render. [power] is 0..1 while the meter is charging and null otherwise. */
interface GolfHudState {
    val hole: Int
    val holeName: String
    val par: Int
    val stroke: Int
    val power: Double?
    val status: GolfStatus?
}

/**
 * Round HUD, composed for the action bar. Shader HUDs draw each slot in its authored colour, so the status colour
 * comes from [GolfStatus.kind] and the power meter is a row of fixed-colour segment slots over a dim track.
 */
class GolfStatusHud(
    private val state: GolfHudState,
) : HudView(WindowHudDefinitions.golf) {
    override fun HudScope.bind() {
        slot("hole") { holeText() }
        slot("par") { text("Par ${state.par}") }
        slot("stroke") { text(state.stroke.toString()) }
        for (kind in StatusKind.entries) {
            slot(kind.slot) {
                state.status?.takeIf { it.kind == kind }?.let { text(fit(it.text, STATUS_WIDTH, UiFont.SMALL_CAPS)) }
                    ?: text("")
            }
        }
        for (i in 0 until METER_SEGMENTS) {
            slot("power_$i") { if (i < filledSegments()) text("█") else text("") }
            slot("power_track_$i") { if (state.power != null && i >= filledSegments()) text("█") else text("") }
        }
    }

    private fun holeText(): Component {
        val prefix = "HOLE ${state.hole}"
        if (state.holeName.isEmpty()) return text(prefix)
        val separator = "  ·  "
        val budget = HOLE_WIDTH - UiFont.VANILLA.width(prefix + separator, bold = true)
        return text(prefix + separator + fit(state.holeName, budget, UiFont.VANILLA, bold = true))
    }

    private fun filledSegments(): Int =
        Math.round((state.power ?: return 0).coerceIn(0.0, 1.0) * METER_SEGMENTS).toInt()

    private companion object {
        const val METER_SEGMENTS = 12
        const val HOLE_WIDTH = 150
        const val STATUS_WIDTH = 132
    }
}
