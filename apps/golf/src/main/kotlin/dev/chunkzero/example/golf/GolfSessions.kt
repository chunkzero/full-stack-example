package dev.chunkzero.example.golf

import dev.chunkzero.example.runGameProcess
import dev.chunkzero.example.ui.ResourcePack
import dev.chunkzero.example.world.GameWorld
import dev.chunkzero.example.world.keepAboveVoid
import dev.chunkzero.runtime.CoroutineSession
import dev.chunkzero.runtime.SessionProvider
import dev.chunkzero.runtime.SessionScope
import dev.chunkzero.runtime.SessionType
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

@SessionType("default")
class GolfSessions : SessionProvider {
    override fun create() = GolfSession()
}

class GolfSession : CoroutineSession() {
    private lateinit var course: Course

    override suspend fun create(scope: SessionScope) {
        course = scope.component(Course::class.java)
        scope.component(GameWorld::class.java).load(scope)
        scope.keepAboveVoid { course.spawn }
    }

    override suspend fun join(player: Player) {
        player.teleport(course.spawn).await()
        player.sendResourcePacks(ResourcePack.request())
        player.sendMessage(Component.text("Welcome to Oakmere Links. Use /lobby to head back."))
    }
}

fun main() = runGameProcess()
