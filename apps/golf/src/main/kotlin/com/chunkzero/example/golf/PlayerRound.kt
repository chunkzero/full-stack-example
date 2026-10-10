package com.chunkzero.example.golf

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.chunk.multistom.coroutines
import com.chunkzero.example.generated.BackendTypes.Shared.Rounds
import com.chunkzero.example.golf.physics.BallEvent
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.golf.physics.Terrain
import com.chunkzero.example.ui.GolfHudState
import com.chunkzero.example.ui.GolfStatus
import com.chunkzero.example.ui.GolfStatusHud
import com.chunkzero.example.ui.StatusKind
import com.chunkzero.window.sendHud
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The course record a ghost replays: the holder's name and their shots for each hole. */
class Ghost(
    val name: String,
    val shots: List<List<Shot>>,
)

/**
 * One player's nine holes: their ball, putter, HUD and ghost, from the first tee to the last cup. Owned by the
 * player's place in the session, so leaving mid-round cleans everything up. [onHoleFinished] runs with the hole and
 * its score as soon as that score is final. [onRoundFinished] runs as soon as the last score is final and starts the
 * save, and [onResultsReady] hands that save over after the celebration delay that follows.
 */
class PlayerRound(
    private val scope: SessionScope,
    val player: Player,
    private val instance: Instance,
    private val terrain: Terrain,
    private val holes: List<Hole>,
    private val skin: BallSkin,
    private val ghost: Ghost?,
    private val onHoleFinished: (round: PlayerRound, hole: Hole, score: Int) -> Unit,
    private val onRoundFinished: (PlayerRound) -> Deferred<Rounds.RecordResult?>,
    private val onResultsReady: (PlayerRound, Deferred<Rounds.RecordResult?>) -> Unit,
) : AutoCloseable {
    val card = RoundCard(holes.map { it.par })
    private val jobs =
        CoroutineScope(scope.coroutines.coroutineContext + SupervisorJob(scope.coroutines.coroutineContext[Job]))
    private var status: GolfStatus? = null
    private val hud =
        GolfStatusHud(
            object : GolfHudState {
                override val hole get() = holes.getOrNull(holeIndex)?.number ?: 1
                override val holeName get() = holes.getOrNull(holeIndex)?.name ?: ""
                override val par get() = holes.getOrNull(holeIndex)?.par ?: 0
                override val stroke get() = card.holes.getOrNull(holeIndex)?.let { it.score ?: it.strokes } ?: 0
                override val maxStrokes get() = HoleCard.MAX_STROKES
                override val secondsLeft
                    get() = if (playing) ((HOLE_TIME_LIMIT_TICKS - (ticks - holeStartedAt) + 19) / 20).toInt() else null
                override val power get() = if (putter.charging) putter.power.value else null
                override val status get() = this@PlayerRound.status ?: lastStroke()
            },
        )
    private val putter =
        Putter(
            scope,
            player,
            ball = { ball?.position ?: player.position },
            onShot = ::shoot,
            terrain = terrain,
            hole = { holes.getOrNull(holeIndex) },
        )
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
    private val playing: Boolean get() = holeIndex >= 0 && !holeCard.finished

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
        ghost?.let { replay = GhostReplay(scope, instance, terrain, hole, it.shots[index], player, label = it.name) }
        standNear(player, instance, hole.ballStart, cupCenter)
        introduce(last = index == holes.lastIndex)
        holeStartedAt = ticks
        putter.armed = true
    }

    private fun introduce(last: Boolean) {
        val subtitle = "${if (last) "Last hole" else "Hole ${hole.number}"} · Par ${hole.par}"
        player.showTitle(
            Title.title(
                Component.text(hole.name, NamedTextColor.GOLD),
                Component.text(subtitle, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(500)),
            ),
        )
        player.playSound(sound(SoundEvent.BLOCK_NOTE_BLOCK_CHIME, volume = 0.6f))
    }

    private fun shoot(shot: Shot) {
        val ball = ball ?: return
        if (ball.moving || holeCard.finished) return
        holeCard.shoot(shot)
        ball.shoot(shot)
    }

    private fun onBallEvent(event: BallEvent) {
        if (holeCard.finished) return
        when (event) {
            is BallEvent.Sunk -> {
                sunk()
            }

            is BallEvent.Rested -> {
                if (holeCard.rested()) pickedUp() else nextStroke()
            }

            is BallEvent.OutOfBounds -> {
                val splash = event.reason == BallEvent.Reason.HAZARD
                val pickedUp = holeCard.outOfBounds()
                ball?.place(event.resetPosition)
                if (pickedUp) {
                    pickedUp()
                } else {
                    showStatus(GolfStatus(if (splash) "Into the drink, +1" else "Out of bounds, +1", StatusKind.BAD))
                    player.playSound(
                        sound(if (splash) SoundEvent.ENTITY_GENERIC_SPLASH else SoundEvent.BLOCK_NOTE_BLOCK_BASS),
                    )
                    nextStroke()
                }
            }
        }
    }

    private fun nextStroke() {
        keepNearBall()
        putter.armed = true
    }

    /** Brings the player back to their resting ball if they have wandered off; otherwise leaves them where they are. */
    private fun keepNearBall() {
        val ball = ball ?: return
        if (ball.moving || putter.charging) return
        if (strayed(player, instance, ball.position)) standNear(player, instance, ball.position, cupCenter)
    }

    private fun lastStroke(): GolfStatus? =
        if (playing && holeCard.strokes == HoleCard.MAX_STROKES - 1 && ball?.moving == false) {
            GolfStatus("Last stroke", StatusKind.BAD)
        } else {
            null
        }

    private fun sunk() {
        instance.playSound(sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, pitch = 1.6f), cupCenter)
        val score = holeCard.sink()
        val name = scoreName(score, hole.par)
        showStatus(GolfStatus(name.text, statusKind(name.tone)))
        celebrate(holeInOne = score == 1)
        finishHole(score)
    }

    private fun pickedUp() {
        ball?.close()
        showStatus(GolfStatus("Picked up at ${HoleCard.MAX_STROKES}", StatusKind.BAD))
        finishHole(holeCard.score!!)
    }

    private fun timedOut() {
        ball?.close()
        val score = holeCard.timeOut()
        showStatus(GolfStatus("Out of time", StatusKind.BAD))
        player.playSound(sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS))
        finishHole(score)
    }

    private fun finishHole(score: Int) {
        putter.armed = false
        onHoleFinished(this, hole, score)
        val last = holeIndex + 1 >= holes.size
        val save = if (last) onRoundFinished(this) else null
        jobs.launch {
            delay(NEXT_HOLE_DELAY)
            if (last) {
                clearHole()
                if (save != null) onResultsReady(this@PlayerRound, save)
            } else {
                startHole(holeIndex + 1)
            }
        }
    }

    private fun tick() {
        ticks++
        if (statusUntil in 1..ticks) {
            status = null
            statusUntil = 0
        }
        if (playing && ticks - holeStartedAt >= HOLE_TIME_LIMIT_TICKS) timedOut()
        if (playing && ticks % STRAY_CHECK_TICKS == 0L) keepNearBall()
        player.sendHud(hud)
    }

    fun showStatus(status: GolfStatus) {
        this.status = status
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
        player.sendActionBar(Component.empty())
    }

    private companion object {
        val START_DELAY = 3.seconds
        val NEXT_HOLE_DELAY = 3.seconds
        const val HOLE_TIME_LIMIT_TICKS = 120 * 20
        const val STATUS_TICKS = 60
        const val STRAY_CHECK_TICKS = 20

        fun sound(
            event: SoundEvent,
            volume: Float = 1f,
            pitch: Float = 1f,
        ) = Sound.sound(event, Sound.Source.PLAYER, volume, pitch)

        fun statusKind(tone: ScoreTone) =
            when (tone) {
                ScoreTone.GOOD -> StatusKind.GOOD
                ScoreTone.NEUTRAL -> StatusKind.INFO
                ScoreTone.BAD -> StatusKind.BAD
            }
    }
}
