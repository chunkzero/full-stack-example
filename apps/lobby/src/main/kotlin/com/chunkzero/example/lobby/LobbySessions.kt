package com.chunkzero.example.lobby

import com.chunkzero.chunk.multistom.CoroutineSession
import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.chunk.multistom.coroutines
import com.chunkzero.chunk.multistom.repeatEvery
import com.chunkzero.chunk.runtime.SessionProvider
import com.chunkzero.chunk.runtime.SessionType
import com.chunkzero.example.generated.BackendTypes.Shared.Players.JoinArgs
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.example.golf.Course
import com.chunkzero.example.runGameProcess
import com.chunkzero.example.ui.ResourcePack
import com.chunkzero.example.world.GameWorld
import com.chunkzero.example.world.keepAboveVoid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds
import com.chunkzero.example.golf.BallSkin as BallLook

@SessionType("default")
class LobbySessions : SessionProvider<LobbySession> {
    override fun create() = LobbySession()
}

class LobbySession : CoroutineSession() {
    private lateinit var scope: SessionScope
    private lateinit var course: Course
    private lateinit var catalog: Catalog
    private lateinit var menus: Menus
    private lateinit var hotbar: Hotbar
    private lateinit var green: PracticeGreen
    private val visitors = HashMap<Player, Visitor>()

    override suspend fun create(scope: SessionScope) {
        this.scope = scope
        course = scope.component(Course::class.java)
        val instance = scope.component(GameWorld::class.java).load(scope)
        scope.keepAboveVoid { course.spawn }

        val backend = CoroutineBackendClient(scope.coroutines.backend(requireNotNull(scope.backend)))
        catalog = Catalog(backend)
        val departures = Departures(scope)
        menus = Menus(scope, backend, catalog, visitors, departures::teeOff)
        hotbar = Hotbar(scope) { player, item -> visitors[player]?.let { menus.open(it, item) } }
        green = PracticeGreen(scope, instance, course.hole(0), ::ballLook, hotbar::give)
        val portal =
            Portal(scope, instance, course.portals.map { it.region }) { player ->
                if (player in visitors) departures.teeOff(player)
            }
        scope.repeatEvery(REGION_CHECK) {
            portal.update(visitors.keys)
            green.update(visitors.keys)
        }
        spawnNpcs(scope, instance, course.npcs) { player, item -> visitors[player]?.let { menus.open(it, item) } }
        showLeaderboards(scope, instance, course.leaderboards, backend)
    }

    override suspend fun join(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.food = 20
        player.teleport(course.spawn).await()
        player.sendResourcePacks(ResourcePack.request())
        hotbar.give(player)

        val visitor =
            Visitor(player, CoroutineBackendClient(scope.coroutines.backend(requireNotNull(scope.backend), player)))
        visitors[player] = visitor
        scope.own(player, visitor)
        green.join(player)
        menus.playersChanged()
        scope.coroutines.launch { welcome(visitor) }
    }

    override suspend fun leave(player: Player) {
        visitors.remove(player)
        menus.playersChanged()
    }

    /** Registers the golfer, greets them, then follows their profile for live menus and their ball skin. */
    private suspend fun welcome(visitor: Visitor) {
        val player = visitor.player
        val players = visitor.backend.shared.players
        try {
            players.join(JoinArgs(player.username), scope.operationId(player, "join"))
            catalog.items()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.warn("Could not register {}", player.username, error)
        }
        if (visitors[player] !== visitor) return
        player.sendMessage(
            Component
                .text("Welcome to the clubhouse, ${player.username}! ", NamedTextColor.GOLD)
                .append(
                    Component.text(
                        "Walk through the gate or talk to the Caddie to tee off at Oakmere Links.",
                        NamedTextColor.YELLOW,
                    ),
                ),
        )
        val profiles =
            watching("profile ${player.username}") { players.watchProfile() }
                .onEach { live ->
                    live.latest?.let { visitor.profile = it.value() }
                    menus.profileChanged(visitor, live.freshness)
                }.launchIn(scope.coroutines)
        scope.own(player, AutoCloseable { profiles.cancel() })
    }

    private fun ballLook(player: Player): BallLook {
        val ball = visitors[player]?.profile?.ball() ?: return BallLook.DEFAULT
        val item = catalog.cached.firstOrNull { it.id() == ball }?.item() ?: return BallLook.DEFAULT
        return BallLook(ItemStack.of(ballMaterial(item)), BallLook.DEFAULT.scale)
    }

    private companion object {
        val REGION_CHECK = 100.milliseconds
        val logger = LoggerFactory.getLogger(LobbySession::class.java)!!
    }
}

fun main() = runGameProcess()
