package dev.chunkzero.example.lobby

import dev.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import dev.chunkzero.example.generated.CoroutineBackendClient
import dev.oglass.window.WindowSession
import net.minestom.server.entity.Player

/** One player in the clubhouse: their own backend client, latest profile and open menu. */
class Visitor(
    val player: Player,
    val backend: CoroutineBackendClient,
) : AutoCloseable {
    var profile: ProfileResult? = null
    var window: WindowSession? = null

    /** Set while a menu is loading or a shop purchase is in flight, so repeated clicks are ignored. */
    var busy = false
    private var operations = 0

    /** A fresh operation action name, e.g. `buy-3`, for one mutation attempt. */
    fun nextOperation(action: String) = "$action-${operations++}"

    override fun close() {
        window?.close()
        window = null
    }
}
