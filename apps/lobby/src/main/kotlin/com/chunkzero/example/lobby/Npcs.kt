package com.chunkzero.example.lobby

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.example.golf.Npc
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.entity.metadata.avatar.MannequinMeta
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.instance.Instance

/** What a clubhouse NPC is called, what it says under its name, and which menu it opens. */
private data class Role(
    val name: String,
    val tagline: String,
    val menu: MenuItem,
)

private val roles =
    mapOf(
        "shop" to Role("Pro Shop", "Ball skins for sale", MenuItem.SHOP),
        "profile" to Role("Reception", "Your golfer profile", MenuItem.PROFILE),
        "caddie" to Role("Caddie", "Ready for a round?", MenuItem.PLAY),
    )

/** Spawns a mannequin at each NPC mark that opens its menu when a player clicks it. */
fun spawnNpcs(
    scope: SessionScope,
    instance: Instance,
    npcs: List<Npc>,
    onClick: (Player, MenuItem) -> Unit,
) {
    val menus = HashMap<Entity, MenuItem>()
    for (npc in npcs) {
        val role = requireNotNull(roles[npc.id]) { "Unknown NPC ${npc.id}" }
        val mannequin = Entity(scope.process, EntityType.MANNEQUIN)
        mannequin.setNoGravity(true)
        mannequin.set(DataComponents.CUSTOM_NAME, Component.text(role.name, NamedTextColor.GOLD))
        mannequin.isCustomNameVisible = true
        mannequin.editEntityMeta(MannequinMeta::class.java) {
            it.isImmovable = true
            it.description = Component.text(role.tagline, NamedTextColor.GRAY)
        }
        mannequin.setInstance(instance, npc.position)
        menus[mannequin] = role.menu
    }
    scope.events
        .addListener(PlayerEntityInteractEvent::class.java) { event ->
            if (event.hand != PlayerHand.MAIN) return@addListener
            menus[event.target]?.let { onClick(event.player, it) }
        }.addListener(EntityAttackEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            menus[event.target]?.let { onClick(player, it) }
        }
}
