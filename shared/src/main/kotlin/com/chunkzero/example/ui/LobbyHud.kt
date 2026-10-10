package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.LobbyHud
import net.kyori.adventure.text.Component

/** What the clubhouse sidebar shows a golfer. [best] is their best round on a course of par [coursePar]. */
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
    val record: CourseRecord?,
    val golfers: Int,
)

/** The clubhouse sidebar, composed for a sidebar's title. */
class LobbyStatsHud(
    var stats: LobbyStats,
) : LobbyHud() {
    override fun name(): Component = text(stats.name)

    override fun level(): Component = text("Level ${stats.level}")

    override fun xp(index: Int): Boolean = index < stats.xp * XP_SIZE / stats.xpForNextLevel.coerceAtLeast(1)

    override fun xpText(): Component = text("${stats.xp} / ${stats.xpForNextLevel} XP")

    override fun coins(): Component = text("${stats.coins} ◆", UiColors.GOLD)

    override fun best(): Component = stats.best?.let { formatRound(it, stats.coursePar) } ?: text("–", UiColors.MUTED)

    override fun rounds(): Component = text(stats.rounds.toString())

    override fun aces(): Component = text(stats.aces.toString())

    override fun record(): Component {
        val record = stats.record ?: return text("No record yet", UiColors.MUTED)
        return formatRound(record.strokes, stats.coursePar).append(text(" · ${record.holder}", UiColors.MUTED))
    }

    override fun golfers(): Component =
        text(
            if (stats.golfers ==
                1
            ) {
                "1 golfer here"
            } else {
                "${stats.golfers} golfers here"
            },
        )
}
