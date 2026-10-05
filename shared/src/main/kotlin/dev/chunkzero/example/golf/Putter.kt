package dev.chunkzero.example.golf

import dev.chunkzero.example.golf.physics.BallPhysics
import dev.chunkzero.example.golf.physics.Shot
import dev.chunkzero.runtime.SessionScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.RGBLike
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.event.EventListener
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule
import kotlin.math.atan2

/**
 * One player's putter. While [armed], using the putter starts charging: [power] sweeps 0 → 1 → 0 every
 * [PERIOD_TICKS] and an aim guide is drawn from the ball along the player's yaw. Using it again calls [onShot] with
 * the player's yaw and the current power, then disarms until the session arms it for the next stroke.
 * Closes itself when the player leaves the session.
 */
class Putter(
    private val scope: SessionScope,
    private val player: Player,
    private val ball: () -> Point,
    private val onShot: (Shot) -> Unit,
) : AutoCloseable {
    private val currentPower = MutableStateFlow(0.0)
    val power: StateFlow<Double> = currentPower.asStateFlow()

    var armed = false
        set(value) {
            field = value
            if (!value) stopCharging()
        }

    val charging: Boolean get() = chargeTicks >= 0

    private var chargeTicks = -1
    private var lastUse = -DEBOUNCE_TICKS.toLong()
    private val listener = EventListener.of(PlayerUseItemEvent::class.java, ::onUse)
    private val task =
        scope.scheduler
            .buildTask(::tick)
            .repeat(TaskSchedule.tick(1))
            .schedule()

    init {
        scope.events.addListener(listener)
        scope.own(player, this)
    }

    private fun onUse(event: PlayerUseItemEvent) {
        if (event.player != player || !isPutter(event.itemStack)) return
        event.isCancelled = true
        // Holding the button repeats uses every few ticks; every packet extends the hold, so only a click after a
        // release gap counts.
        val tick = player.aliveTicks
        val fresh = tick - lastUse >= DEBOUNCE_TICKS
        lastUse = tick
        if (!fresh) return
        when {
            !armed -> {}

            !charging -> {
                chargeTicks = 0
            }

            else -> {
                val shot = Shot(player.position.yaw, currentPower.value)
                armed = false
                onShot(shot)
            }
        }
    }

    private fun tick() {
        if (!charging) return
        chargeTicks++
        val phase = (chargeTicks % PERIOD_TICKS).toDouble() / PERIOD_TICKS
        currentPower.value = if (phase < 0.5) phase * 2 else 2 - phase * 2
        if (chargeTicks % 2 == 0) drawAimGuide()
    }

    private fun drawAimGuide() {
        val yaw = Math.toRadians(player.position.yaw.toDouble())
        val direction = Vec(-StrictMath.sin(yaw), 0.0, StrictMath.cos(yaw))
        val origin = ball()
        val particle = Particle.DUST.withProperties(guideColor(currentPower.value), 0.8f)
        for (step in 1..GUIDE_POINTS) {
            val point = origin.add(direction.mul(step * GUIDE_SPACING))
            player.sendPacket(ParticlePacket(particle, point, Vec.ZERO, 0f, 1))
        }
    }

    private fun stopCharging() {
        chargeTicks = -1
        currentPower.value = 0.0
    }

    override fun close() {
        task.cancel()
        scope.events.removeListener(listener)
    }

    companion object {
        const val PERIOD_TICKS = 40
        private const val DEBOUNCE_TICKS = 5
        private const val GUIDE_POINTS = 6
        private const val GUIDE_SPACING = 0.5
        private val PUTTER = Tag.Boolean("chunk_golf_putter")

        fun item(material: Material = Material.STICK): ItemStack =
            ItemStack.of(material).withCustomName(Component.text("Putter")).withTag(PUTTER, true)

        fun isPutter(item: ItemStack) = item.getTag(PUTTER) == true

        private fun guideColor(power: Double): RGBLike = Color((255 * power).toInt(), (255 * (1 - power)).toInt(), 60)
    }
}

/** Stands [player] [distance] blocks behind the ball at [ball] (its centre), facing [target]. */
fun placeBehind(
    player: Player,
    ball: Point,
    target: Point,
    distance: Double = 1.5,
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
    val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
    player.teleport(Pos(ball.x() - dx * distance, ball.y() - BallPhysics.RADIUS, ball.z() - dz * distance, yaw, 30f))
}
