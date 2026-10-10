package com.chunkzero.example.golf

import com.chunkzero.example.golf.physics.BallPhysics
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import kotlin.math.atan2
import kotlin.math.floor

/** Farther than this many blocks from their ball, a player has wandered off. */
const val STRAY_DISTANCE = 12.0

/** Spots tried by the ball, in order, as (blocks towards the target, blocks to its left); the last is its own. */
private val SPOTS = listOf(-1.5 to 0.0, -1.0 to 0.0, 0.0 to 1.5, 0.0 to -1.5, -1.5 to 1.5, -1.5 to -1.5, 0.0 to 0.0)

/** Feet heights tried at each spot, relative to the ground under the ball. */
private val LEVELS = listOf(0, 1, -1)

/** Whether [player] has wandered off from their ball at [ball]: too far from it or in water. */
fun strayed(
    player: Player,
    instance: Instance,
    ball: Point,
): Boolean {
    val at = player.position
    if (at.distanceSquared(ball) > STRAY_DISTANCE * STRAY_DISTANCE) return true
    return block(instance, at.blockX(), at.blockY(), at.blockZ())?.fluid() == true
}

/**
 * Stands [player] by their ball at [ball] (its centre), facing along the line to [target]: behind the ball if there is
 * room, else beside it, else on top of it. A spot has room when the player's two blocks are clear and they stand on
 * solid ground.
 */
fun standNear(
    player: Player,
    instance: Instance,
    ball: Point,
    target: Point,
) {
    var dx = target.x() - ball.x()
    var dz = target.z() - ball.z()
    val length = StrictMath.sqrt(dx * dx + dz * dz)
    if (length < 1e-6) {
        val yaw = Math.toRadians(player.position.yaw.toDouble())
        dx = -StrictMath.sin(yaw)
        dz = StrictMath.cos(yaw)
    } else {
        dx /= length
        dz /= length
    }
    val ground = floor(ball.y() - BallPhysics.RADIUS + 1e-3).toInt()
    val spot =
        SPOTS.firstNotNullOfOrNull { (along, left) ->
            val x = floor(ball.x() + dx * along - dz * left).toInt()
            val z = floor(ball.z() + dz * along + dx * left).toInt()
            LEVELS.firstNotNullOfOrNull { level ->
                standingHeight(instance, x, ground + level, z)?.let { Vec(x + 0.5, it, z + 0.5) }
            }
        } ?: Vec(ball.x(), ball.y() - BallPhysics.RADIUS, ball.z())
    player.teleport(Pos(spot.x(), spot.y(), spot.z(), Math.toDegrees(atan2(-dx, dz)).toFloat(), 30f))
}

/** The height a player stands at with their feet in block ([x], [feet], [z]), or null if there is no room. */
private fun standingHeight(
    instance: Instance,
    x: Int,
    feet: Int,
    z: Int,
): Double? {
    val below = block(instance, x, feet - 1, z) ?: return null
    val top = below.collisionShape().relativeEnd().y()
    if (!below.solid() || top > 1.0) return null
    val clear =
        (0..1).all { up ->
            block(instance, x, feet + up, z)?.let { !it.solid() && !it.fluid() } == true
        }
    return if (clear) feet - 1 + top else null
}

private fun block(
    instance: Instance,
    x: Int,
    y: Int,
    z: Int,
): Block? =
    if (instance.isChunkLoaded(x shr 4, z shr 4)) instance.getBlock(x, y, z, Block.Getter.Condition.TYPE) else null
