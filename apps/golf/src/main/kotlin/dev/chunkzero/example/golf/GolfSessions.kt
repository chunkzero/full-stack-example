package dev.chunkzero.example.golf

import dev.chunkzero.example.generated.BackendTypes.Shared.Courses
import dev.chunkzero.example.generated.BackendTypes.Shared.Players
import dev.chunkzero.example.generated.BackendTypes.Shared.Rounds
import dev.chunkzero.example.generated.CoroutineBackendClient
import dev.chunkzero.example.generated.Destinations
import dev.chunkzero.example.generated.SessionMethods
import dev.chunkzero.example.golf.physics.Shot
import dev.chunkzero.example.runGameProcess
import dev.chunkzero.example.ui.GolfStatus
import dev.chunkzero.example.ui.ResourcePack
import dev.chunkzero.example.ui.ScorecardMenu
import dev.chunkzero.example.ui.ScorecardRow
import dev.chunkzero.example.ui.StatusKind
import dev.chunkzero.example.ui.formatScoreToPar
import dev.chunkzero.example.ui.generated.WindowPack
import dev.chunkzero.example.world.GameWorld
import dev.chunkzero.example.world.keepAboveVoid
import dev.chunkzero.runtime.CoroutineSession
import dev.chunkzero.runtime.MoveResult
import dev.chunkzero.runtime.SessionProvider
import dev.chunkzero.runtime.SessionScope
import dev.chunkzero.runtime.SessionType
import dev.chunkzero.runtime.coroutines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val COURSE = "oakmere"

@SessionType("default")
class GolfSessions : SessionProvider {
    override fun create() = GolfSession()
}

/**
 * A game of golf on Oakmere Links. Players play their own nine holes at their own pace; the session shows everyone's
 * balls, keeps scorecards live and records each finished round.
 */
