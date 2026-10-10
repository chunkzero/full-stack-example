package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.ScorecardView
import com.chunkzero.window.Click
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player

data class ScorecardRow(
    val name: String,
    val strokes: List<Int?>,
    val isViewer: Boolean,
)

/** Up to [MAX_PLAYERS] rows over [pars].size holes; strokes are null for holes not yet played. */
class ScorecardMenu(
    player: Player,
    private val course: String,
    private val pars: List<Int>,
    initialRows: List<ScorecardRow>,
    private val onBack: () -> Unit,
    private val onClosed: (ScorecardMenu) -> Unit,
) : ScorecardView(player) {
    var rows: List<ScorecardRow> by state(initialRows)

    override fun course(): Component = text(course.uppercase())

    override fun holePar(index: Int): Component = text(pars.getOrNull(index)?.toString().orEmpty())

    override fun parTotal(): Component = text(pars.take(HOLE_PAR_SIZE).sum().toString())

    override fun playerName(index: Int): Component {
        val row = rows.getOrNull(index) ?: return text("")
        return text(row.name).decoration(TextDecoration.BOLD, row.isViewer)
    }

    override fun viewer(index: Int): Boolean = rows.getOrNull(index)?.isViewer == true

    override fun strokes(
        row: Int,
        column: Int,
    ): Component {
        val strokes = rows.getOrNull(row)?.strokes?.getOrNull(column) ?: return text("")
        val par = pars.getOrNull(column) ?: return text(strokes.toString())
        return text(strokes.toString(), toParColor(strokes - par))
    }

    override fun total(index: Int): Component {
        val strokes = rows.getOrNull(index)?.strokes.orEmpty()
        return if (strokes.all { it == null }) text("") else text(strokes.sumOf { it ?: 0 }.toString())
    }

    override fun score(index: Int): Component {
        val played =
            rows
                .getOrNull(index)
                ?.strokes
                .orEmpty()
                .withIndex()
                .filter { it.value != null }
        if (played.isEmpty()) return text("")
        val difference = played.sumOf { it.value!! - (pars.getOrNull(it.index) ?: 0) }
        return text(formatScoreToPar(difference), toParColor(difference))
    }

    override fun onBack(click: Click) = onBack.invoke()

    override fun onClose() = onClosed(this)

    private fun toParColor(difference: Int): TextColor? =
        when {
            difference < 0 -> UiColors.INK_GREEN
            difference > 0 -> UiColors.INK_RED
            else -> null
        }

    companion object {
        const val MAX_PLAYERS = PLAYER_NAME_SIZE
    }
}
