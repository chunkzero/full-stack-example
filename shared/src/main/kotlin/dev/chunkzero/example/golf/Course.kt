package dev.chunkzero.example.golf

import dev.chunkzero.example.world.BlockMark
import dev.chunkzero.example.world.BlockPos
import dev.chunkzero.example.world.BlockRegion
import dev.chunkzero.example.world.Marks
import dev.chunkzero.example.world.PointMark
import dev.chunkzero.example.world.RegionMark
import net.minestom.server.coordinate.Pos

/** One hole: the ball starts at [tee] and drops into the air block at [cup]; leaving [region] is out of bounds. */
data class Hole(
    val number: Int,
    val name: String,
    val par: Int,
    val tee: Pos,
    val cup: BlockPos,
    val region: BlockRegion,
)

data class Portal(
    val region: BlockRegion,
    val destination: String,
)

data class Leaderboard(
    val position: Pos,
    val board: String,
)

data class Npc(
    val position: Pos,
    val id: String,
)

/** The typed view of a world's marks. Hole 0 is the lobby practice green. */
class Course(
    /** Holes sorted by number. */
    val holes: List<Hole>,
    val spawns: List<Pos>,
    val portals: List<Portal>,
    val leaderboards: List<Leaderboard>,
    val npcs: List<Npc>,
) {
    fun hole(number: Int): Hole = requireNotNull(holes.firstOrNull { it.number == number }) { "No hole $number" }

    /** Holes played in a round, i.e. all but the practice green. */
    val roundHoles: List<Hole> get() = holes.filter { it.number > 0 }

    val spawn: Pos get() = spawns.firstOrNull() ?: holes.first().tee

    companion object {
        fun from(marks: Marks): Course {
            val tees = marks.ofKind<PointMark>("tee").associateUniqueBy { it.int("hole") }
            val cups = marks.ofKind<BlockMark>("cup").associateUniqueBy { it.int("hole") }
            val holes =
                marks
                    .ofKind<RegionMark>("hole")
                    .map {
                        val number = it.int("hole")
                        Hole(
                            number = number,
                            name = it.string("name"),
                            par = it.int("par"),
                            tee = requireNotNull(tees[number]) { "Hole $number has no tee" }.pos,
                            cup = requireNotNull(cups[number]) { "Hole $number has no cup" }.position,
                            region = it.region,
                        )
                    }.sortedBy { it.number }
            require(holes.isNotEmpty()) { "The world has no holes" }
            holes.zipWithNext().forEach { (a, b) ->
                require(a.number != b.number) { "Hole ${a.number} is marked twice" }
            }
            return Course(
                holes = holes,
                spawns = marks.ofKind<PointMark>("spawn").map { it.pos },
                portals = marks.ofKind<RegionMark>("portal").map { Portal(it.region, it.string("destination")) },
                leaderboards = marks.ofKind<PointMark>("leaderboard").map { Leaderboard(it.pos, it.string("board")) },
                npcs = marks.ofKind<PointMark>("npc").map { Npc(it.pos, it.string("id")) },
            )
        }

        private fun <T> List<T>.associateUniqueBy(key: (T) -> Int): Map<Int, T> =
            groupBy(key).mapValues { (number, marks) ->
                require(marks.size == 1) { "Hole $number has ${marks.size} marks of the same kind" }
                marks.single()
            }
    }
}
