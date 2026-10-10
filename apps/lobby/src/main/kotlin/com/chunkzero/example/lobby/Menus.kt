package com.chunkzero.example.lobby

import com.chunkzero.chunk.multistom.SessionScope
import com.chunkzero.chunk.multistom.coroutines
import com.chunkzero.example.generated.BackendTypes.Shared.Courses.CourseArgs
import com.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import com.chunkzero.example.generated.BackendTypes.Shared.Rounds.CourseRecordArgs
import com.chunkzero.example.generated.BackendTypes.Shared.Rounds.PersonalBestArgs
import com.chunkzero.example.generated.BackendTypes.Shared.Shop.BuyArgs
import com.chunkzero.example.generated.BackendTypes.Shared.Shop.BuyResult
import com.chunkzero.example.generated.BackendTypes.Shared.Shop.CatalogResultItem
import com.chunkzero.example.generated.BackendTypes.Shared.Shop.EquipArgs
import com.chunkzero.example.generated.CoroutineBackendClient
import com.chunkzero.example.ui.BallSkin
import com.chunkzero.example.ui.CourseRecord
import com.chunkzero.example.ui.CourseSummary
import com.chunkzero.example.ui.PlayMenu
import com.chunkzero.example.ui.ProfileCard
import com.chunkzero.example.ui.ProfileMenu
import com.chunkzero.example.ui.ShopMenu
import com.chunkzero.window.WindowView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import org.slf4j.LoggerFactory

/**
 * Opens the Play, Shop and Profile menus and keeps open ones live: the course record for everyone, each player's own
 * profile, and the clubhouse head count.
 */
