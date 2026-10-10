package com.chunkzero.example.lobby

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.example.golf.GolfBall
import com.chunkzero.example.golf.Hole
import com.chunkzero.example.golf.InstanceTerrain
import com.chunkzero.example.golf.Putter
import com.chunkzero.example.golf.ballStart
import com.chunkzero.example.golf.physics.BallEvent
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.golf.standNear
import com.chunkzero.example.golf.strayed
import com.chunkzero.example.ui.GolfHudState
import com.chunkzero.example.ui.GolfStatus
import com.chunkzero.example.ui.GolfStatusHud
import com.chunkzero.example.ui.StatusKind
import com.chunkzero.window.HudStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import com.chunkzero.example.golf.BallSkin as BallLook

/**
 * The practice green: stepping onto [hole] swaps the hotbar for a putter and the player's own ball at the tee. Nothing
 * is scored; leaving by any means removes both and calls [onLeave] to restore the hotbar. Each player keeps one putter
 * for the whole session, armed only while practising. The putting HUD joins the player's action bar [huds] meanwhile.
 */
class PracticeGreen(
    private val scope: SessionScope,
    private val instance: Instance,
    private val hole: Hole,
    private val lookOf: (Player) -> BallLook,
    private val onLeave: (Player) -> Unit,
    private val huds: (Player) -> HudStack?,
) {
    private val terrain = InstanceTerrain(instance)
    private val practices = HashMap<Player, Practice>()
    private val putters = HashMap<Player, Putter>()

    fun join(player: Player) {
        putters[player] =
            Putter(
                scope,
                player,
                ball = { practices[player]?.ball?.position ?: player.position },
                onShot = { practices[player]?.onShot(it) },
                terrain = terrain,
                hole = { hole },
            )
        scope.own(
            player,
            AutoCloseable {
                practices.remove(player)?.close()
                putters.remove(player)
            },
        )
    }

    /** Reconciles who is practising with where [players] actually are, so teleports count like walking. */
    fun update(players: Collection<Player>) {
        for (player in players) {
            val practice = practices[player]
            if (practice?.resetting == true) {
                practice.resetting = false
            } else if (hole.region.containsPlayer(player.position)) {
                if (practice == null) start(player)
            } else if (practice != null) {
                stop(player)
            }
        }
    }

    private fun start(player: Player) {
        val putter = putters[player] ?: return
        practices[player] = Practice(player, putter, lookOf(player))
    }

    private fun stop(player: Player) {
        val practice = practices.remove(player) ?: return
        practice.close()
        onLeave(player)
    }

    private inner class Practice(
        private val player: Player,
        private val putter: Putter,
        look: BallLook,
    ) : AutoCloseable {
        /** Set when the practice moves the player itself, so the next check doesn't read the teleport as leaving. */
        var resetting = false
        private var strokes = 0
        private var notice: GolfStatus? = null
        private var reset: Task? = null
        val ball = GolfBall(scope, instance, terrain, hole, look, onEvent = ::onBall)
        private val hud =
            GolfStatusHud(
                object : GolfHudState {
                    override val hole get() = this@PracticeGreen.hole.number
                    override val holeName get() = this@PracticeGreen.hole.name
                    override val par get() = this@PracticeGreen.hole.par
                    override val stroke get() = strokes
                    override val maxStrokes: Int? = null
                    override val secondsLeft: Int? = null
                    override val power get() = if (putter.charging) putter.power.value else null
                    override val status get() = notice
                },
            )

        init {
            huds(player)?.add(hud)
            player.inventory.clear()
            player.inventory.setItemStack(0, Putter.item())
            player.setHeldItemSlot(0)
            player.sendMessage(
                Component.text(
                    "Practice green: right-click the putter to charge, right-click again to putt.",
                    NamedTextColor.GREEN,
                ),
            )
            putter.armed = true
        }

        fun onShot(shot: Shot) {
            strokes++
            notice = null
            ball.shoot(shot)
        }

        private fun onBall(event: BallEvent) {
            when (event) {
                is BallEvent.Rested -> {
                    putter.armed = true
                }

                is BallEvent.Sunk -> {
                    notice = GolfStatus("Nice putt!", StatusKind.GOOD)
                    reset =
                        scope.scheduler
                            .buildTask(::restart)
                            .delay(TaskSchedule.seconds(2))
                            .schedule()
                }

                is BallEvent.OutOfBounds -> {
                    val splash = event.reason == BallEvent.Reason.HAZARD
                    notice = GolfStatus(if (splash) "Into the drink" else "Out of bounds", StatusKind.BAD)
                    ball.place(event.resetPosition)
                    putter.armed = true
                }
            }
        }

        private fun restart() {
            strokes = 0
            notice = null
            ball.place(hole.ballStart)
            if (strayed(player, instance, ball.position)) {
                resetting = true
                standNear(player, instance, ball.position, hole.cup.bottomCenter())
            }
            putter.armed = true
        }

        override fun close() {
            huds(player)?.remove(hud)
            reset?.cancel()
            putter.armed = false
            ball.close()
        }
    }
}
