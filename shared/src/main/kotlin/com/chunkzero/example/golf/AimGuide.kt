package com.chunkzero.example.golf

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.example.golf.physics.BallPhysics
import com.chunkzero.example.golf.physics.BallSimulation
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.golf.physics.Terrain
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

/**
 * What [player] alone sees while charging a putt: a ribbon from the ball along the aim, as long as the putt would roll
 * on flat ground and coloured by power like the HUD meter, plus, when [terrain] and the [hole] are known, a dashed
 * trail of where the ball will really go, walls included. The entities exist only between [show] and [hide].
 */
class AimGuide(
    private val scope: SessionScope,
    private val player: Player,
    private val terrain: Terrain? = null,
    private val hole: () -> Hole? = { null },
) : AutoCloseable {
    private var ribbon: Entity? = null
    private var ribbonBlock: Block? = null
    private val markers = ArrayList<Marker>()

    /** Draws the guide for [shot] from the ball centred at [ball]. */
    fun show(
        ball: Point,
        shot: Shot,
    ) {
        val instance = player.instance ?: return
        drawRibbon(instance, ball, shot)
        val terrain = terrain
        val hole = hole()
        val path = if (terrain == null || hole == null) emptyList() else predictPath(terrain, hole, ball.asVec(), shot)
        drawPath(instance, path)
    }

    fun hide() {
        ribbon?.remove()
        ribbon = null
        ribbonBlock = null
        markers.forEach { it.entity.remove() }
        markers.clear()
    }

    override fun close() = hide()

    private fun drawRibbon(
        instance: Instance,
        ball: Point,
        shot: Shot,
    ) {
        val at = groundUnder(ball)
        val ribbon = ribbon ?: spawn(instance, at, RIBBON_INTERPOLATION_TICKS).also { ribbon = it }
        if (!ribbon.position.samePoint(at)) ribbon.teleport(at)

        // Rotating the block's +Z onto the aim direction (-sin yaw, 0, cos yaw) is a turn of -yaw about Y. The
        // translation then centres the ribbon's width on the aim line.
        val angle = -Math.toRadians(shot.yaw.toDouble())
        val sin = StrictMath.sin(angle)
        val cos = StrictMath.cos(angle)
        val block = bandBlock(shot.power)
        val swap = block != ribbonBlock
        ribbonBlock = block
        ribbon.editEntityMeta(BlockDisplayMeta::class.java) {
            if (swap) it.setBlockState(block)
            it.transformationInterpolationStartDelta = 0
            it.translation = Vec(-RIBBON_WIDTH / 2 * cos, 0.0, RIBBON_WIDTH / 2 * sin)
            it.leftRotation =
                floatArrayOf(0f, StrictMath.sin(angle / 2).toFloat(), 0f, StrictMath.cos(angle / 2).toFloat())
            it.scale = Vec(RIBBON_WIDTH, RIBBON_HEIGHT, ribbonLength(shot.power))
        }
    }

    private fun drawPath(
        instance: Instance,
        path: List<Vec>,
    ) {
        for ((index, point) in path.withIndex()) {
            val at = groundUnder(point).add(0.0, MARKER_SIZE / 2, 0.0)
            val marker = markers.getOrNull(index) ?: Marker(spawnMarker(instance, at)).also(markers::add)
            if (!marker.entity.position.samePoint(at)) marker.entity.teleport(at)
            marker.shown = true
        }
        for (index in path.size until markers.size) markers[index].shown = false
    }

    private fun spawnMarker(
        instance: Instance,
        at: Pos,
    ): Entity =
        spawn(instance, at, 0).apply {
            editEntityMeta(BlockDisplayMeta::class.java) {
                it.setBlockState(Block.WHITE_CONCRETE)
                it.posRotInterpolationDuration = MARKER_INTERPOLATION_TICKS
                it.translation = Vec(-MARKER_SIZE / 2)
                it.scale = Vec(MARKER_SIZE)
            }
        }

    private fun spawn(
        instance: Instance,
        at: Pos,
        interpolationTicks: Int,
    ): Entity {
        val entity = Entity(scope.process, EntityType.BLOCK_DISPLAY)
        entity.setNoGravity(true)
        entity.isAutoViewable = false
        entity.editEntityMeta(BlockDisplayMeta::class.java) {
            it.transformationInterpolationDuration = interpolationTicks
            it.setBrightness(15, 15)
            it.scale = Vec.ZERO
        }
        entity.setInstance(instance, at).thenRun { entity.addViewer(player) }
        return entity
    }

    /** A path marker; hidden markers stay spawned at zero scale so they can be reused. */
    private class Marker(
        val entity: Entity,
    ) {
        var shown = true
            set(value) {
                if (field == value) return
                field = value
                val scale = if (value) Vec(MARKER_SIZE) else Vec.ZERO
                entity.editEntityMeta(BlockDisplayMeta::class.java) { it.scale = scale }
            }
    }

    companion object {
        /** How far ahead the predicted path looks: 1.5 s. */
        private const val PREVIEW_TICKS = 30
        private const val SAMPLE_TICKS = 2
        private const val MIN_MARKER_GAP = 0.3
        private const val MAX_MARKERS = 16

        private const val RIBBON_WIDTH = 0.12
        private const val RIBBON_HEIGHT = 0.02
        private const val MIN_RIBBON_LENGTH = 0.3
        private const val MAX_RIBBON_LENGTH = 10.0
        private const val RIBBON_INTERPOLATION_TICKS = 2
        private const val MARKER_SIZE = 0.07
        private const val MARKER_INTERPOLATION_TICKS = 2
        private const val GROUND_CLEARANCE = 0.01

        /** The ribbon grows with power over its whole range; the predicted path shows the true distance. */
        fun ribbonLength(power: Double): Double = (MAX_RIBBON_LENGTH * power).coerceAtLeast(MIN_RIBBON_LENGTH)

        /** Green, yellow, then red, in thirds, like the HUD meter. */
        private fun bandBlock(power: Double): Block =
            when {
                power < 1.0 / 3 -> Block.LIME_STAINED_GLASS
                power < 2.0 / 3 -> Block.YELLOW_STAINED_GLASS
                else -> Block.RED_STAINED_GLASS
            }

        private fun groundUnder(ball: Point) = Pos(ball.x(), ball.y() - BallPhysics.RADIUS + GROUND_CLEARANCE, ball.z())

        /**
         * Ball centres along [shot] from [start], sampled every [SAMPLE_TICKS] for up to [PREVIEW_TICKS] and ending
         * where the shot ends. Points closer than [MIN_MARKER_GAP] to the previous one are dropped.
         */
        fun predictPath(
            terrain: Terrain,
            hole: Hole,
            start: Vec,
            shot: Shot,
        ): List<Vec> {
            val simulation = BallSimulation(terrain, hole.cup, hole.region, start)
            simulation.shoot(shot)
            val points = ArrayList<Vec>()
            var last = start
            for (tick in 1..PREVIEW_TICKS) {
                val event = simulation.tick()
                val point = event?.position ?: simulation.position
                if ((event != null || tick % SAMPLE_TICKS == 0) &&
                    point.distanceSquared(last) >= MIN_MARKER_GAP * MIN_MARKER_GAP
                ) {
                    points += point
                    last = point
                }
                if (event != null || points.size >= MAX_MARKERS) break
            }
            return points
        }
    }
}
