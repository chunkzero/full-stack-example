package dev.chunkzero.example.world

import dev.chunkzero.runtime.SessionScope
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.PlayerMoveEvent

/** Below this height a player has fallen off the world. */
const val VOID_Y = 32.0

/** Returns players who fall off this session's world to [spawn]. */
fun SessionScope.keepAboveVoid(spawn: () -> Pos) {
    events.addListener(PlayerMoveEvent::class.java) { event ->
        if (event.newPosition.y() < VOID_Y) event.newPosition = spawn()
    }
}
