package com.chunkzero.example.golf

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.example.golf.physics.BallEvent
import com.chunkzero.example.golf.physics.BallPhysics
import com.chunkzero.example.golf.physics.BallSimulation
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.golf.physics.Terrain
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
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
 * A simulated ball for one hole, drawn as an item display that follows the simulation every tick, with a dust [trail]
 * while it's fast and a knock when it hits a wall. A [viewer] makes it a ghost: glowing and, with its sounds, for that
 * player only. A [label] floats above the ball. [onEvent] receives each shot's outcome on the tick thread; the caller
 * decides what happens next, e.g. [place] it back at [BallEvent.OutOfBounds.resetPosition].
 */
class GolfBall(
    scope: SessionScope,
    instance: Instance,
    terrain: Terrain,
    hole: Hole,
    skin: BallSkin = BallSkin.DEFAULT,
    private val viewer: Player? = null,
    label: Component? = null,
    private val trail: Boolean = true,
    private val onEvent: (BallEvent) -> Unit,
) : AutoCloseable {
    val simulation = BallSimulation(terrain, hole.cup, hole.region, hole.ballStart)
    private val audience: Audience = viewer ?: instance
    private val display = Entity(scope.process, EntityType.ITEM_DISPLAY)
    private val tag =
        label?.let { text ->
            Entity(scope.process, EntityType.TEXT_DISPLAY).apply {
                editEntityMeta(TextDisplayMeta::class.java) {
                    it.text = text
                    it.scale = Vec(LABEL_SCALE)
                    it.backgroundColor = LABEL_BACKGROUND
                    it.posRotInterpolationDuration = 1
                    it.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                }
            }
        }
    private var trailDistance = 0.0
    private var lastKnock = -KNOCK_GAP_TICKS.toLong()
    private val task =
        scope.scheduler
            .buildTask(::tick)
            .repeat(TaskSchedule.tick(1))
            .schedule()

    val position: Vec get() = simulation.position
    val moving: Boolean get() = simulation.moving

    init {
        display.editEntityMeta(ItemDisplayMeta::class.java) {
            it.itemStack = skin.item
            it.scale = Vec(skin.scale.toDouble())
            it.posRotInterpolationDuration = 1
            it.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            it.shadowRadius = SHADOW_RADIUS
            it.shadowStrength = SHADOW_STRENGTH
        }
        if (viewer != null) display.isGlowing = true
        spawn(display, instance, position)
        tag?.let { spawn(it, instance, position.add(0.0, LABEL_HEIGHT, 0.0)) }
    }

    private fun spawn(
        entity: Entity,
        instance: Instance,
        at: Vec,
    ) {
        entity.setNoGravity(true)
        if (viewer != null) entity.isAutoViewable = false
        entity.setInstance(instance, at.asPos()).thenRun { viewer?.let(entity::addViewer) }
    }

    fun shoot(shot: Shot) = simulation.shoot(shot)

    /** Puts the ball at rest at [center]; it becomes the reset position for out-of-bounds shots. */
    fun place(center: Vec) {
        simulation.place(center)
        moveTo(center)
    }

    private fun tick() {
        if (!simulation.moving) return
        val event = simulation.tick()
        moveTo(position)
        knock(simulation.wallImpactSpeed)
        if (trail && simulation.moving) dust(simulation.velocity.length())
        event?.let(onEvent)
    }

    private fun moveTo(center: Vec) {
        display.teleport(center.asPos())
        tag?.teleport(center.add(0.0, LABEL_HEIGHT, 0.0).asPos())
    }

    /** Puffs of dust spaced along the ball's path, so they thin out in time as it slows and stop when it's slow. */
    private fun dust(speed: Double) {
        if (speed < TRAIL_MIN_SPEED) {
            trailDistance = 0.0
            return
        }
        trailDistance += speed / BallPhysics.TICKS_PER_SECOND
        if (trailDistance < TRAIL_SPACING) return
        trailDistance = 0.0
        val fast = (speed / BallPhysics.MAX_SPEED).coerceIn(0.0, 1.0)
        val shade = (190 + 60 * fast).toInt()
        val particle = Particle.DUST.withProperties(Color(shade, shade, shade), (0.5 + 0.5 * fast).toFloat())
        val at = position.add(0.0, -BallPhysics.RADIUS / 2, 0.0)
        display.sendPacketToViewers(ParticlePacket(particle, at, Vec.ZERO, 0f, 1))
    }

    private fun knock(speed: Double) {
        val now = display.aliveTicks
        if (speed < KNOCK_MIN_SPEED || now - lastKnock < KNOCK_GAP_TICKS) return
        lastKnock = now
        val volume = (speed / BallPhysics.MAX_SPEED).coerceIn(0.15, 0.8).toFloat()
        val sound = Sound.sound(SoundEvent.BLOCK_STONE_HIT, Sound.Source.PLAYER, volume, 1.3f)
        audience.playSound(sound, position.x(), position.y(), position.z())
    }

    override fun close() {
        task.cancel()
        display.remove()
        tag?.remove()
    }

    private companion object {
        const val SHADOW_RADIUS = 0.2f
        const val SHADOW_STRENGTH = 0.8f
        const val TRAIL_MIN_SPEED = 2.0
        const val TRAIL_SPACING = 0.35
        const val KNOCK_MIN_SPEED = 1.0
        const val KNOCK_GAP_TICKS = 3
        const val LABEL_HEIGHT = 0.45
        const val LABEL_SCALE = 0.6
        const val LABEL_BACKGROUND = 0x40000000
    }
}
