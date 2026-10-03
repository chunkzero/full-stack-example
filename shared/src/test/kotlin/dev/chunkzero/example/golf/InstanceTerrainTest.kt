package dev.chunkzero.example.golf

import dev.chunkzero.example.golf.physics.Box
import dev.chunkzero.example.golf.physics.Surface
import net.minestom.server.instance.block.Block
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstanceTerrainTest {
    @Test
    fun `vanilla collision shapes and surfaces map onto the terrain model`() {
        assertEquals(listOf(Box.FULL), InstanceTerrain.boxesOf(Block.STONE))
        assertEquals(listOf(Box.BOTTOM_SLAB), InstanceTerrain.boxesOf(Block.STONE_SLAB.withProperty("type", "bottom")))
        assertEquals(2, InstanceTerrain.boxesOf(Block.OAK_STAIRS).size)
        assertEquals(1.5, InstanceTerrain.boxesOf(Block.OAK_FENCE).maxOf { it.maxY })
        assertEquals(emptyList<Box>(), InstanceTerrain.boxesOf(Block.WATER))

        assertEquals(Surface.ICE, InstanceTerrain.surfaceOf(Block.BLUE_ICE))
        assertEquals(Surface.SAND, InstanceTerrain.surfaceOf(Block.SOUL_SAND))
        assertEquals(Surface.HAZARD, InstanceTerrain.surfaceOf(Block.LAVA))
        assertEquals(Surface.NORMAL, InstanceTerrain.surfaceOf(Block.MOSS_BLOCK))
    }
}
