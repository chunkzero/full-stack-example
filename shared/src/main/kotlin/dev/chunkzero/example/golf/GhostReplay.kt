package dev.chunkzero.example.golf

import dev.chunkzero.example.golf.physics.BallEvent
import dev.chunkzero.example.golf.physics.Shot
import dev.chunkzero.example.golf.physics.Terrain
import dev.chunkzero.runtime.SessionScope
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule

/**
 * Replays recorded [shots] for [hole] from its tee as a ghost ball only [viewer] sees. Each shot fires [delayTicks]
 * after the ghost comes to rest; out-of-bounds shots go back to their reset position like a played ball. Calls
 * [onFinish] and removes the ghost after it sinks or runs out of shots; it also goes away if [viewer] leaves.
 */
class GhostReplay(
    private val scope: SessionScope,
    instance: Instance,
    terrain: Terrain,
    hole: Hole,
    private val shots: List<Shot>,
    viewer: Player,
    skin: BallSkin = BallSkin.GHOST,
    private val delayTicks: Int = 15,
    private val onFinish: () -> Unit = {},
) : AutoCloseable {
    private val ball = GolfBall(scope, instance, terrain, hole, skin, viewer, onEvent = ::onEvent)
    private var next = 0
    private var pending: Task? = null

    init {
        scope.own(viewer, this)
        later(::shootNext)
    }

    private fun onEvent(event: BallEvent) {
        when (event) {
            is BallEvent.Sunk -> {
                later(::finish)
            }

            is BallEvent.Rested -> {
                later(::shootNext)
            }

            is BallEvent.OutOfBounds -> {
                ball.place(event.resetPosition)
                later(::shootNext)
            }
        }
    }

    private fun shootNext() {
        if (next < shots.size) ball.shoot(shots[next++]) else finish()
    }

    private fun finish() {
        close()
        onFinish()
    }

    private fun later(action: () -> Unit) {
        pending =
            scope.scheduler
                .buildTask(action)
                .delay(TaskSchedule.tick(delayTicks))
                .schedule()
    }

    override fun close() {
        pending?.cancel()
        ball.close()
    }
}
