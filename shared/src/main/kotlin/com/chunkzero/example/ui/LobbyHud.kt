package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.LobbyCourseHud
import com.chunkzero.example.ui.generated.LobbyGolferHud
import com.chunkzero.example.ui.generated.LobbyWalletHud
import com.chunkzero.window.HudView
import net.kyori.adventure.text.Component

/** What the clubhouse HUDs show a golfer. [best] is their best round on a course of par [coursePar]. */
data class LobbyStats(
    val name: String,
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val coins: Int,
    val rounds: Int,
    val aces: Int,
    val best: Int?,
    val coursePar: Int,
    val golfers: Int,
)

/**
 * The clubhouse HUDs, composed for the action bar and all reading [stats]: the golfer at the top left, their coins and
 * best round at the top right, and the course's head count at the bottom right. Shader HUDs draw text in its authored
 * colour, so the best round picks a tone instead of colouring its score to par.
 */
class LobbyHuds(
    var stats: LobbyStats,
) {
    val views: List<HudView> = listOf(Golfer(), Wallet(), Course())

    private inner class Golfer : LobbyGolferHud() {
        override fun level(): Component = text("Lv ${stats.level}")

        override fun name(): Component = text(stats.name)

        override fun xpBar(index: Int): Boolean = index < stats.xp * XP_BAR_SIZE / stats.xpForNextLevel.coerceAtLeast(1)

        override fun xpText(): Component = text("${stats.xp} / ${stats.xpForNextLevel}")
    }

    private inner class Wallet : LobbyWalletHud() {
        override fun coins(): Component = text("${stats.coins} ◆")

        override fun best(): Component {
            val best = stats.best ?: return text("Best none yet")
            return text("Best $best (${formatScoreToPar(best - stats.coursePar)})")
        }

        override fun bestTone(): BestTone {
            val difference = (stats.best ?: return BestTone.NONE) - stats.coursePar
            return when {
                difference < 0 -> BestTone.UNDER
                difference > 0 -> BestTone.OVER
                else -> BestTone.EVEN
            }
        }

        override fun tally(): Component = text("${plural(stats.rounds, "round")} · ${plural(stats.aces, "ace")}")
    }

    private inner class Course : LobbyCourseHud() {
        override fun golfers(): Component = text("${plural(stats.golfers, "golfer")} here")
    }
}

private fun plural(
    count: Int,
    noun: String,
): String = if (count == 1) "1 $noun" else "$count ${noun}s"
