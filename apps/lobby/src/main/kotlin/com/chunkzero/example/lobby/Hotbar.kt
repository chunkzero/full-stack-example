package com.chunkzero.example.lobby

import com.chunkzero.chunk.runtime.SessionScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerSwapItemEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

enum class MenuItem(
    val slot: Int,
    val material: Material,
    val label: String,
    val color: NamedTextColor,
) {
    PLAY(0, Material.COMPASS, "Play", NamedTextColor.GREEN),
    SHOP(4, Material.EMERALD, "Pro Shop", NamedTextColor.GOLD),
    PROFILE(8, Material.BOOK, "Profile", NamedTextColor.AQUA),
    ;

    fun item(): ItemStack =
        ItemStack
            .of(material)
            .withCustomName(
                Component
                    .text(label, color)
                    .append(Component.text("  (right-click)", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false),
            ).withTag(TAG, name)

    companion object {
        private val TAG = Tag.String("lobby_menu")

        fun of(item: ItemStack): MenuItem? = item.getTag(TAG)?.let { tag -> entries.firstOrNull { it.name == tag } }
    }
}

/** The clubhouse hotbar: one item per menu. Players can't drop, move or swap anything in the lobby. */
class Hotbar(
    scope: SessionScope,
    onUse: (Player, MenuItem) -> Unit,
) {
    init {
        scope.events
            .addListener(PlayerUseItemEvent::class.java) { event ->
                val item = MenuItem.of(event.itemStack) ?: return@addListener
                event.isCancelled = true
                onUse(event.player, item)
            }.addListener(ItemDropEvent::class.java) { it.isCancelled = true }
            .addListener(PlayerSwapItemEvent::class.java) { it.isCancelled = true }
            .addListener(InventoryPreClickEvent::class.java) { it.isCancelled = true }
    }

    fun give(player: Player) {
        player.inventory.clear()
        MenuItem.entries.forEach { player.inventory.setItemStack(it.slot, it.item()) }
        player.setHeldItemSlot(MenuItem.PLAY.slot.toByte())
    }
}
