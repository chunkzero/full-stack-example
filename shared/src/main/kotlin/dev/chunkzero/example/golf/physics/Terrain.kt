package dev.chunkzero.example.golf.physics

/** A collision box in block-local coordinates; boxes may extend past their block, as fences do. */
data class Box(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double,
) {
    companion object {
        val FULL = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val BOTTOM_SLAB = Box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)
    }
}

/**
 * How a block treats the ball. [friction] is rolling deceleration (blocks/s²), [bounce] the restitution of landings
 * on it, [wallBounce] the restitution when it's hit side-on, and [grip] the share of horizontal speed a hard landing
 * keeps.
 */
enum class Surface(
    val friction: Double,
    val bounce: Double,
    val wallBounce: Double,
    val grip: Double,
) {
    NORMAL(6.0, 0.35, 0.6, 0.9),
    ICE(1.2, 0.3, 0.7, 0.98),
    SAND(40.0, 0.05, 0.3, 0.3),
    SLIME(6.0, 0.85, 1.2, 0.95),
    HONEY(60.0, 0.0, 0.1, 0.1),

    /** Water or lava: a ball inside it is out of bounds. */
    HAZARD(0.0, 0.0, 0.0, 0.0),
}

/** The blocks the ball simulation sees, by integer block position. */
interface Terrain {
    fun boxes(
        x: Int,
        y: Int,
        z: Int,
    ): List<Box>

    fun surface(
        x: Int,
        y: Int,
        z: Int,
    ): Surface
}
