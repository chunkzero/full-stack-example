package dev.chunkzero.example.golf.physics

import dev.chunkzero.example.world.BlockPos
import dev.chunkzero.example.world.BlockRegion
import net.minestom.server.coordinate.Vec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Shots travel along +Z (yaw 0) over a flat floor whose top is at y = 0. */
class BallSimulationTest {
    private class Grid : Terrain {
        val blocks = HashMap<BlockPos, Pair<List<Box>, Surface>>()

        fun set(
            x: Int,
            y: Int,
            z: Int,
            surface: Surface = Surface.NORMAL,
            boxes: List<Box> = listOf(Box.FULL),
        ) {
            blocks[BlockPos(x, y, z)] = boxes to surface
        }

        fun fill(
            xs: IntRange,
            y: Int,
            zs: IntRange,
            surface: Surface = Surface.NORMAL,
            boxes: List<Box> = listOf(Box.FULL),
        ) {
            for (x in xs) for (z in zs) set(x, y, z, surface, boxes)
        }

        fun clear(
            x: Int,
            y: Int,
            z: Int,
        ) {
            blocks.remove(BlockPos(x, y, z))
        }

        override fun boxes(
            x: Int,
            y: Int,
            z: Int,
        ) = blocks[BlockPos(x, y, z)]?.first ?: emptyList()

        override fun surface(
            x: Int,
            y: Int,
            z: Int,
        ) = blocks[BlockPos(x, y, z)]?.second ?: Surface.NORMAL
    }

    private val start = Vec(0.5, BallPhysics.RADIUS, 0.5)
    private val farCup = BlockPos(100, 0, 100)
    private val region = BlockRegion(BlockPos(-10, -5, -10), BlockPos(10, 10, 80))

    private fun green(surface: Surface = Surface.NORMAL) = Grid().apply { fill(-10..10, -1, -10..80, surface) }

    private fun simulate(
        terrain: Terrain,
        shot: Shot,
        from: Vec = start,
        cup: BlockPos = farCup,
    ): BallEvent {
        val ball = BallSimulation(terrain, cup, region, from)
        ball.shoot(shot)
        repeat(BallPhysics.MAX_TICKS + 1) { ball.tick()?.let { return it } }
        error("The shot never ended")
    }

    private fun rolled(event: BallEvent) = event.position.z() - start.z()

    @Test
    fun `a full power putt rolls 25 to 30 blocks and stops on a flat green`() {
        val event = simulate(green(), Shot(0f, 1.0))
        assertInstanceOf(BallEvent.Rested::class.java, event)
        assertTrue(rolled(event) in 25.0..30.0, "rolled ${rolled(event)}")
        assertEquals(start.x(), event.position.x(), 1e-9)
        assertEquals(BallPhysics.RADIUS, event.position.y(), 1e-2)
    }

    @Test
    fun `ice rolls farther than the green and sand stops quicker`() {
        val normal = rolled(simulate(green(), Shot(0f, 0.4)))
        val ice = rolled(simulate(green(Surface.ICE), Shot(0f, 0.4)))
        val sand = rolled(simulate(green(Surface.SAND), Shot(0f, 0.4)))
        assertTrue(ice > normal * 2, "ice $ice vs normal $normal")
        assertTrue(sand < normal / 4, "sand $sand vs normal $normal")
    }

    @Test
    fun `a wall bounces the ball back and slime bumps it harder`() {
        fun bounce(surface: Surface): Double {
            val terrain =
                green().apply {
                    fill(-10..10, 0, 5..5, surface)
                    fill(-10..10, 1, 5..5, surface)
                }
            return rolled(simulate(terrain, Shot(0f, 0.6)))
        }
        val stone = bounce(Surface.NORMAL)
        val slime = bounce(Surface.SLIME)
        assertTrue(stone < 4.0, "stone wall left the ball at $stone")
        assertTrue(slime < stone, "slime bumper $slime should send it farther back than stone $stone")
    }

    @Test
    fun `a ball dropped on slime bounces higher than on the green`() {
        fun peakAfterLanding(surface: Surface): Double {
            val ball = BallSimulation(green(surface), farCup, region, Vec(0.5, 3.0, 0.5))
            ball.shoot(Shot(0f, 0.0))
            var landed = false
            var peak = 0.0
            repeat(60) {
                ball.tick()
                if (ball.velocity.y() > 0) landed = true
                if (landed) peak = maxOf(peak, ball.position.y())
            }
            return peak
        }
        val slime = peakAfterLanding(Surface.SLIME)
        assertTrue(slime > 1.5, "slime peak $slime")
        assertTrue(slime > peakAfterLanding(Surface.NORMAL) * 2)
    }