class Menus(
    private val scope: SessionScope,
    private val backend: CoroutineBackendClient,
    private val catalog: Catalog,
    private val visitors: Map<Player, Visitor>,
    private val teeOff: (Player) -> Unit,
) {
    private var course: CourseSummary? = null
    private var record: CourseRecord? = null

    init {
        watching("course record") { backend.shared.rounds.watchCourseRecord(CourseRecordArgs(OAKMERE)) }
            .onEach { live ->
                val latest = live.latest ?: return@onEach
                record = latest.value()?.let { CourseRecord(it.name(), it.total().toInt()) }
                visitors.values.forEach { (it.window?.view as? PlayMenu)?.record = record }
            }.launchIn(scope.coroutines)
    }

    fun open(
        visitor: Visitor,
        item: MenuItem,
    ) = when (item) {
        MenuItem.PLAY -> openPlay(visitor)
        MenuItem.SHOP -> openShop(visitor)
        MenuItem.PROFILE -> openProfile(visitor)
    }

    fun openPlay(visitor: Visitor) =
        load(visitor) {
            PlayMenu(
                visitor.player,
                course = course(),
                initialRecord = record,
                initialPersonalBest = personalBest(visitor),
                initialPlayerCount = visitors.size,
                onTeeOff = { teeOff(visitor.player) },
                onShop = { openShop(visitor) },
                onProfile = { openProfile(visitor) },
            )
        }

    fun openShop(visitor: Visitor) =
        load(visitor) {
            val profile = currentProfile(visitor)
            ShopMenu(
                visitor.player,
                ballSkins(catalog.items(), profile),
                profile.coins().toInt(),
            ) { onSkin(visitor, it) }
        }

    fun openProfile(visitor: Visitor) =
        load(visitor) {
            val profile = currentProfile(visitor)
            ProfileMenu(
                visitor.player,
                card(profile, catalog.items().firstOrNull { it.id() == profile.ball() }),
                coursePar = course().par,
                initialBest = personalBest(visitor),
            )
        }

    private suspend fun personalBest(visitor: Visitor): Int? =
        visitor.backend.shared.rounds
            .personalBest(PersonalBestArgs(OAKMERE))
            ?.toInt()

    /** The watched profile, or a one-off query until the watch delivers its first value. */
    private suspend fun currentProfile(visitor: Visitor): ProfileResult =
        visitor.profile ?: visitor.backend.shared.players
            .profile()

    /** Pushes the live profile into whichever of the player's menus shows it, hinting when it may be out of date. */
    fun profileChanged(
        visitor: Visitor,
        freshness: Freshness,
    ) {
        val profile = visitor.profile ?: return
        when (val view = visitor.window?.view) {
            is ProfileMenu -> {
                view.card = card(profile, catalog.cached.firstOrNull { it.id() == profile.ball() })
            }

            is ShopMenu -> {
                view.update(ballSkins(catalog.cached, profile), profile.coins().toInt())
                val hint = HINTS[freshness]
                if (hint != null) {
                    if (view.message == null || view.message in HINTS.values) view.message = hint
                } else if (view.message in HINTS.values) {
                    view.message = null
                }
            }
        }
    }

    fun playersChanged() {
        visitors.values.forEach { (it.window?.view as? PlayMenu)?.playerCount = visitors.size }
    }

    private suspend fun course(): CourseSummary =
        course ?: requireNotNull(backend.shared.courses.course(CourseArgs(OAKMERE))) { "Unknown course $OAKMERE" }
            .let { CourseSummary(it.name(), it.pars().sumOf(Long::toInt), it.pars().size) }
            .also { course = it }

    /** Builds a menu from backend data and opens it, unless the player left or is already waiting on one. */
    private fun load(
        visitor: Visitor,
        build: suspend () -> WindowView<*>,
    ) {
        if (visitor.busy) return
        visitor.busy = true
        scope.coroutines.launch {
            try {
                val view = build()
                if (visitors[visitor.player] === visitor) {
                    visitor.window?.close()
                    visitor.window = view.open()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.warn("Could not open a menu for {}", visitor.player.username, error)
                visitor.player.sendMessage(
                    Component.text("The clubhouse staff are busy. Try again in a moment.", NamedTextColor.RED),
                )
            } finally {
                visitor.busy = false
            }
        }
    }

    private fun onSkin(
        visitor: Visitor,
        skin: BallSkin,
    ) {
        if (visitor.busy) return
        visitor.busy = true
        scope.coroutines.launch {
            val message =
                try {
                    shop(visitor, skin).also { visitor.settled() }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    logger.warn("Shop action failed for {}", visitor.player.username, error)
                    Component.text("The shop is closed for a moment. Try again.", NamedTextColor.RED)
                } finally {
                    visitor.busy = false
                }
            (visitor.window?.view as? ShopMenu)?.message = message
        }
    }

    private suspend fun shop(
        visitor: Visitor,
        skin: BallSkin,
    ): Component {
        val shop = visitor.backend.shared.shop
        val click = visitor.operation(if (skin.owned) "equip" else "buy", skin.id)
        if (skin.equipped) return Component.text("You're already playing the ${skin.name} ball.", NamedTextColor.GRAY)
        if (!skin.owned) {
            val bought = shop.buy(BuyArgs(skin.id), scope.operationId(visitor.player, click))
            when (bought) {
                BuyResult.bought, BuyResult.owned -> {}

                BuyResult.insufficient -> {
                    return Component.text("Not enough coins for ${skin.name}.", NamedTextColor.RED)
                }

                BuyResult.unknown -> {
                    return Component.text("${skin.name} is no longer sold.", NamedTextColor.RED)
                }
            }
        }
        val equipped = shop.equip(EquipArgs(skin.id), scope.operationId(visitor.player, "$click-equip"))
        return when {
            !equipped -> Component.text("Couldn't equip ${skin.name}.", NamedTextColor.RED)
            skin.owned -> Component.text("Equipped ${skin.name}.", NamedTextColor.GREEN)
            else -> Component.text("Bought and equipped ${skin.name}!", NamedTextColor.GREEN)
        }
    }

    private fun card(
        profile: ProfileResult,
        ball: CatalogResultItem?,
    ) = ProfileCard(
        name = profile.name(),
        level = profile.level().toInt(),
        xp = profile.levelXp().toInt(),
        xpForNextLevel = profile.nextLevelXp().toInt(),
        coins = profile.coins().toInt(),
        roundsPlayed = profile.rounds().toInt(),
        holesInOne = profile.holesInOne().toInt(),
        ballName = ball?.name() ?: profile.ball(),
        ballItem = ballMaterial(ball?.item()),
    )

    private companion object {
        val logger = LoggerFactory.getLogger(Menus::class.java)!!
        val HINTS =
            mapOf(
                Freshness.UPDATING to Component.text("Updating...", NamedTextColor.GRAY),
                Freshness.OFFLINE to Component.text("Offline: showing your last known balance.", NamedTextColor.GRAY),
            )
    }
}
