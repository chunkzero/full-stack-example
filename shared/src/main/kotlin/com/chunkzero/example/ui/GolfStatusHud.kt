package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.GolfHud
import net.kyori.adventure.text.Component

enum class StatusKind {
    INFO,
    GOOD,
    BAD,
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

/** Round HUD, composed for the action bar. Shader HUDs draw text in its authored colour, so the status picks a tone. */
class GolfStatusHud(
    private val state: GolfHudState,
) : GolfHud() {
    override fun hole(): Component = text("HOLE ${state.hole}")

    override fun holeName(): Component = text(state.holeName)

    override fun par(): Component = text("Par ${state.par}")

    override fun stroke(): Component = text(state.stroke.toString())

    override fun status(): Component = text(state.status?.text.orEmpty())

    override fun statusTone(): StatusTone =
        when (state.status?.kind) {
            StatusKind.GOOD -> StatusTone.GOOD
            StatusKind.BAD -> StatusTone.BAD
            StatusKind.INFO, null -> StatusTone.INFO
        }

    override fun power(index: Int): Power {
        val power = state.power ?: return Power.IDLE
        val lit = Math.round(power.coerceIn(0.0, 1.0) * POWER_SIZE).toInt()
        return if (index < lit) Power.LIT else Power.TRACK
    }
}
