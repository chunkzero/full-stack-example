package dev.chunkzero.example.lobby

import dev.chunkzero.backend.client.QueryResult
import dev.chunkzero.backend.client.WatchState
import dev.chunkzero.example.generated.BackendTypes.Shared.Players.ProfileResult
import dev.chunkzero.example.generated.BackendTypes.Shared.Shop.CatalogResultItem
import dev.chunkzero.example.generated.CoroutineBackendClient
import dev.chunkzero.example.ui.BallSkin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import net.minestom.server.item.Material

/** The course the clubhouse sends players to. */
const val OAKMERE = "oakmere"

/** Each successful snapshot's value, skipping states that carry no result yet or a failed one. */
fun <T> Flow<WatchState<T>>.values(): Flow<T> =
    transform { state ->
        val result = state.snapshot().orElse(null)?.result()
        if (result is QueryResult.Value<T>) emit(result.value())
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
