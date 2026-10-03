package dev.chunkzero.example.golf

import dev.chunkzero.example.golf.physics.BallEvent
import dev.chunkzero.example.golf.physics.Shot
import dev.chunkzero.example.golf.physics.Terrain
import dev.chunkzero.example.ui.GolfStatus
import dev.chunkzero.example.ui.GolfStatusHud
import dev.chunkzero.example.ui.StatusKind
import dev.chunkzero.example.ui.generated.WindowPack
import dev.chunkzero.runtime.SessionScope
import dev.chunkzero.runtime.coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import kotlin.time.Duration.Companion.seconds

/** The course record a ghost replays: the holder's name and their shots for each hole. */
class Ghost(
    val name: String,
    val shots: List<List<Shot>>,
)

/**
 * One player's nine holes: their ball, putter, HUD and ghost, from the first tee to the last cup. Owned by the
 * player's place in the session, so leaving mid-round cleans everything up. [onHoleFinished] runs after each hole's
 * score is final and [onRoundFinished] after the last one.
 */
class PlayerRound(
    private val scope: SessionScope,
    val player: Player,
    private val instance: Instance,
    private val terrain: Terrain,
    private val holes: List<Hole>,
    private val skin: BallSkin,
    private val ghost: Ghost?,
    private val onHoleFinished: (PlayerRound) -> Unit,
    private val onRoundFinished: (PlayerRound) -> Unit,
) : AutoCloseable {
    val card = RoundCard(holes.map { it.par })
    private val jobs =
        CoroutineScope(scope.coroutines.coroutineContext + SupervisorJob(scope.coroutines.coroutineContext[Job]))
    private val hud = GolfStatusHud()
    private val hudSession = WindowPack.windows().show(player, hud)
    private val putter = Putter(scope, player, ball = { ball?.position ?: player.position }, onShot = ::shoot)
    private val task =
        scope.scheduler
            .buildTask(::tick)
            .repeat(TaskSchedule.tick(1))
            .schedule()

    private var holeIndex = -1
    private var ball: GolfBall? = null
    private var replay: GhostReplay? = null
    private var ticks = 0L
    private var holeStartedAt = 0L
    private var statusUntil = 0L

    private val hole: Hole get() = holes[holeIndex]
    private val holeCard: HoleCard get() = card.holes[holeIndex]
    private val cupCenter: Vec get() = hole.cup.bottomCenter()

    fun start() {
        jobs.launch {
            delay(START_DELAY)
            startHole(0)
        }
    }

    private fun startHole(index: Int) {
        holeIndex = index
        clearHole()
        ball = GolfBall(scope, instance, terrain, hole, skin, onEvent = ::onBallEvent)
        ghost?.let { replay = GhostReplay(scope, instance, terrain, hole, it.shots[index], player) }
        placeBehind(player, hole.ballStart, cupCenter)
        hud.hole = hole.number
        hud.holeName = hole.name
        hud.par = hole.par
        hud.stroke = 0
        holeStartedAt = ticks
        putter.armed = true
    }

    private fun shoot(shot: Shot) {
        val ball = ball ?: return
        if (ball.moving || holeCard.finished) return
        holeCard.shoot(shot)
        hud.stroke = holeCard.strokes
        ball.shoot(shot)
    }

    private fun onBallEvent(event: BallEvent) {
        if (holeCard.finished) return
        when (event) {
            is BallEvent.Sunk -> {
                sunk()
            }

            is BallEvent.Rested -> {
                if (holeCard.rested()) pickedUp() else nextStroke(event.position)
            }

            is BallEvent.OutOfBounds -> {
                val splash = event.reason == BallEvent.Reason.HAZARD
                val pickedUp = holeCard.outOfBounds()
                hud.stroke = holeCard.strokes
                ball?.place(event.resetPosition)
                if (pickedUp) {
                    pickedUp()
                } else {
                    showStatus(GolfStatus(if (splash) "Splash! +1" else "Out of bounds +1", StatusKind.BAD))
                    player.playSound(
                        sound(if (splash) SoundEvent.ENTITY_GENERIC_SPLASH else SoundEvent.BLOCK_NOTE_BLOCK_BASS),
                    )
                    nextStroke(event.resetPosition)
                }
            }
        }
    }

    private fun nextStroke(ballAt: Point) {
        placeBehind(player, ballAt, cupCenter)
        putter.armed = true
    }

    private fun sunk() {
        val score = holeCard.sink()
        val name = scoreName(score, hole.par)
        showStatus(GolfStatus(name.text, statusKind(name.tone)))
        celebrate(holeInOne = score == 1)
        finishHole()
    }

    private fun pickedUp() {
        ball?.close()
        showStatus(GolfStatus("Picked up", StatusKind.BAD))
        finishHole()
    }

    private fun timedOut() {
        ball?.close()
        holeCard.timeOut()
        showStatus(GolfStatus("Time's up", StatusKind.BAD))
        player.playSound(sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS))
        finishHole()
    }

    private fun finishHole() {
        putter.armed = false
        hud.stroke = holeCard.score ?: holeCard.strokes
        onHoleFinished(this)
        jobs.launch {
            delay(NEXT_HOLE_DELAY)
            if (holeIndex + 1 < holes.size) {
                startHole(holeIndex + 1)
            } else {
                clearHole()
                onRoundFinished(this@PlayerRound)
            }
        }
    }

    private fun tick() {
        ticks++
        hud.power = if (putter.charging) putter.power.value else null
        if (statusUntil in 1..ticks) {
            hud.status = null
            statusUntil = 0
        }
        val playing = holeIndex >= 0 && !holeCard.finished
        if (playing && ticks - holeStartedAt >= HOLE_TIME_LIMIT_TICKS) timedOut()
    }

    fun showStatus(status: GolfStatus) {
        hud.status = status
        statusUntil = ticks + STATUS_TICKS
    }

    private fun celebrate(holeInOne: Boolean) {
        val at = cupCenter.add(0.0, 1.0, 0.0)
        val particle = if (holeInOne) Particle.FIREWORK else Particle.HAPPY_VILLAGER
        instance.sendGroupedPacket(ParticlePacket(particle, at, Vec(0.4, 0.6, 0.4), 0.15f, if (holeInOne) 80 else 20))
        player.playSound(sound(SoundEvent.ENTITY_PLAYER_LEVELUP))
        if (holeInOne) {
            instance.sendGroupedPacket(ParticlePacket(Particle.TOTEM_OF_UNDYING, at, Vec(0.5, 1.0, 0.5), 0.5f, 60))
            instance.playSound(sound(SoundEvent.ENTITY_FIREWORK_ROCKET_BLAST), at)
            instance.playSound(sound(SoundEvent.ENTITY_FIREWORK_ROCKET_TWINKLE), at)
        }
    }

    private fun clearHole() {
        ball?.close()
        ball = null
        replay?.close()
        replay = null
    }

    override fun close() {
        jobs.cancel()
        task.cancel()
        clearHole()
        putter.close()
        hudSession.hide()
    }

    private companion object {
        val START_DELAY = 3.seconds
        val NEXT_HOLE_DELAY = 3.seconds
        const val HOLE_TIME_LIMIT_TICKS = 120 * 20
        const val STATUS_TICKS = 60

        fun sound(event: SoundEvent) = Sound.sound(event, Sound.Source.PLAYER, 1f, 1f)

        fun statusKind(tone: ScoreTone) =
            when (tone) {
                ScoreTone.GOOD -> StatusKind.GOOD
                ScoreTone.NEUTRAL -> StatusKind.INFO
                ScoreTone.BAD -> StatusKind.BAD
            }
    }
}
