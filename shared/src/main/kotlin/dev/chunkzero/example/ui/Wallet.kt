package dev.chunkzero.example.ui

import dev.chunkzero.example.ui.generated.WalletView
import dev.oglass.window.Click
import net.kyori.adventure.text.Component

class Wallet(
    initialCoins: Long?,
    private val earn: () -> Unit,
) : WalletView() {
    var coins: Long? by state(initialCoins)

    override fun balance(): Component = Component.text("Coins: ${coins ?: "..."}")

    override fun onEarn(click: Click) = earn()
}
