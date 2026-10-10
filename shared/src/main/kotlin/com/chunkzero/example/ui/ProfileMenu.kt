package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.ProfileView
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
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

/** The player's profile, with their best round on a course of par [coursePar]. */
class ProfileMenu(
    player: Player,
    initialCard: ProfileCard,
    private val coursePar: Int,
    initialBest: Int?,
) : ProfileView(player) {
    var card: ProfileCard by state(initialCard)
    var bestRound: Int? by state(initialBest)

    override fun name(): Component = text(card.name)

    override fun ballName(): Component = text("Ball: ${card.ballName}")

    override fun level(): Component = text("Level ${card.level}")

    override fun xpText(): Component = text("${card.xp} / ${card.xpForNextLevel} XP")

    override fun xp(index: Int): Boolean = index < card.xp * XP_SIZE / card.xpForNextLevel.coerceAtLeast(1)

    override fun coins(): Component = text("${card.coins} ◆")

    override fun rounds(): Component = text(card.roundsPlayed.toString())

    override fun aces(): Component = text(card.holesInOne.toString())

    override fun best(): Component = bestRound?.let { formatRound(it, coursePar) } ?: text("-", UiColors.MUTED)

    override fun ball(): ItemStack = ItemStack.of(card.ballItem)
}