    @Test
    fun `a half slab is climbable with speed but a full block is a wall`() {
        fun withStep(boxes: List<Box>) = green().apply { fill(-10..10, 0, 5..80, boxes = boxes) }
        val slab = withStep(listOf(Box.BOTTOM_SLAB))

        val climbed = simulate(slab, Shot(0f, 0.6))
        assertTrue(climbed.position.z() > 6, "fast ball stopped at ${climbed.position}")
        assertEquals(0.5 + BallPhysics.RADIUS, climbed.position.y(), 1e-2)
        assertTrue(rolled(climbed) < rolled(simulate(green(), Shot(0f, 0.6))), "climbing should cost speed")

        val stalled = simulate(slab, Shot(0f, 0.15))
        assertTrue(stalled.position.z() < 5, "slow ball should not climb, ended at ${stalled.position}")

        val wall = simulate(withStep(listOf(Box.FULL)), Shot(0f, 1.0))
        assertTrue(wall.position.z() < 5, "full block let the ball through to ${wall.position}")
    }

    @Test
    fun `a slow putt sinks while a fast one skips over the cup`() {
        val cup = BlockPos(0, -1, 3)
        val terrain = green().apply { clear(0, -1, 3) }
        assertInstanceOf(BallEvent.Sunk::class.java, simulate(terrain, Shot(0f, 0.35), cup = cup))
        assertInstanceOf(BallEvent.Rested::class.java, simulate(terrain, Shot(0f, 0.9), cup = cup))
    }

    @Test
    fun `a ball rolling on a level below the cup does not sink`() {
        val cup = BlockPos(0, -1, 3)
        val lower = Grid().apply { fill(-10..10, -4, -10..80) }
        val event = simulate(lower, Shot(0f, 0.35), from = Vec(0.5, -3 + BallPhysics.RADIUS, 0.5), cup = cup)
        assertInstanceOf(BallEvent.Rested::class.java, event)
    }

    @Test
    fun `a ball may fly above the region and return but is out of bounds when it rests above it`() {
        val high = Vec(0.5, region.max.y + 3.0, 0.5)
        assertInstanceOf(BallEvent.Rested::class.java, simulate(green(), Shot(0f, 0.0), from = high))

        val roof = green().apply { fill(-10..10, region.max.y + 1, -10..80) }
        val onRoof = simulate(roof, Shot(0f, 0.3), from = high)
        assertEquals(BallEvent.Reason.LEFT_REGION, (onRoof as BallEvent.OutOfBounds).reason)
        assertEquals(high, onRoof.resetPosition)
    }

    @Test
    fun `leaving the region or entering water is out of bounds`() {
        val left = simulate(green(), Shot(180f, 1.0))
        assertEquals(BallEvent.Reason.LEFT_REGION, (left as BallEvent.OutOfBounds).reason)
        assertEquals(start, left.resetPosition)

        val pond = green().apply { fill(-10..10, -1, 6..7, Surface.HAZARD, boxes = emptyList()) }
        val wet = simulate(pond, Shot(0f, 0.5))
        assertEquals(BallEvent.Reason.HAZARD, (wet as BallEvent.OutOfBounds).reason)
    }

    @Test
    fun `the same shots produce identical paths`() {
        val terrain =
            green().apply {
                fill(-10..10, 0, 12..12, Surface.SLIME)
                fill(-3..3, 0, 6..6, boxes = listOf(Box.BOTTOM_SLAB))
                fill(-10..-2, -1, 0..80, Surface.ICE)
            }
        val shots = listOf(Shot(-17.5f, 0.83), Shot(140f, 0.41), Shot(33.3f, 0.66))

        fun path(): List<Vec> {
            val ball = BallSimulation(terrain, farCup, region, start)
            val path = ArrayList<Vec>()
            for (shot in shots) {
                ball.shoot(shot)
                while (ball.moving) {
                    val event = ball.tick()
                    path += ball.position
                    if (event is BallEvent.OutOfBounds) ball.place(event.resetPosition)
                }
            }
            return path
        }
        val first = path()
        assertTrue(first.size > 40)
        assertEquals(first, path())
    }
}
