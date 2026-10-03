package dev.chunkzero.example.lobby

import dev.chunkzero.backend.client.QueryResult
import dev.chunkzero.backend.client.WatchState
import dev.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import dev.chunkzero.example.generated.BackendTypes.Shared.Shop.CatalogResultItem
import dev.chunkzero.example.generated.CoroutineBackendClient
import dev.chunkzero.example.ui.BallSkin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.minestom.server.item.Material
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/** The course the clubhouse sends players to. */
const val OAKMERE = "oakmere"

private val logger = LoggerFactory.getLogger("dev.chunkzero.example.lobby.Backend")!!
private val MIN_BACKOFF = 1.seconds
private val MAX_BACKOFF = 30.seconds

enum class Freshness { LIVE, UPDATING, OFFLINE }

/** A watch's latest value, if any has arrived, and how current it is. */
class Live<T>(
    val latest: QueryResult.Value<T>?,
    val freshness: Freshness,
)

/**
 * A watch that outlives failures: a collector that falls behind or an ended subscription is logged and resubscribed
 * with a doubling delay, and the last value stays available while [Live.freshness] says it may be out of date. Stops
 * only when the collecting scope is cancelled.
 */
fun <T> watching(
    name: String,
    watch: () -> Flow<WatchState<T>>,
): Flow<Live<T>> =
    flow {
        var latest: QueryResult.Value<T>? = null
        var backoff = MIN_BACKOFF
        while (true) {
            watch()
                .catch { error ->
                    if (error is CancellationException) throw error
                    logger.warn("Watch {} failed; resubscribing", name, error)
                }.collect { state ->
                    val result = state.snapshot().orElse(null)?.result()
                    if (result is QueryResult.Value<T>) {
                        latest = result
                        backoff = MIN_BACKOFF
                    }
                    val freshness =
                        when {
                            state.error().isPresent -> Freshness.OFFLINE
                            state.stale() -> Freshness.UPDATING
                            else -> Freshness.LIVE
                        }
                    emit(Live(latest, freshness))
                }
            emit(Live(latest, Freshness.OFFLINE))
            delay(backoff)
            backoff = minOf(backoff * 2, MAX_BACKOFF)
        }
    }

/** The shop's ball catalog, fetched once per session. */
class Catalog(
    private val backend: CoroutineBackendClient,
) {
    var cached: List<CatalogResultItem> = emptyList()
        private set

    suspend fun items(): List<CatalogResultItem> =
        cached.ifEmpty {
            backend.shared.shop
                .catalog()
                .also { cached = it }
        }
}

fun ballMaterial(item: String?): Material = item?.let(Material::fromKey) ?: Material.SNOWBALL

/** The catalog as shop entries for a player; free balls and the equipped one always count as owned. */
fun ballSkins(
    catalog: List<CatalogResultItem>,
    profile: ProfileResult,
): List<BallSkin> =
    catalog.map {
        BallSkin(
            id = it.id(),
            name = it.name(),
            description = it.description(),
            price = it.price().toInt(),
            item = ballMaterial(it.item()),
            owned = it.price() == 0L || it.id() in profile.owned() || it.id() == profile.ball(),
            equipped = it.id() == profile.ball(),
        )
    }
