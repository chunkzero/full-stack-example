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

class ProfileMenu(
    player: Player,
    initialCard: ProfileCard,
) : ProfileView(player) {
    var card: ProfileCard by state(initialCard)

    override fun name(): Component = text(card.name)

    override fun ballName(): Component = text("Ball: ${card.ballName}")

    override fun level(): Component = text("Level ${card.level}")

    override fun xpText(): Component = text("${card.xp} / ${card.xpForNextLevel} XP")

    override fun xp(index: Int): Boolean = index < card.xp * XP_SIZE / card.xpForNextLevel.coerceAtLeast(1)

    override fun coins(): Component = text(card.coins.toString())

    override fun rounds(): Component = text(card.roundsPlayed.toString())

    override fun aces(): Component = text(card.holesInOne.toString())

    override fun ball(): ItemStack = ItemStack.of(card.ballItem)
}
