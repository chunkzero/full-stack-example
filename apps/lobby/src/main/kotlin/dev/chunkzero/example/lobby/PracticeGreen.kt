package dev.chunkzero.example.lobby

import dev.chunkzero.example.golf.GolfBall
import dev.chunkzero.example.golf.Hole
import dev.chunkzero.example.golf.InstanceTerrain
import dev.chunkzero.example.golf.Putter
import dev.chunkzero.example.golf.ballStart
import dev.chunkzero.example.golf.physics.BallEvent
import dev.chunkzero.example.golf.physics.Shot
import dev.chunkzero.example.golf.placeBehind
import dev.chunkzero.runtime.SessionScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import dev.chunkzero.example.golf.BallSkin as BallLook

/**
 * The practice green: stepping onto [hole] swaps the hotbar for a putter and the player's own ball at the tee. Nothing
 * is scored; walking off removes both and calls [onLeave] to restore the hotbar.
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

    init {
        scope.events.addListener(PlayerMoveEvent::class.java) { event ->
            val inside = hole.region.containsPlayer(event.newPosition)
            if (inside == hole.region.containsPlayer(event.player.position)) return@addListener
            if (inside) start(event.player) else stop(event.player)
        }
    }

    fun join(player: Player) {
        scope.own(player, AutoCloseable { practices.remove(player)?.close() })
    }

    private fun start(player: Player) {
        if (player in practices) return
        practices[player] = Practice(player, lookOf(player))
    }

    private fun stop(player: Player) {
        val practice = practices.remove(player) ?: return
        practice.close()
        onLeave(player)
    }

    private inner class Practice(
        private val player: Player,
        look: BallLook,
    ) : AutoCloseable {
        private var strokes = 0
        private var notice: Component? = null
        private var reset: Task? = null
        private val ball = GolfBall(scope, instance, terrain, hole, look, onEvent = ::onBall)
        private val putter = Putter(scope, player, ball = { ball.position }, onShot = ::onShot)
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

        private fun onShot(shot: Shot) {
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
            putter.close()
            ball.close()
        }
    }

    private companion object {
        const val POWER_SEGMENTS = 20
    }
}
