package dev.chunkzero.example.world

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec

/** The subset of World Builder's `marks.json` sidecar the example reads: points, blocks and regions. */
@Serializable
data class Marks(
    val version: Int = 1,
    val points: List<PointMark> = emptyList(),
    val blocks: List<BlockMark> = emptyList(),
    val regions: List<RegionMark> = emptyList(),
) {
    val all: List<Mark> get() = points + blocks + regions

    inline fun <reified T : Mark> ofKind(kind: String): List<T> = all.filterIsInstance<T>().filter { it.kind == kind }

    companion object {
        private const val SUPPORTED_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Marks {
            val marks = json.decodeFromString<Marks>(text)
            require(
                marks.version <= SUPPORTED_VERSION,
            ) { "marks.json version ${marks.version} is newer than $SUPPORTED_VERSION" }
            return marks
        }
    }
}

sealed interface Mark {
    val name: String
    val kind: String?
    val tags: Set<String>
    val properties: Map<String, String>

    fun string(key: String): String = requireNotNull(properties[key]) { "Mark $name has no $key" }

    fun int(key: String): Int = requireNotNull(string(key).toIntOrNull()) { "Mark $name has a non-integer $key" }
}

@Serializable
data class MarkVec(
    val x: Double,
    val y: Double,
    val z: Double,
)

@Serializable
data class BlockPos(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun toVec() = Vec(x.toDouble(), y.toDouble(), z.toDouble())

    /** Centre of the block's bottom face. */
    fun bottomCenter() = Vec(x + 0.5, y.toDouble(), z + 0.5)
}

/** Axis-aligned box of blocks with inclusive [min] and [max] block coordinates. */
@Serializable
data class BlockRegion(
    val min: BlockPos,
    val max: BlockPos,
) {
    fun contains(
        x: Double,
        y: Double,
        z: Double,
    ) = x >= min.x && x < max.x + 1 && y >= min.y && y < max.y + 1 && z >= min.z && z < max.z + 1

    fun contains(point: Point) = contains(point.x(), point.y(), point.z())
}

@Serializable
data class PointMark(
    override val name: String,
    override val kind: String? = null,
    val position: MarkVec,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    override val tags: Set<String> = emptySet(),
    override val properties: Map<String, String> = emptyMap(),
) : Mark {
    val pos: Pos get() = Pos(position.x, position.y, position.z, yaw, pitch)
}

@Serializable
data class BlockMark(
    override val name: String,
    override val kind: String? = null,
    val position: BlockPos,
    val state: String? = null,
    override val tags: Set<String> = emptySet(),
    override val properties: Map<String, String> = emptyMap(),
) : Mark

@Serializable
data class RegionMark(
    override val name: String,
    override val kind: String? = null,
    val min: BlockPos,
    val max: BlockPos,
    override val tags: Set<String> = emptySet(),
    override val properties: Map<String, String> = emptyMap(),
) : Mark {
    val region: BlockRegion get() = BlockRegion(min, max)
}
