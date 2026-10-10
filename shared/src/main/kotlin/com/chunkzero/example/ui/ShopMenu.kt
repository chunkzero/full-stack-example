package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.ShopView
import com.chunkzero.window.Click
import com.chunkzero.window.IndexedClick
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

data class BallSkin(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val item: Material,
    val owned: Boolean,
    val equipped: Boolean,
)

class ShopMenu(
    player: Player,
    initialSkins: List<BallSkin>,
    initialCoins: Int,
    private val onSkinClicked: (BallSkin) -> Unit,
) : ShopView(player) {
    var skins: List<BallSkin> by state(initialSkins)
    var coins: Int by state(initialCoins)
    var message: Component? by state(null)

    private val catalog = list(cells = ENTRY_SIZE, key = BallSkin::id) { skins }

    /** Replaces the catalog state after a purchase or equip, keeping the current page. */
    fun update(
        skins: List<BallSkin>,
        coins: Int,
    ) {
        this.skins = skins
        this.coins = coins
    }

    override fun balance(): Component = text("$coins ◆")

    override fun page(): Component = text("Page ${catalog.page} / ${catalog.pageCount}")

    override fun message(): Component = message ?: text("Click a ball to buy or equip it", UiColors.MUTED)

    override fun canPrevious(): Boolean = catalog.canPrevious()

    override fun canNext(): Boolean = catalog.canNext()

    override fun entryState(index: Int): EntryState {
        val skin = catalog.at(index) ?: return EntryState.EMPTY
        return if (skin.equipped) EntryState.EQUIPPED else EntryState.AVAILABLE
    }

    override fun entryName(index: Int): Component = text(catalog.at(index)?.name.orEmpty())

    override fun entryPrice(index: Int): Component {
        val skin = catalog.at(index) ?: return text("")
        return when {
            skin.equipped -> text("In use", UiColors.GOLD)
            skin.owned -> text("Owned", UiColors.GREEN)
            else -> text("${skin.price} ◆", if (skin.price <= coins) UiColors.GOLD else UiColors.RED)
        }
    }

    override fun entryIcon(index: Int): ItemStack? = catalog.at(index)?.let(::icon)

    override fun onEntry(click: IndexedClick) {
        val skin = catalog.at(click.index) ?: return
        if (skin.owned || skin.price <= coins) {
            message = null
            onSkinClicked(skin)
        } else {
            message = text("Not enough coins: ${skin.price - coins} more needed", UiColors.RED)
        }
    }

    override fun onPrevious(click: Click) = catalog.previous()

    override fun onNext(click: Click) = catalog.next()

    private fun icon(skin: BallSkin): ItemStack {
        val action =
            when {
                skin.equipped -> line("Equipped", UiColors.GOLD)
                skin.owned -> line("Click to equip", UiColors.GREEN)
                skin.price <= coins -> line("Click to buy for ${skin.price} coins", UiColors.GOLD)
                else -> line("Not enough coins (${skin.price})", UiColors.RED)
            }
        return ItemStack
            .builder(skin.item)
            .set(DataComponents.CUSTOM_NAME, line(skin.name, UiColors.WHITE))
            .set(DataComponents.LORE, listOf(line(skin.description, UiColors.MUTED), Component.empty(), action))
            .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, skin.equipped)
            .build()
    }

    private fun line(
        value: String,
        color: TextColor,
    ): Component = text(value, color).decoration(TextDecoration.ITALIC, false)
}
