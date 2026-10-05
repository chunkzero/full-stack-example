package com.chunkzero.example.golf

import com.chunkzero.example.golf.physics.Box
import com.chunkzero.example.golf.physics.Surface
import com.chunkzero.example.golf.physics.Terrain
import net.minestom.server.collision.BoundingBox
import net.minestom.server.collision.ShapeImpl
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import java.util.concurrent.ConcurrentHashMap

/** [Terrain] backed by an instance's blocks and their vanilla collision shapes. Unloaded chunks read as air. */
class InstanceTerrain(
    private val instance: Instance,
) : Terrain {
    override fun boxes(
        x: Int,
        y: Int,
        z: Int,
    ): List<Box> = block(x, y, z)?.let(::boxesOf) ?: emptyList()

    override fun surface(
        x: Int,
        y: Int,
        z: Int,
    ): Surface = block(x, y, z)?.let(::surfaceOf) ?: Surface.NORMAL

    private fun block(
        x: Int,
        y: Int,
        z: Int,
    ): Block? =
        if (instance.isChunkLoaded(x shr 4, z shr 4)) instance.getBlock(x, y, z, Block.Getter.Condition.TYPE) else null

    companion object {
        private val shapes = ConcurrentHashMap<Int, List<Box>>()
        private val surfaces =
            mapOf(
                Block.ICE to Surface.ICE,
                Block.PACKED_ICE to Surface.ICE,
                Block.BLUE_ICE to Surface.ICE,
                Block.SAND to Surface.SAND,
                Block.RED_SAND to Surface.SAND,
                Block.SOUL_SAND to Surface.SAND,
                Block.SLIME_BLOCK to Surface.SLIME,
                Block.HONEY_BLOCK to Surface.HONEY,
            ).mapKeys { it.key.id() }

        fun boxesOf(block: Block): List<Box> =
            shapes.computeIfAbsent(block.stateId()) {
                when (val shape = block.collisionShape()) {
                    is ShapeImpl -> shape.boundingBoxes().map(::box)
                    is BoundingBox -> listOf(box(shape))
                    else -> emptyList()
                }
            }

        fun surfaceOf(block: Block): Surface =
            if (block.fluid()) {
                Surface.HAZARD
            } else {
                surfaces[block.id()]
                    ?: Surface.NORMAL
            }

        private fun box(shape: BoundingBox) =
            Box(shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ())
    }
}
