package com.chunkzero.example.golf

import com.chunkzero.example.world.BlockPos
import com.chunkzero.example.world.Marks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CourseTest {
    private val marks =
        """
        {
          "version": 1,
          "points": [
            { "name": "tee-2", "kind": "tee", "position": { "x": 20.5, "y": 65.0, "z": 3.5 }, "yaw": 90.0, "properties": { "hole": "2" } },
            { "name": "tee-1", "kind": "tee", "position": { "x": 0.5, "y": 64.0, "z": 0.5 }, "properties": { "hole": "1" } },
            { "name": "spawn", "kind": "spawn", "position": { "x": 1.0, "y": 70.0, "z": 1.0 }, "tags": ["main"] },
            { "name": "shop", "kind": "npc", "position": { "x": 2.0, "y": 64.0, "z": 2.0 }, "properties": { "id": "shop" } }
          ],
          "blocks": [
            { "name": "cup-1", "kind": "cup", "position": { "x": 0, "y": 63, "z": 12 }, "state": "minecraft:air", "properties": { "hole": "1" } },
            { "name": "cup-2", "kind": "cup", "position": { "x": 30, "y": 64, "z": 3 }, "properties": { "hole": "2" } }
          ],
          "regions": [
            { "name": "hole-2", "kind": "hole", "min": { "x": 18, "y": 60, "z": 0 }, "max": { "x": 32, "y": 70, "z": 6 }, "properties": { "hole": "2", "par": "3", "name": "Windmill Hollow" } },
            { "name": "hole-1", "kind": "hole", "min": { "x": -2, "y": 60, "z": -1 }, "max": { "x": 2, "y": 70, "z": 14 }, "properties": { "hole": "1", "par": "2", "name": "First Steps" } }
          ],
          "paths": [ { "name": "ignored", "points": [] } ]
        }
        """.trimIndent()

    @Test
    fun `holes are built from tee, cup and region marks in hole order`() {
        val course = Course.from(Marks.parse(marks))
        assertEquals(listOf(1, 2), course.holes.map { it.number })
        val windmill = course.hole(2)
        assertEquals("Windmill Hollow", windmill.name)
        assertEquals(3, windmill.par)
        assertEquals(90f, windmill.tee.yaw)
        assertEquals(BlockPos(30, 64, 3), windmill.cup)
        assertEquals(BlockPos(32, 70, 6), windmill.region.max)
        assertEquals(1.0, course.spawn.x())
        assertEquals(listOf("shop"), course.npcs.map { it.id })
    }
}
