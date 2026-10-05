package com.chunkzero.example.lobby

import com.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.window.WindowSession
import net.minestom.server.entity.Player

/** One player in the clubhouse: their own backend client, latest profile and open menu. */
class Visitor(
    val player: Player,
    val backend: CoroutineBackendClient,
) : AutoCloseable {
    var profile: ProfileResult? = null
    var window: WindowSession? = null

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

    override fun close() {
        window?.close()
        window = null
    }
}
