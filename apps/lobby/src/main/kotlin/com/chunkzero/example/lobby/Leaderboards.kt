package com.chunkzero.example.lobby

import com.chunkzero.chunk.runtime.SessionScope
import com.chunkzero.chunk.runtime.coroutines
import com.chunkzero.example.generated.BackendTypes.Shared.Leaderboards.LeaderboardArgs
import com.chunkzero.example.generated.BackendTypes.Shared.Leaderboards.LeaderboardArgs.Board
import com.chunkzero.example.generated.BackendTypes.Shared.Leaderboards.LeaderboardResultItem
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.example.golf.Leaderboard
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import java.util.Locale

private const val ROWS = 10

/** Text grows upward from a display; this lowers the board so its middle sits at the mark. */
private const val HALF_HEIGHT = 1.6

private val GOLD = TextColor.color(0xffd75e)
private val SILVER = TextColor.color(0xd0d6dc)
private val BRONZE = TextColor.color(0xd08a4e)

/**
 * Shows a live board at each leaderboard mark. Each board has one watch for the whole session, so it redraws as rounds
 * finish anywhere.
 */
fun showLeaderboards(
    scope: SessionScope,
    instance: Instance,
    boards: List<Leaderboard>,
    backend: CoroutineBackendClient,
) {
    for (mark in boards) {
        val board = Board.valueOf(mark.board)
        val display = Entity(scope.process, EntityType.TEXT_DISPLAY)
        display.setNoGravity(true)
        display.editEntityMeta(TextDisplayMeta::class.java) {
            it.lineWidth = 200
            it.text = renderLeaderboard(board, emptyList())
        }
        display.setInstance(instance, mark.position.sub(0.0, HALF_HEIGHT, 0.0))
        val course = if (board == Board.course_record) OAKMERE else null
        watching("leaderboard $board") {
            backend.shared.leaderboards.watchLeaderboard(LeaderboardArgs(board, course))
        }.onEach { live ->
            display.editEntityMeta(TextDisplayMeta::class.java) {
                it.text = renderLeaderboard(board, live.latest?.value() ?: emptyList(), live.freshness)
            }
        }.launchIn(scope.coroutines)
    }
}

fun renderLeaderboard(
    board: Board,
    rows: List<LeaderboardResultItem>,
    freshness: Freshness = Freshness.LIVE,
): Component {
    val (title, subtitle) =
        when (board) {
            Board.course_record -> "Course Record" to "Oakmere Links · fewest strokes"
            Board.holes_in_one -> "Holes in One" to "Most aces"
            Board.coins -> "Top Earners" to "Most coins"
        }
    val text =
        Component
            .text()
            .append(Component.text(title.uppercase(), GOLD, TextDecoration.BOLD))
            .appendNewline()
            .append(Component.text(subtitle, NamedTextColor.GRAY))
            .also {
                when (freshness) {
                    Freshness.LIVE -> {}

                    Freshness.UPDATING -> {
                        it.append(Component.text(" · updating…", NamedTextColor.DARK_GRAY))
                    }

                    Freshness.OFFLINE -> {
                        it.append(Component.text(" · offline", NamedTextColor.DARK_GRAY))
                    }
                }
            }.appendNewline()
    for (rank in 1..ROWS) {
        text.appendNewline().append(row(board, rank, rows.getOrNull(rank - 1)))
    }
    return text.build()
}

private fun row(
    board: Board,
    rank: Int,
    entry: LeaderboardResultItem?,
): Component {
    val color =
        when (rank) {
            1 -> GOLD
            2 -> SILVER
            3 -> BRONZE
            else -> NamedTextColor.GRAY
        }
    val place = Component.text("$rank. ", color)
    if (entry == null) return place.append(Component.text("---", NamedTextColor.DARK_GRAY))
    return place
        .append(Component.text(entry.name(), NamedTextColor.WHITE))
        .append(Component.text("  ${formatValue(board, entry.value())}", valueColor(board)))
}

fun formatValue(
    board: Board,
    value: Long,
): String {
    val unit =
        when (board) {
            Board.course_record -> "stroke"
            Board.holes_in_one -> "ace"
            Board.coins -> "coin"
        }
    return if (value == 1L) "1 $unit" else String.format(Locale.ROOT, "%,d %ss", value, unit)
}

private fun valueColor(board: Board): TextColor =
    when (board) {
        Board.course_record -> NamedTextColor.GREEN
        Board.holes_in_one -> NamedTextColor.AQUA
        Board.coins -> GOLD
    }
