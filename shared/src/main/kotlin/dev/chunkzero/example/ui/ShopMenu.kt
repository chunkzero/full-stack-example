package dev.chunkzero.example.ui

import dev.chunkzero.example.ui.generated.ShopView
import dev.oglass.window.Click
import dev.oglass.window.IndexedClick
import dev.oglass.window.WindowPager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
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
    initialSkins: List<BallSkin>,
    initialCoins: Int,
    private val onSkinClicked: (BallSkin) -> Unit,
) : ShopView() {
    private val pager = WindowPager(cellCount = 9)
    private var opened = false
    private var offset by state(0)

    var skins: List<BallSkin> by state(initialSkins)
    var coins: Int by state(initialCoins)
    var message: Component? by state(null)

    /** Replaces the catalog state after a purchase or equip, keeping the current page. */
    fun update(
        skins: List<BallSkin>,
        coins: Int,
    ) {
        this.skins = skins
        this.coins = coins
        offset = pager.clamp(offset, skins.size)
        if (opened) syncPager()
    }

    override fun balance(): Component = text("◆ $coins", UiColors.GOLD)

    override fun page(): Component = text("Page ${pager.page(offset, skins.size)} / ${pager.pageCount(skins.size)}")

    override fun message(): Component = message ?: text("")

    override fun prevLabel(): Component = pageLabel("< PREV", pager.canPrevious(offset))

    override fun nextLabel(): Component = pageLabel("NEXT >", pager.canNext(offset, skins.size))

    override fun entryName(index: Int): Component = skinAt(index)?.let { text(fit(it.name, 52)) } ?: text("")

    override fun entryPrice(index: Int): Component {
        val skin = skinAt(index) ?: return text("")
        return when {
            skin.equipped -> text("Equipped", UiColors.GOLD)
            skin.owned -> text("Owned", UiColors.GREEN)
            else -> text("${skin.price} ◆", if (skin.price <= coins) UiColors.GOLD else UiColors.RED)
        }
    }

    override fun entryIconItem(index: Int): ItemStack? = skinAt(index)?.let(::icon)

    override fun onEntry(click: IndexedClick) {
        val skin = skinAt(click.index) ?: return
        if (skin.owned || skin.price <= coins) {
            message = null
            onSkinClicked(skin)
        } else {
            message = text("Not enough coins: ${skin.price - coins} more needed", UiColors.RED)
        }
    }

    override fun onPrev(click: Click) {
        offset = pager.previous(offset, skins.size)
        syncPager()
    }

    override fun onNext(click: Click) {
        offset = pager.next(offset, skins.size)
        syncPager()
    }

    override fun onOpen() {
        opened = true
        syncPager()
    }

    override fun onClose() {
        opened = false
    }

    private fun skinAt(cell: Int): BallSkin? = pager.itemIndex(offset, cell, skins.size)?.let(skins::get)

    private fun syncPager() {
        buttonState("prev", if (pager.canPrevious(offset)) "enabled" else "disabled")
        buttonState("next", if (pager.canNext(offset, skins.size)) "enabled" else "disabled")
    }

    private fun pageLabel(
        label: String,
        enabled: Boolean,
    ): Component = text(label, if (enabled) UiColors.WHITE else UiColors.DIM)

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
