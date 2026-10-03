package dev.chunkzero.example.ui

import dev.chunkzero.example.ui.generated.ProfileView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

data class ProfileCard(
    val name: String,
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val coins: Int,
    val roundsPlayed: Int,
    val holesInOne: Int,
    val ballName: String,
    val ballItem: Material,
)

class ProfileMenu(
    initialCard: ProfileCard,
) : ProfileView() {
    var card: ProfileCard by state(initialCard)

    override fun name(): Component = text(card.name)

    override fun ballName(): Component = text("Ball: ${card.ballName}", UiColors.MUTED)

    override fun level(): Component = text("Level ${card.level}", UiColors.GOLD)

    override fun xpText(): Component = text("${card.xp} / ${card.xpForNextLevel} XP", UiColors.MUTED)

    override fun xpBar(): Component = progressBar(card.xp.toDouble() / card.xpForNextLevel.coerceAtLeast(1))

    override fun coins(): Component = text(card.coins.toString(), UiColors.GOLD)

    override fun rounds(): Component = text(card.roundsPlayed.toString(), UiColors.GOLD)

    override fun aces(): Component = text(card.holesInOne.toString(), UiColors.GOLD)

    override fun ballItem(): ItemStack = ItemStack.of(card.ballItem)

    private fun progressBar(fraction: Double): Component {
        val filled = (fraction.coerceIn(0.0, 1.0) * XP_SEGMENTS).toInt()
        return segments(filled, XP_SEGMENTS) { UiColors.GREEN }
    }

    private companion object {
        const val XP_SEGMENTS = 16
    }
}

internal fun segments(
    filled: Int,
    total: Int,
    color: (index: Int) -> TextColor,
): Component {
    val bar = Component.text()
    repeat(total) { i ->
        bar.append(Component.text("█", if (i < filled) color(i) else UiColors.DIM))
    }
    return bar.build()
}
