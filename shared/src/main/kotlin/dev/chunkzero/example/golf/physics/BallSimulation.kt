package dev.chunkzero.example.golf.physics

import dev.chunkzero.example.world.BlockPos
import dev.chunkzero.example.world.BlockRegion
import net.minestom.server.coordinate.Vec
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/** A putt: Minecraft [yaw] in degrees (0 = +Z, 90 = -X) and [power] from 0 to 1. */
data class Shot(
    val yaw: Float,
    val power: Double,
) {
    init {
        require(power in 0.0..1.0) { "Power $power is outside 0..1" }
    }
}

sealed interface BallEvent {
    val position: Vec

    data class Rested(
        override val position: Vec,
    ) : BallEvent

    data class Sunk(
        override val position: Vec,
    ) : BallEvent

    /** The ball left play; the round puts it back at [resetPosition], the last place it rested. */
    data class OutOfBounds(
        override val position: Vec,
        val reason: Reason,
        val resetPosition: Vec,
    ) : BallEvent

    enum class Reason { LEFT_REGION, HAZARD }
}

/** Tuning shared by the simulation and its renderers. Distances are blocks, times seconds. */
object BallPhysics {
    const val TICKS_PER_SECOND = 20
    const val SUBSTEPS = 10
    const val DT = 1.0 / (TICKS_PER_SECOND * SUBSTEPS)
    const val RADIUS = 0.15
    const val GRAVITY = 24.0

    /** Launch speed at full power; rolls v²/2a = 27 blocks on a normal surface. */
    const val MAX_SPEED = 18.0

    /** Bumpers can add energy, up to this speed. */
    const val MAX_BUMP_SPEED = 24.0

    /** Highest rise a rolling ball can climb; climbing [height] costs `2 * g * height * STEP_COST` of speed². */
    const val STEP_HEIGHT = 0.5
    const val STEP_COST = 0.6

    /**
     * How far a resting ball sinks into its floor during one substep of gravity. A wall can be tested before the floor
     * has pushed the ball back up, so a rise measures this much taller than it is.
     */
    internal const val SETTLE_DEPTH = GRAVITY * DT * DT

    /** Landings slower than this stop instead of bouncing. */
    const val MIN_BOUNCE_SPEED = 1.5

    /** A ball rolling over the cup faster than this skips across it as if it were covered. */
    const val CAPTURE_SPEED = 6.0

    const val REST_SPEED = 0.05
    const val REST_TICKS = 4

    /** A shot ends as rested after this many ticks whatever the ball is doing. */
    const val MAX_TICKS = 60 * TICKS_PER_SECOND

    private const val GROUND_NORMAL = 0.7
    private const val WALL_NORMAL = 0.2

    internal fun isGround(normalY: Double) = normalY >= GROUND_NORMAL

    internal fun isWall(normalY: Double) = abs(normalY) < WALL_NORMAL
}

/**
 * Deterministic golf ball on a [Terrain]: fixed substeps of pure double math, so the same shots from the same start
 * always produce the same path. [cup] is the air block the ball drops into; leaving [region] is out of bounds.
 */
