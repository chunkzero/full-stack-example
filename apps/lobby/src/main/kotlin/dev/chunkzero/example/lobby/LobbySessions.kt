package dev.chunkzero.example.lobby

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
class LobbySessions : SessionProvider {
    override fun create() = LobbySession()
}

class LobbySession : CoroutineSession() {
    override suspend fun create(scope: SessionScope) {
        scope.createInstance().apply {
            setChunkSupplier(::LightingChunk)
            setGenerator { it.modifier().fillHeight(0, 40, Block.GRASS_BLOCK) }
        }
    }

    override suspend fun join(player: Player) {
        player.sendResourcePacks(ResourcePack.request())
        player.sendMessage(Component.text("Welcome to the clubhouse. Use /play to tee off."))
    }
}

fun main() = runGameProcess()