class GolfSession :
    CoroutineSession(),
    SessionMethods.Golf.Default.Scorecard {
    private lateinit var scope: SessionScope
    private lateinit var course: Course
    private lateinit var terrain: InstanceTerrain
    private lateinit var instance: InstanceContainer
    private lateinit var ghost: Deferred<Ghost?>
    private var courseName = "Oakmere Links"

    private val rounds = LinkedHashMap<Player, PlayerRound>()
    private val roundsPlayed = HashMap<UUID, Int>()
    private val scorecards = HashMap<Player, Pair<ScorecardMenu, Inventory>>()

    private val pars: List<Int> get() = course.roundHoles.map { it.par }

    override suspend fun create(scope: SessionScope) {
        this.scope = scope
        course = scope.component(Course::class.java)
        instance = scope.component(GameWorld::class.java).load(scope)
        terrain = InstanceTerrain(instance)
        scope.keepAboveVoid { course.spawn }

        val backend = CoroutineBackendClient(scope.coroutines.backend(requireNotNull(scope.backend)))
        scope.coroutines.launch { checkPars(backend) }
        ghost = scope.coroutines.async { loadGhost(backend) }
    }

    override suspend fun join(player: Player) {
        player.teleport(course.spawn).await()
        player.sendResourcePacks(ResourcePack.request())
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.food = 20
        player.foodSaturation = 20f
        player.inventory.clear()
        player.inventory.setItemStack(0, Putter.item())
        player.setHeldItemSlot(0)

        val backend = playerBackend(player)
        try {
            backend.shared.players.join(Players.JoinArgs(player.username), scope.operationId(player, "join"))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.warn("Registering {} failed", player.username, error)
        }
        val skin = ballSkin(backend)
        val ghost = ghost.await()

        val round =
            PlayerRound(
                scope,
                player,
                instance,
                terrain,
                course.roundHoles,
                skin,
                ghost,
                onHoleFinished = { refreshScorecards() },
                onRoundFinished = { scope.coroutines.launch { finishRound(it) } },
            )
        rounds[player] = round
        scope.own(player, round)
        refreshScorecards()

        player.showTitle(Title.title(Component.text(courseName), Component.text("Nine holes. Right-click to putt.")))
        ghost?.let {
            player.sendMessage(
                Component.text("Racing ${it.name}'s course record ghost.", NamedTextColor.GRAY),
            )
        }
        round.start()
    }

    override suspend fun leave(player: Player) {
        rounds.remove(player)
        scorecards.remove(player)
        refreshScorecards()
    }

    override fun scorecard(args: SessionMethods.Golf.Default.Scorecard.Args): Boolean {
        val player = rounds.keys.firstOrNull { it.uuid.toString() == args.player() } ?: return false
        openScorecard(player) { player.closeInventory() }
        return true
    }

    private fun openScorecard(
        player: Player,
        onBack: () -> Unit,
    ) {
        val menu = ScorecardMenu(courseName, pars, rowsFor(player), onBack)
        val session = WindowPack.windows().open(player, menu)
        scorecards[player] = menu to session.inventory
    }

    private fun rowsFor(viewer: Player): List<ScorecardRow> =
        rounds.values.take(ScorecardMenu.MAX_PLAYERS).map {
            ScorecardRow(it.player.username, it.card.scores, it.player == viewer)
        }

    private fun refreshScorecards() {
        scorecards.entries.removeIf { (player, open) -> player.openInventory != open.second }
        scorecards.forEach { (player, open) -> open.first.rows = rowsFor(player) }
    }

    private suspend fun finishRound(round: PlayerRound) {
        val player = round.player
        val number = roundsPlayed.merge(player.uuid, 1, Int::plus)!!
        val args =
            Rounds.RecordArgs(
                COURSE,
                "${scope.id}/${player.uuid}/$number",
                round.card.holes.map { hole ->
                    hole.shots.map { Rounds.RecordArgs.ShotsItemItem(it.power, it.yaw.toDouble()) }
                },
                round.card.scores.map { it!!.toLong() },
            )
        val operation = scope.operationId(player, "round-$number")
        val backend = playerBackend(player)
        var result: Rounds.RecordResult? = null
        for (attempt in 1..RECORD_ATTEMPTS) {
            try {
                result = backend.shared.rounds.record_(args, operation)
                break
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.warn("Recording round {} failed (attempt {})", args.round(), attempt, error)
                if (attempt < RECORD_ATTEMPTS) {
                    player.sendMessage(Component.text("Saving your round failed, retrying...", NamedTextColor.RED))
                    delay(RECORD_RETRY_DELAY)
                }
            }
        }
        if (!player.isOnline || player !in rounds) return
        announce(player, round.card, result)

        var moved = false
        val toLobby = {
            if (!moved) {
                moved = true
                scope.coroutines.launch { moveToLobby(player) }
            }
        }
        openScorecard(player, toLobby)
        delay(RESULTS_DELAY)
        toLobby()
    }

    private fun announce(
        player: Player,
        card: RoundCard,
        result: Rounds.RecordResult?,
    ) {
        val line = "Round complete: ${card.total} (${formatScoreToPar(card.toPar)})"
        player.sendMessage(Component.text(line, NamedTextColor.GOLD))
        if (result == null) {
            player.sendMessage(
                Component.text("Your round couldn't be saved, so no rewards this time.", NamedTextColor.RED),
            )
            rounds[player]?.showStatus(GolfStatus("Round not saved", StatusKind.BAD))
            return
        }
        player.sendMessage(Component.text("+${result.coins()} coins, +${result.xp()} XP", NamedTextColor.YELLOW))
        if (result.courseRecord()) {
            player.sendMessage(Component.text("New course record!", NamedTextColor.GREEN))
        } else if (result.personalBest()) {
            player.sendMessage(Component.text("New personal best!", NamedTextColor.GREEN))
        }
        rounds[player]?.showStatus(GolfStatus("Round complete", StatusKind.GOOD))
    }

    private suspend fun moveToLobby(player: Player) {
        if (!player.isOnline || player !in rounds) return
        val result =
            try {
                scope.move(player, Destinations.Lobby.main).await()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.warn("Moving {} to the lobby failed", player.username, error)
                null
            }
        if (result != MoveResult.ACCEPTED) {
            player.sendMessage(
                Component.text(
                    "Couldn't head back to the lobby (${result ?: "error"}). Try /lobby.",
                    NamedTextColor.RED,
                ),
            )
        }
    }

    private fun playerBackend(player: Player) =
        CoroutineBackendClient(scope.coroutines.backend(requireNotNull(scope.backend), player))

    private suspend fun ballSkin(backend: CoroutineBackendClient): BallSkin =
        try {
            val ball =
                backend.shared.players
                    .profile()
                    .ball()
            val item =
                backend.shared.shop
                    .catalog()
                    .firstOrNull { it.id() == ball }
                    ?.item()
            BallSkin(ItemStack.of(item?.let(Material::fromKey) ?: Material.SNOWBALL), BallSkin.DEFAULT.scale)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.warn("Loading the ball skin failed", error)
            BallSkin.DEFAULT
        }

    private suspend fun checkPars(backend: CoroutineBackendClient) {
        try {
            val server = backend.shared.courses.course(Courses.CourseArgs(COURSE))
            if (server == null) {
                logger.warn("The backend doesn't know course {}", COURSE)
                return
            }
            courseName = server.name()
            val serverPars = server.pars().map(Long::toInt)
            if (serverPars != pars) logger.warn("Course {} pars differ: marks {}, backend {}", COURSE, pars, serverPars)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.warn("Checking course {} failed", COURSE, error)
        }
    }

    private suspend fun loadGhost(backend: CoroutineBackendClient): Ghost? =
        try {
            backend.shared.rounds
                .courseRecord(Rounds.CourseRecordArgs(COURSE))
                ?.takeIf { it.shots().size == course.roundHoles.size }
                ?.let { record ->
                    Ghost(
                        record.name(),
                        record.shots().map { hole ->
                            hole.map { Shot(it.yaw().toFloat(), it.power()) }
                        },
                    )
                }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.warn("Loading the course record failed", error)
            null
        }

    private companion object {
        val logger = LoggerFactory.getLogger(GolfSession::class.java)!!
        const val RECORD_ATTEMPTS = 4
        val RECORD_RETRY_DELAY = 3.seconds
        val RESULTS_DELAY = 15.seconds
    }
}

fun main() = runGameProcess()
