package com.chunkzero.example.lobby

import com.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.example.ui.LobbyStats
import com.chunkzero.example.ui.LobbyStatsHud
import com.chunkzero.window.WindowSession
import com.chunkzero.window.multistom.showHud
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.scoreboard.Sidebar

/** One player in the clubhouse: their own backend client, latest profile, open menu and sidebar. */
class Visitor(
    val player: Player,
    val backend: CoroutineBackendClient,
) : AutoCloseable {
    var profile: ProfileResult? = null

    /** Their best round on the course, fetched once when they arrive. */
    var personalBest: Int? = null
    var window: WindowSession? = null
    private var board: StatsBoard? = null

    /** Set while a menu is loading or a shop mutation is in flight, so repeated clicks are ignored. */
    var busy = false
    private var operations = 0
    private var unsettled: Attempt? = null

    private class Attempt(
        val action: String,
        val item: String,
        val operation: String,
    )

    /**
     * The operation name for [action] on [item], e.g. `buy-3`. Repeating the action that last failed uncertainly reuses
     * its name so the backend treats it as the same operation; anything else gets a fresh one.
     */
    fun operation(
        action: String,
        item: String,
    ): String {
        val attempt =
            unsettled?.takeIf { it.action == action && it.item == item }
                ?: Attempt(action, item, "$action-${operations++}")
        unsettled = attempt
        return attempt.operation
    }

    /** Forgets the last attempt once it has a definitive result. */
    fun settled() {
        unsettled = null
    }

    /** Shows [stats] on this player's clubhouse sidebar, creating it on first use. */
    fun showStats(stats: LobbyStats) {
        val board = board
        if (board == null) this.board = StatsBoard(player, stats) else board.show(stats)
    }

    override fun close() {
        window?.close()
        window = null
        board?.close()
        board = null
    }
}

/**
 * A sidebar whose title is the [LobbyStatsHud], over blank score lines that lift it towards the top of the screen.
 * The HUD's frame is sized to cover the vanilla backdrop of exactly [LINES] lines, the `lines` of lobby.tsx.
 */
private class StatsBoard(
    private val player: Player,
    stats: LobbyStats,
) : AutoCloseable {
    private val hud = LobbyStatsHud(stats)
    private val sidebar =
        Sidebar(Component.empty()).apply {
            repeat(LINES) { line ->
                createLine(Sidebar.ScoreboardLine("$line", Component.empty(), line, Sidebar.NumberFormat.blank()))
            }
            showHud(hud)
            addViewer(player)
        }

    fun show(stats: LobbyStats) {
        if (stats == hud.stats) return
        hud.stats = stats
        sidebar.showHud(hud)
    }

    override fun close() {
        sidebar.removeViewer(player)
    }

    private companion object {
        const val LINES = 15
    }
}
