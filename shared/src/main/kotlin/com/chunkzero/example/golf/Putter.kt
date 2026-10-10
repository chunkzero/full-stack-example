package com.chunkzero.example.golf

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.example.golf.physics.Shot
import com.chunkzero.example.golf.physics.Terrain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.EventListener
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule

/**
 * One player's putter. While [armed], using the putter starts charging: [power] eases 0 → 1 → 0 every
 * [PERIOD_TICKS], slowest at either end, and an [AimGuide] is drawn from the ball along the player's yaw. Using it
 * again calls [onShot] with the player's yaw and the current power, then disarms until the session arms it for the
 * next stroke. With [terrain] and the current [hole], the guide also predicts the ball's path.
 * Closes itself when the player leaves the session.
 */
class Putter(
    private val scope: SessionScope,
    private val player: Player,
    private val ball: () -> Point,
    private val onShot: (Shot) -> Unit,
    terrain: Terrain? = null,
    hole: () -> Hole? = { null },
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
    private val guide = AimGuide(scope, player, terrain, hole)
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
                player.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.PLAYER, 0.4f, 1.4f))
            }

            else -> {
                val shot = Shot(player.position.yaw, currentPower.value)
                val at = ball()
                armed = false
                player.swingMainHand()
                player.instance?.playSound(puttSound(shot.power), at.x(), at.y(), at.z())
                onShot(shot)
            }
        }
    }

    private fun tick() {
        if (!charging) return
        chargeTicks++
        val phase = (chargeTicks % PERIOD_TICKS).toDouble() / PERIOD_TICKS
        val power = (1 - StrictMath.cos(2 * Math.PI * phase)) / 2
        currentPower.value = power
        if (chargeTicks % METER_TICK_TICKS == 0) {
            player.playSound(
                Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.PLAYER, 0.15f, (0.6 + 1.2 * power).toFloat()),
            )
        }
        if (chargeTicks % 2 == 0) guide.show(ball(), Shot(player.position.yaw, power))
    }

    private fun stopCharging() {
        chargeTicks = -1
        currentPower.value = 0.0
        guide.hide()
    }

    override fun close() {
        task.cancel()
        scope.events.removeListener(listener)
        guide.close()
    }

    companion object {
        const val PERIOD_TICKS = 40
        private const val DEBOUNCE_TICKS = 5
        private const val METER_TICK_TICKS = 4
        private val PUTTER = Tag.Boolean("chunk_golf_putter")

        fun item(material: Material = Material.STICK): ItemStack =
            ItemStack.of(material).withCustomName(Component.text("Putter")).withTag(PUTTER, true)

        fun isPutter(item: ItemStack) = item.getTag(PUTTER) == true

        /** A crisp tap for a soft putt, a fuller knock for a hard one. */
        private fun puttSound(power: Double): Sound =
            Sound.sound(
                SoundEvent.BLOCK_WOOD_HIT,
                Sound.Source.PLAYER,
                (0.5 + 0.5 * power).toFloat(),
                (1.8 - 0.6 * power).toFloat(),
            )
    }
}
