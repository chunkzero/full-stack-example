package com.chunkzero.example.world

import com.chunkzero.chunk.multistom.SessionScope
import kotlinx.coroutines.future.await
import net.hollowcube.polar.PolarReader
import net.hollowcube.polar.PolarWorld
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.world.DimensionType
import java.util.concurrent.CompletableFuture

/**
 * A World Builder world shipped as `/worlds/<name>/world.polar` with its `marks.json` sidecar. Parse it once per
 * process (e.g. as a process component) and [load] a fresh instance per session.
 */
class GameWorld private constructor(
    val name: String,
    private val polar: PolarWorld,
    val marks: Marks,
) {
    /** Creates an instance owned by [scope] with every chunk of the world loaded and lit. */
    suspend fun load(scope: SessionScope): InstanceContainer {
        val instance = scope.createInstance(DimensionType.OVERWORLD, PolarLoader(scope.process, polar))
        instance.setChunkSupplier(::LightingChunk)
        val chunks = polar.chunks().map { instance.loadChunk(it.x(), it.z()) }
        CompletableFuture.allOf(*chunks.toTypedArray()).await()
        return instance
    }

    companion object {
        fun read(name: String): GameWorld =
            GameWorld(
                name,
                PolarReader.read(resource("/worlds/$name/world.polar")),
                Marks.parse(resource("/worlds/$name/marks.json").decodeToString()),
            )

        private fun resource(path: String): ByteArray =
            requireNotNull(
                GameWorld::class.java.getResourceAsStream(path),
            ) { "Missing resource $path" }.use { it.readAllBytes() }
    }
}
