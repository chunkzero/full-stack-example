// Adapted from Hollow Cube Polar 1.16.0 (MIT); see META-INF/POLAR-LICENSE.
// Loading only, bound to one ServerProcess's registries; the codec and file layout are Polar's.
package com.chunkzero.example.world

import net.hollowcube.polar.PolarChunk
import net.hollowcube.polar.PolarSection
import net.hollowcube.polar.PolarSection.LightContent
import net.hollowcube.polar.PolarWorld
import net.minestom.server.ServerProcess
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.ChunkLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.Section
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.light.LightCompute
import net.minestom.server.registry.RegistryKey
import net.minestom.server.world.biome.Biome
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private const val SECTION_SIZE = Chunk.CHUNK_SECTION_SIZE

/** Read-only [ChunkLoader] serving the chunks of an in-memory [PolarWorld]. */
class PolarLoader(
    private val process: ServerProcess,
    private val world: PolarWorld,
) : ChunkLoader {
    private val blockCache = ConcurrentHashMap<String, Block>()
    private val biomeCache = ConcurrentHashMap<String, Int>()
    private val plainsBiome by lazy { process.registries().biome().getId(Biome.PLAINS) }

    override fun supportsParallelLoading() = true

    override fun loadChunk(
        instance: Instance,
        chunkX: Int,
        chunkZ: Int,
    ): Chunk? {
        val data = world.chunkAt(chunkX, chunkZ) ?: return null
        val chunk = instance.chunkSupplier.createChunk(instance, chunkX, chunkZ)
        chunk.lockWriteLock()
        try {
            data.sections().forEachIndexed { index, section ->
                if (!section.isEmpty) loadSection(section, chunk.getSection(chunk.minSection + index))
            }
            data.blockEntities().forEach { loadBlockEntity(chunk, it) }
        } finally {
            chunk.unlockWriteLock()
        }
        return chunk
    }

    override fun saveChunk(chunk: Chunk) {}

    /** Parses a palette entry once; states this Minestom version does not know become air and are logged. */
    fun block(state: String): Block =
        blockCache.computeIfAbsent(state) {
            try {
                @Suppress("DEPRECATION")
                ArgumentBlockState.staticParse(it)
            } catch (error: ArgumentSyntaxException) {
                logger.error("Unknown block state {} ({})", it, error.message)
                Block.AIR
            }
        }

    private fun biome(name: String): Int =
        biomeCache.computeIfAbsent(name) {
            val id = process.registries().biome().getId(RegistryKey.unsafeOf<Biome>(it))
            if (id >= 0) {
                id
            } else {
                logger.error("Unknown biome {}", it)
                plainsBiome
            }
        }

    private fun loadSection(
        data: PolarSection,
        section: Section,
    ) {
        val blocks = data.blockPalette().map(::block)
        if (blocks.size == 1) {
            section.blockPalette().fill(blocks[0].stateId())
        } else {
            val indices = data.blockData()
            for (y in 0 until SECTION_SIZE) {
                for (z in 0 until SECTION_SIZE) {
                    for (x in 0 until SECTION_SIZE) {
                        val index = (y * SECTION_SIZE + z) * SECTION_SIZE + x
                        section.blockPalette().set(x, y, z, blocks[indices[index]].stateId())
                    }
                }
            }
        }

        val biomes = data.biomePalette().map(::biome)
        if (biomes.size == 1) {
            section.biomePalette().fill(biomes[0])
        } else {
            val indices = data.biomeData()
            for (y in 0 until 4) {
                for (z in 0 until 4) {
                    for (x in 0 until 4) {
                        section.biomePalette().set(
                            x,
                            y,
                            z,
                            biomes.getOrElse(indices[x + z * 4 + y * 16]) { plainsBiome },
                        )
                    }
                }
            }
        }

        lightArray(data.blockLightContent(), data.blockLight())?.let(section::setBlockLight)
        lightArray(data.skyLightContent(), data.skyLight())?.let(section::setSkyLight)
    }

    private fun loadBlockEntity(
        chunk: Chunk,
        entity: PolarChunk.BlockEntity,
    ) {
        var block = chunk.getBlock(entity.x(), entity.y(), entity.z(), Block.Getter.Condition.TYPE)
        entity.id()?.let { block = block.withHandler(process.blockManager().getHandlerOrDummy(it)) }
        entity.data()?.let { block = block.withNbt(it) }
        chunk.setBlock(entity.x(), entity.y(), entity.z(), block)
    }

    private fun lightArray(
        content: LightContent,
        data: ByteArray?,
    ): ByteArray? =
        when (content) {
            LightContent.MISSING -> null
            LightContent.EMPTY -> LightCompute.EMPTY_CONTENT
            LightContent.FULL -> LightCompute.CONTENT_FULLY_LIT
            LightContent.PRESENT -> data
        }

    private companion object {
        val logger = LoggerFactory.getLogger(PolarLoader::class.java)!!
    }
}
