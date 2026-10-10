package com.chunkzero.example.lobby

import com.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.example.ui.LobbyHuds
import com.chunkzero.example.ui.LobbyStats
import com.chunkzero.window.HudChannel
import com.chunkzero.window.HudStack
import com.chunkzero.window.WindowSession
import net.minestom.server.entity.Player

/** One player in the clubhouse: their own backend client, latest profile, open menu and HUDs. */
class Visitor(
    val player: Player,
    val backend: CoroutineBackendClient,
) : AutoCloseable {
    var profile: ProfileResult? = null

    /** Their best round on the course, fetched once when they arrive. */
    var personalBest: Int? = null
    var window: WindowSession? = null

    /** Everything this player's action bar shows: the clubhouse HUDs, and the practice green's while putting. */
    val huds = HudStack(HudChannel.ACTION_BAR)
    private var lobbyHuds: LobbyHuds? = null

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

    /** Shows [stats] on the clubhouse HUDs, adding them to [huds] on first use. */
    fun showStats(stats: LobbyStats) {
        val current = lobbyHuds
        if (current != null) {
            current.stats = stats
        } else {
            lobbyHuds = LobbyHuds(stats).also { it.views.forEach(huds::add) }
        }
    }

    override fun close() {
        window?.close()
        window = null
    }
}
