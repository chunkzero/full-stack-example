package com.chunkzero.example.lobby

import com.chunkzero.chunk.runtime.SessionScope
import com.chunkzero.example.golf.GolfBall
import com.chunkzero.example.golf.Hole
import com.chunkzero.example.golf.InstanceTerrain
import com.chunkzero.example.golf.Putter
import com.chunkzero.example.golf.ballStart
import com.chunkzero.example.golf.physics.BallEvent
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.golf.placeBehind
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
 * for the whole session, armed only while practising.
 */
class PracticeGreen(
    private val scope: SessionScope,
    private val instance: Instance,
    private val hole: Hole,
    private val lookOf: (Player) -> BallLook,
    private val onLeave: (Player) -> Unit,
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
        private var notice: Component? = null
        private var reset: Task? = null
        val ball = GolfBall(scope, instance, terrain, hole, look, onEvent = ::onBall)
        private val status =
            scope.scheduler
                .buildTask(::showStatus)
                .repeat(TaskSchedule.tick(2))
                .schedule()

        init {
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
                    notice = Component.text("Nice putt! (${strokesText()})", NamedTextColor.GOLD)
                    reset =
                        scope.scheduler
                            .buildTask(::restart)
                            .delay(TaskSchedule.seconds(2))
                            .schedule()
                }

                is BallEvent.OutOfBounds -> {
                    notice = Component.text("Out of bounds! Your ball is back where it was.", NamedTextColor.RED)
                    ball.place(event.resetPosition)
                    putter.armed = true
                }
            }
        }

        private fun restart() {
            strokes = 0
            notice = null
            ball.place(hole.ballStart)
            resetting = true
            placeBehind(player, ball.position, hole.cup.bottomCenter())
            putter.armed = true
        }

        private fun showStatus() {
            player.sendActionBar(notice ?: statusLine())
        }

        private fun statusLine(): Component {
            val line =
                Component
                    .text("${hole.name}  ·  Par ${hole.par}  ·  ", NamedTextColor.GRAY)
                    .append(Component.text(strokesText(), NamedTextColor.WHITE))
            if (!putter.charging) return line
            val filled = (putter.power.value * POWER_SEGMENTS).toInt()
            return line
                .append(Component.text("  ·  Power ", NamedTextColor.GRAY))
                .append(Component.text("|".repeat(filled), NamedTextColor.GREEN))
                .append(Component.text("|".repeat(POWER_SEGMENTS - filled), NamedTextColor.DARK_GRAY))
        }

        private fun strokesText() = if (strokes == 1) "1 stroke" else "$strokes strokes"

        override fun close() {
            status.cancel()
            reset?.cancel()
            putter.armed = false
            ball.close()
        }
    }

    private companion object {
        const val POWER_SEGMENTS = 20
    }
}
