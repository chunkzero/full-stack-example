package dev.chunkzero.example.golf

import dev.chunkzero.example.golf.physics.BallEvent
import dev.chunkzero.example.golf.physics.BallPhysics
import dev.chunkzero.example.golf.physics.BallSimulation
import dev.chunkzero.example.golf.physics.Shot
import dev.chunkzero.example.golf.physics.Terrain
import dev.chunkzero.runtime.SessionScope
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.timer.TaskSchedule

/** How a ball looks: any vanilla [item] shown at [scale]. */
data class BallSkin(
    val item: ItemStack,
    val scale: Float,
) {
    companion object {
        val DEFAULT = BallSkin(ItemStack.of(Material.SNOWBALL), 0.6f)

        /** Stained glass renders translucent, which suits ghosts. */
        val GHOST = BallSkin(ItemStack.of(Material.WHITE_STAINED_GLASS), 0.3f)
    }
}

/** Where a hole's ball starts: resting on the ground at the tee. */
val Hole.ballStart: Vec get() = tee.asVec().add(0.0, BallPhysics.RADIUS, 0.0)

/**
 * A simulated ball for one hole, drawn as an item display that follows the simulation every tick. A [viewer] makes it
 * a ghost: glowing and visible to that player only. [onEvent] receives each shot's outcome on the tick thread; the
 * caller decides what happens next, e.g. [place] it back at [BallEvent.OutOfBounds.resetPosition].
 */
class GolfBall(
    scope: SessionScope,
    instance: Instance,
    terrain: Terrain,
    hole: Hole,
    skin: BallSkin = BallSkin.DEFAULT,
    private val viewer: Player? = null,
    private val trail: Particle? = Particle.WHITE_SMOKE,
    private val onEvent: (BallEvent) -> Unit,
) : AutoCloseable {
    val simulation = BallSimulation(terrain, hole.cup, hole.region, hole.ballStart)
    private val display = Entity(scope.process, EntityType.ITEM_DISPLAY)
    private val task =
        scope.scheduler
            .buildTask(::tick)
            .repeat(TaskSchedule.tick(1))
            .schedule()

    val position: Vec get() = simulation.position
    val moving: Boolean get() = simulation.moving

    init {
        display.setNoGravity(true)
        display.editEntityMeta(ItemDisplayMeta::class.java) {
            it.itemStack = skin.item
            it.scale = Vec(skin.scale.toDouble())
            it.posRotInterpolationDuration = 1
            it.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
        }
        if (viewer != null) {
            display.isAutoViewable = false
            display.isGlowing = true
        }
        display.setInstance(instance, position.asPos()).thenRun { viewer?.let(display::addViewer) }
    }

    fun shoot(shot: Shot) = simulation.shoot(shot)

    /** Puts the ball at rest at [center]; it becomes the reset position for out-of-bounds shots. */
    fun place(center: Vec) {
        simulation.place(center)
        display.teleport(center.asPos())
    }

    private fun tick() {
        if (!simulation.moving) return
        val event = simulation.tick()
        display.teleport(position.asPos())
        if (trail != null &&
            simulation.moving
        ) {
            display.sendPacketToViewers(ParticlePacket(trail, position, Vec.ZERO, 0f, 1))
        }
        event?.let(onEvent)
    }

    override fun close() {
        task.cancel()
        display.remove()
    }
}
