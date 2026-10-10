package com.chunkzero.example.golf

import com.chunkzero.chunk.backend.api.PlayerId
import com.chunkzero.chunk.backend.client.OperationId
import com.chunkzero.chunk.multistom.CoroutineSession
import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.chunk.multistom.coroutines
import com.chunkzero.chunk.runtime.MoveResult
import com.chunkzero.chunk.runtime.SessionProvider
import com.chunkzero.chunk.runtime.SessionType
import com.chunkzero.example.generated.BackendTypes.Shared.Courses
import com.chunkzero.example.generated.BackendTypes.Shared.Players
import com.chunkzero.example.generated.BackendTypes.Shared.Rounds
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.example.generated.Destinations
import com.chunkzero.example.generated.SessionMethods
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.runGameProcess
import com.chunkzero.example.ui.GolfStatus
import com.chunkzero.example.ui.ResourcePack
import com.chunkzero.example.ui.ScorecardMenu
import com.chunkzero.example.ui.ScorecardRow
import com.chunkzero.example.ui.StatusKind
import com.chunkzero.example.ui.formatScoreToPar
import com.chunkzero.example.world.GameWorld
import com.chunkzero.example.world.keepAboveVoid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val COURSE = "oakmere"

@SessionType("default")
class GolfSessions : SessionProvider<GolfSession> {
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
    private lateinit var backend: CoroutineBackendClient
    private lateinit var course: Course
    private lateinit var terrain: InstanceTerrain
    private lateinit var instance: InstanceContainer
    private lateinit var ghost: Deferred<Ghost?>
    private var courseName = "Oakmere Links"

    private val rounds = LinkedHashMap<Player, PlayerRound>()
    private val roundsPlayed = HashMap<UUID, Int>()
    private val saves = HashSet<Deferred<Rounds.RecordResult?>>()
    private val scorecards = HashMap<Player, ScorecardMenu>()

    private val pars: List<Int> get() = course.roundHoles.map { it.par }

    override suspend fun create(scope: SessionScope) {
        this.scope = scope
        course = scope.component(Course::class.java)
        instance = scope.component(GameWorld::class.java).load(scope)
        terrain = InstanceTerrain(instance)
        scope.keepAboveVoid { course.spawn }

        backend = CoroutineBackendClient(scope.coroutines.backend(requireNotNull(scope.backend)))
        scope.coroutines.launch { checkPars() }
        ghost = scope.coroutines.async { loadGhost() }
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
                onHoleFinished = { finished, hole, score ->
                    refreshScorecards()
                    shareHighlight(finished.player, hole, score)
                },
                onRoundFinished = ::startSave,
                onResultsReady = { finished, save -> scope.coroutines.launch { showResults(finished, save) } },
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

    override suspend fun finish() {
        saves.toList().awaitAll()
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
        val menu = ScorecardMenu(player, courseName, pars, rowsFor(player), onBack) { scorecards.remove(player, it) }
        menu.open()
        scorecards[player] = menu
    }

    private fun rowsFor(viewer: Player): List<ScorecardRow> =
        rounds.values.take(ScorecardMenu.MAX_PLAYERS).map {
            ScorecardRow(it.player.username, it.card.scores, it.player == viewer)
        }

    private fun refreshScorecards() {
        scorecards.forEach { (player, menu) -> menu.rows = rowsFor(player) }
    }

    /** Tells everyone else in the session when [player] scores a birdie or better. */
    private fun shareHighlight(
        player: Player,
        hole: Hole,
        score: Int,
    ) {
        val feat =
            when {
                score == 1 -> "aced"
                score <= hole.par - 3 -> "albatrossed"
                score == hole.par - 2 -> "eagled"
                score == hole.par - 1 -> "birdied"
                else -> return
            }
        val line = Component.text("${player.username} $feat ${hole.name}", NamedTextColor.GRAY)
        rounds.keys.filter { it != player }.forEach { it.sendMessage(line) }
    }

    /**
     * Starts saving a finished round right away. The session records it for the player, so the save outlives both the
     * player and the results screen.
     */
    private fun startSave(round: PlayerRound): Deferred<Rounds.RecordResult?> {
        val player = round.player
        val number = roundsPlayed.merge(player.uuid, 1, Int::plus)!!
        val args =
            Rounds.RecordArgs(
                COURSE,
                PlayerId(player.uuid.toString()),
                "${scope.id}/${player.uuid}/$number",
                round.card.holes.map { hole ->
                    hole.shots.map { Rounds.RecordArgs.ShotsItemItem(it.power, it.yaw.toDouble()) }
                },
                round.card.scores.map { it!!.toLong() },
            )
        val operation = scope.operationId(player, "round-$number")
        val save = scope.coroutines.async { record(round, args, operation) }
        saves += save
        save.invokeOnCompletion { saves -= save }
        return save
    }

    private suspend fun record(
        round: PlayerRound,
        args: Rounds.RecordArgs,
        operation: OperationId,
    ): Rounds.RecordResult? {
        for (attempt in 1..RECORD_ATTEMPTS) {
            try {
                return backend.shared.rounds.record_(args, operation)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.warn("Recording round {} failed (attempt {})", args.round(), attempt, error)
                if (attempt < RECORD_ATTEMPTS) {
                    if (rounds[round.player] === round && round.player.isOnline) {
                        round.player.sendMessage(
                            Component.text("Saving your round failed, retrying...", NamedTextColor.RED),
                        )
                    }
                    delay(RECORD_RETRY_DELAY)
                }
            }
        }
        return null
    }

    private suspend fun showResults(
        round: PlayerRound,
        save: Deferred<Rounds.RecordResult?>,
    ) {
        val player = round.player
        val result = save.await()
        if (!player.isOnline || rounds[player] !== round) return
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

    private suspend fun checkPars() {
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

    private suspend fun loadGhost(): Ghost? =
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
