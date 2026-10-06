package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.WindowDefinitions
import com.chunkzero.window.WindowScope
import com.chunkzero.window.WindowView
import com.chunkzero.window.multistom.MultistomHost
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

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
) : WindowView<ItemStack>(WindowDefinitions.scorecard, MultistomHost.of(player)) {
    var rows: List<ScorecardRow> by state(initialRows)

    override fun WindowScope<ItemStack>.bind() {
        slot("course") { text(course.uppercase(), UiColors.GOLD) }
        slot("par_total") { text(pars.take(HOLES).sum().toString(), UiColors.GOLD) }
        slot("par_score") { text("") }
        for (hole in 1..HOLES) {
            slot("par_h$hole") { text(pars.getOrNull(hole - 1)?.toString() ?: "", UiColors.GOLD) }
        }
        for (i in 0 until MAX_PLAYERS) {
            slot("p${i}_name") { nameOf(rows.getOrNull(i)) }
            slot("p${i}_total") { totalOf(rows.getOrNull(i)) }
            slot("p${i}_score") { scoreOf(rows.getOrNull(i)) }
            for (hole in 1..HOLES) {
                slot("p${i}_h$hole") { strokesOf(rows.getOrNull(i), hole - 1) }
            }
        }
        button("back") { onBack() }
    }

    override fun onClose() = onClosed(this)

    private fun nameOf(row: ScorecardRow?): Component {
        row ?: return text("")
        return text(fit(row.name, NAME_WIDTH, UiFont.SMALL_CAPS), if (row.isViewer) UiColors.GOLD else UiColors.WHITE)
    }

    private fun strokesOf(
        row: ScorecardRow?,
        index: Int,
    ): Component {
        val strokes = row?.strokes?.getOrNull(index) ?: return text("")
        val par = pars.getOrNull(index) ?: return text(strokes.toString())
        return text(
            strokes.toString(),
            when {
                strokes < par -> UiColors.GREEN
                strokes > par -> UiColors.RED
                else -> UiColors.WHITE
            },
        )
    }

    private fun totalOf(row: ScorecardRow?): Component {
        row ?: return text("")
        return if (row.strokes.all { it == null }) text("") else text(row.strokes.sumOf { it ?: 0 }.toString())
    }

    private fun scoreOf(row: ScorecardRow?): Component {
        row ?: return text("")
        val played = row.strokes.withIndex().filter { it.value != null }
        if (played.isEmpty()) return text("")
        val difference = played.sumOf { it.value!! - (pars.getOrNull(it.index) ?: 0) }
        return scoreToParComponent(difference)
    }

    companion object {
        const val MAX_PLAYERS = 8
        private const val HOLES = 9
        private const val NAME_WIDTH = 44
    }
}