class BallSimulation(
    private val terrain: Terrain,
    private val cup: BlockPos,
    private val region: BlockRegion,
    start: Vec,
) {
    private var px = start.x()
    private var py = start.y()
    private var pz = start.z()
    private var vx = 0.0
    private var vy = 0.0
    private var vz = 0.0
    private var grounded = false
    private var restTicks = 0
    private var ticks = 0

    /** Ball centre. */
    val position: Vec get() = Vec(px, py, pz)
    val velocity: Vec get() = Vec(vx, vy, vz)

    var moving = false
        private set

    /** Where an out-of-bounds ball goes back to: the start or the last rest position. */
    var restPosition: Vec = start
        private set

    /** Puts the ball at rest at [center], which becomes the reset position. */
    fun place(center: Vec) {
        px = center.x()
        py = center.y()
        pz = center.z()
        vx = 0.0
        vy = 0.0
        vz = 0.0
        moving = false
        grounded = false
        restPosition = center
    }

    fun shoot(shot: Shot) {
        check(!moving) { "The ball is still moving" }
        val yaw = Math.toRadians(shot.yaw.toDouble())
        val speed = BallPhysics.MAX_SPEED * shot.power
        vx = -StrictMath.sin(yaw) * speed
        vy = 0.0
        vz = StrictMath.cos(yaw) * speed
        moving = true
        restTicks = 0
        ticks = 0
    }

    /** Advances one server tick; returns the shot's outcome on the tick it ends, otherwise null. */
    fun tick(): BallEvent? {
        if (!moving) return null
        repeat(BallPhysics.SUBSTEPS) { substep()?.let { return stop(it) } }
        ticks++
        val speedSquared = vx * vx + vy * vy + vz * vz
        restTicks = if (grounded && speedSquared < BallPhysics.REST_SPEED * BallPhysics.REST_SPEED) restTicks + 1 else 0
        if (restTicks >= BallPhysics.REST_TICKS || ticks >= BallPhysics.MAX_TICKS) {
            // Flying above the region is fine, but a ball that stops up there is on a roof, not in play.
            if (py >= region.max.y + 1) return stop(leftRegion())
            restPosition = position
            return stop(BallEvent.Rested(restPosition))
        }
        return null
    }

    private fun stop(event: BallEvent): BallEvent {
        vx = 0.0
        vy = 0.0
        vz = 0.0
        moving = false
        return event
    }

    private fun substep(): BallEvent? {
        val wasGrounded = grounded
        vy -= BallPhysics.GRAVITY * BallPhysics.DT
        px += vx * BallPhysics.DT
        py += vy * BallPhysics.DT
        pz += vz * BallPhysics.DT

        grounded = false
        var ground: Surface? = null
        val boxes = nearbyBoxes()
        passes@ for (pass in 0 until 2) {
            for (box in boxes) {
                when (collide(box, wasGrounded)) {
                    Contact.NONE, Contact.SIDE -> {}

                    Contact.GROUND -> {
                        grounded = true
                        ground = terrain.surface(box.blockX, box.blockY, box.blockZ)
                    }

                    Contact.STEPPED -> {
                        grounded = true
                        break@passes
                    }
                }
            }
        }

        val speed = sqrt(vx * vx + vy * vy + vz * vz)
        if (speed > BallPhysics.MAX_BUMP_SPEED) {
            val scale = BallPhysics.MAX_BUMP_SPEED / speed
            vx *= scale
            vy *= scale
            vz *= scale
        }
        ground?.let(::applyFriction)
        return outcome()
    }

    private fun applyFriction(surface: Surface) {
        val horizontal = sqrt(vx * vx + vz * vz)
        val loss = surface.friction * BallPhysics.DT
        if (horizontal <= loss) {
            vx = 0.0
            vz = 0.0
        } else {
            val scale = (horizontal - loss) / horizontal
            vx *= scale
            vz *= scale
        }
    }

    private fun outcome(): BallEvent? {
        val inCup = px >= cup.x && px < cup.x + 1 && pz >= cup.z && pz < cup.z + 1
        if (inCup && py >= cup.y && py < cup.y + 1 - BallPhysics.RADIUS) return BallEvent.Sunk(position)
        if (terrain.surface(floor(px).toInt(), floor(py).toInt(), floor(pz).toInt()) == Surface.HAZARD) {
            return BallEvent.OutOfBounds(position, BallEvent.Reason.HAZARD, restPosition)
        }
        val inside = px >= region.min.x && px < region.max.x + 1 && pz >= region.min.z && pz < region.max.z + 1
        return if (!inside || py < region.min.y) leftRegion() else null
    }

    private fun leftRegion() = BallEvent.OutOfBounds(position, BallEvent.Reason.LEFT_REGION, restPosition)

    private class WorldBox(
        val blockX: Int,
        val blockY: Int,
        val blockZ: Int,
        val minX: Double,
        val minY: Double,
        val minZ: Double,
        val maxX: Double,
        val maxY: Double,
        val maxZ: Double,
    )

    private enum class Contact { NONE, GROUND, SIDE, STEPPED }

    private fun nearbyBoxes(): List<WorldBox> {
        val r = BallPhysics.RADIUS
        val result = ArrayList<WorldBox>()
        // One extra block below catches boxes taller than a block, such as fences.
        for (x in floor(px - r).toInt()..floor(px + r).toInt()) {
            for (y in floor(py - r).toInt() - 1..floor(py + r).toInt()) {
                for (z in floor(pz - r).toInt()..floor(pz + r).toInt()) {
                    for (b in terrain.boxes(x, y, z)) {
                        result +=
                            WorldBox(x, y, z, x + b.minX, y + b.minY, z + b.minZ, x + b.maxX, y + b.maxY, z + b.maxZ)
                    }
                }
            }
        }
        val fast = vx * vx + vz * vz > BallPhysics.CAPTURE_SPEED * BallPhysics.CAPTURE_SPEED
        if (fast && py >= cup.y + 1) {
            val (x, y, z) = cup
            result += WorldBox(x, y, z, x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
        }
        return result
    }

    private fun collide(
        box: WorldBox,
        wasGrounded: Boolean,
    ): Contact {
        val r = BallPhysics.RADIUS
        val dx = px - px.coerceIn(box.minX, box.maxX)
        val dy = py - py.coerceIn(box.minY, box.maxY)
        val dz = pz - pz.coerceIn(box.minZ, box.maxZ)
        val distanceSquared = dx * dx + dy * dy + dz * dz
        if (distanceSquared >= r * r) return Contact.NONE

        var nx: Double
        var ny: Double
        var nz: Double
        val depth: Double
        if (distanceSquared > 1e-12) {
            val distance = sqrt(distanceSquared)
            nx = dx / distance
            ny = dy / distance
            nz = dz / distance
            depth = r - distance
        } else {
            // Centre inside the box: leave through the nearest face.
            val faces =
                doubleArrayOf(px - box.minX, box.maxX - px, py - box.minY, box.maxY - py, pz - box.minZ, box.maxZ - pz)
            val face = faces.indices.minBy { faces[it] }
            nx =
                if (face == 0) {
                    -1.0
                } else if (face == 1) {
                    1.0
                } else {
                    0.0
                }
            ny =
                if (face == 2) {
                    -1.0
                } else if (face == 3) {
                    1.0
                } else {
                    0.0
                }
            nz =
                if (face == 4) {
                    -1.0
                } else if (face == 5) {
                    1.0
                } else {
                    0.0
                }
            depth = faces[face] + r
        }

        var vn = vx * nx + vy * ny + vz * nz
        if (BallPhysics.isWall(ny) && vn < 0 && wasGrounded && tryStep(box, nx, nz)) return Contact.STEPPED

        px += nx * depth
        py += ny * depth
        pz += nz * depth
        if (BallPhysics.isGround(ny)) {
            // A floor only pushes up. Touching the edge of the next flush block would otherwise turn forward speed
            // into a hop, which then blocks the step-up onto the block after it.
            nx = 0.0
            ny = 1.0
            nz = 0.0
            vn = vy
        }
        if (vn >= 0) return if (BallPhysics.isGround(ny)) Contact.GROUND else Contact.SIDE

        val restitution =
            when {
                BallPhysics.isGround(ny) -> {
                    val surface = terrain.surface(box.blockX, box.blockY, box.blockZ)
                    if (-vn > BallPhysics.MIN_BOUNCE_SPEED) {
                        vx *= surface.grip
                        vz *= surface.grip
                        surface.bounce
                    } else {
                        0.0
                    }
                }

                // Rolling onto an edge from above is inelastic, so the ball rides over or around it.
                ny > 0 && !BallPhysics.isWall(ny) -> {
                    0.0
                }

                else -> {
                    terrain.surface(box.blockX, box.blockY, box.blockZ).wallBounce
                }
            }
        val impulse = (1 + restitution) * vn
        vx -= impulse * nx
        vy -= impulse * ny
        vz -= impulse * nz
        return if (BallPhysics.isGround(ny)) Contact.GROUND else Contact.SIDE
    }

    /** Lifts a rolling ball onto a rise no higher than [BallPhysics.STEP_HEIGHT] if it's fast enough and fits. */
    private fun tryStep(
        box: WorldBox,
        nx: Double,
        nz: Double,
    ): Boolean {
        val r = BallPhysics.RADIUS
        val height = box.maxY - (py - r)
        if (height <= 0 || height > BallPhysics.STEP_HEIGHT + BallPhysics.SETTLE_DEPTH + 1e-9) return false
        val horizontalSquared = vx * vx + vz * vz
        val remaining = horizontalSquared - 2 * BallPhysics.GRAVITY * height * BallPhysics.STEP_COST
        if (remaining <= 0) return false

        val length = sqrt(nx * nx + nz * nz)
        val tx = px - nx / length * r
        val ty = box.maxY + r + 1e-6
        val tz = pz - nz / length * r
        if (overlapsAny(tx, ty, tz)) return false

        px = tx
        py = ty
        pz = tz
        val scale = sqrt(remaining / horizontalSquared)
        vx *= scale
        vy = 0.0
        vz *= scale
        return true
    }

    private fun overlapsAny(
        x: Double,
        y: Double,
        z: Double,
    ): Boolean {
        val r = BallPhysics.RADIUS - 1e-3
        for (bx in floor(x - r).toInt()..floor(x + r).toInt()) {
            for (by in floor(y - r).toInt() - 1..floor(y + r).toInt()) {
                for (bz in floor(z - r).toInt()..floor(z + r).toInt()) {
                    for (b in terrain.boxes(bx, by, bz)) {
                        val dx = x - x.coerceIn(bx + b.minX, bx + b.maxX)
                        val dy = y - y.coerceIn(by + b.minY, by + b.maxY)
                        val dz = z - z.coerceIn(bz + b.minZ, bz + b.maxZ)
                        if (dx * dx + dy * dy + dz * dz < r * r) return true
                    }
                }
            }
        }
        return false
    }
}
