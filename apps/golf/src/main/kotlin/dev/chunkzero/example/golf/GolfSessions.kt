package dev.chunkzero.example.golf

import dev.chunkzero.example.runGameProcess
import dev.chunkzero.example.ui.ResourcePack
import dev.chunkzero.runtime.CoroutineSession
import dev.chunkzero.runtime.SessionProvider
import dev.chunkzero.runtime.SessionScope
import dev.chunkzero.runtime.SessionType
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block

@SessionType("default")
class GolfSessions : SessionProvider {
    override fun create() = GolfSession()
}

class GolfSession : CoroutineSession() {
    override suspend fun create(scope: SessionScope) {
        scope.createInstance().apply {
            setChunkSupplier(::LightingChunk)
            setGenerator { it.modifier().fillHeight(0, 40, Block.MOSS_BLOCK) }
        }
    }

    override suspend fun join(player: Player) {
        player.sendResourcePacks(ResourcePack.request())
        player.sendMessage(Component.text("Welcome to Oakmere Links. Use /lobby to head back."))
    }
}

fun main() = runGameProcess()
