package com.chunkzero.example.lobby

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.chunk.multistom.coroutines
import com.chunkzero.chunk.runtime.MoveResult
import com.chunkzero.example.generated.Destinations
import com.chunkzero.example.world.BlockRegion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.sound.SoundEvent
import java.time.Duration

/** Whether a player standing at [position] has their feet or head inside the region. */
fun BlockRegion.containsPlayer(position: Point) = contains(position) || contains(position.add(0.0, 1.0, 0.0))

/** Calls [onEnter] once each time a player is found inside one of the [regions] after being outside, with a label above each. */
class Portal(
    scope: SessionScope,
    instance: Instance,
    private val regions: List<BlockRegion>,
    private val onEnter: (Player) -> Unit,
) {
    private val members = HashSet<Player>()

    init {
        regions.forEach { label(scope, instance, it) }
    }

    /** Reconciles membership with where [players] actually are, so teleports count like walking. */
    fun update(players: Collection<Player>) {
        members.retainAll(players.toSet())
        for (player in players) {
            val inside = regions.any { it.containsPlayer(player.position) }
            if (!inside) {
                members.remove(player)
            } else if (members.add(player)) {
                onEnter(player)
            }
        }
    }

    private fun label(
        scope: SessionScope,
        instance: Instance,
        region: BlockRegion,
    ) {
        val label = Entity(scope.process, EntityType.TEXT_DISPLAY)
        label.setNoGravity(true)
        label.editEntityMeta(TextDisplayMeta::class.java) {
            it.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            it.text =
                Component
                    .text()
                    .append(Component.text("Oakmere Links", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .appendNewline()
                    .append(Component.text("Walk through to tee off", NamedTextColor.GRAY))
                    .build()
        }
        val top =
            Pos(
                (region.min.x + region.max.x + 1) / 2.0,
                region.max.y + 1.3,
                (region.min.z + region.max.z + 1) / 2.0,
            )
        label.setInstance(instance, top)
    }
}

/** Sends players to Oakmere Links and explains when they can't go. */
class Departures(
    private val scope: SessionScope,
) {
    private val leaving = HashSet<Player>()

    fun teeOff(player: Player) {
        if (!leaving.add(player)) return
        player.closeInventory()
        player.showTitle(
            Title.title(
                Component.empty(),
                Component.text("Heading to the first tee...", NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(400)),
            ),
        )
        player.playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 0.3f, 1.6f))
        scope.coroutines.launch {
            val result =
                try {
                    scope.move(player, Destinations.Golf.oakmere).await()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                } finally {
                    leaving.remove(player)
                }
            if (player.isOnline) refusal(result)?.let { refuse(player, it) }
        }
    }

    private fun refusal(result: MoveResult?): String? =
        when (result) {
            MoveResult.ACCEPTED, MoveResult.OFFLINE -> null
            MoveResult.FULL -> "Every course is full right now. Try again in a moment."
            MoveResult.STALE -> "You're already on your way. Hold on!"
            MoveResult.UNKNOWN_DESTINATION -> "Oakmere Links isn't open in this release."
            null -> "The course can't be reached right now. Try again in a moment."
        }

    private fun refuse(
        player: Player,
        message: String,
    ) {
        player.sendMessage(Component.text(message, NamedTextColor.RED))
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f))
    }
}
