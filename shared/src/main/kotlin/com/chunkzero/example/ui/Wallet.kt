package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.WalletView
import com.chunkzero.window.Click
import net.kyori.adventure.text.Component

class Wallet(
    initialCoins: Long?,
    private val earn: () -> Unit,
) : WalletView() {
    var coins: Long? by state(initialCoins)

    override fun balance(): Component = Component.text("Coins: ${coins ?: "..."}")

    override fun onEarn(click: Click) = earn()
}
