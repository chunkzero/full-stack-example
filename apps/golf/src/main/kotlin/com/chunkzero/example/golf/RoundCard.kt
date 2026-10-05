package com.chunkzero.example.golf

import com.chunkzero.example.golf.physics.Shot

/**
 * Scoring for one hole. Every stroke, penalties included, counts towards [MAX_STROKES]: a ball still in play at the
 * cap is picked up for a score of [MAX_STROKES]. Running out of time scores two over the greater of par and the
 * strokes taken so far, also capped, so a score is always 1..[MAX_STROKES].
 *
 * [shots] holds only the putts actually taken, for ghost replays and the backend; penalties are strokes but not shots.
 */
class HoleCard(
    val par: Int,
) {
    private val taken = ArrayList<Shot>()
    val shots: List<Shot> get() = taken

    var strokes = 0
        private set

    /** The final score once the hole is over. */
    var score: Int? = null
        private set

    val finished: Boolean get() = score != null

    fun shoot(shot: Shot) {
        check(!finished && strokes < MAX_STROKES) { "No strokes left on this hole" }
        taken += shot
        strokes++
    }

    fun sink(): Int = finish(strokes)

    /** After a shot comes to rest; returns true if that used the last stroke and the ball is picked up. */
    fun rested(): Boolean = pickUpAtCap()

    /** Adds the out-of-bounds penalty stroke; returns true if the ball is picked up. */
    fun outOfBounds(): Boolean {
        if (strokes < MAX_STROKES) strokes++
        return pickUpAtCap()
    }

    fun timeOut(): Int = finish(minOf(MAX_STROKES, TIMEOUT_PENALTY + maxOf(strokes, par)))

    private fun pickUpAtCap(): Boolean {
        if (strokes < MAX_STROKES) return false
        finish(MAX_STROKES)
        return true
    }

    private fun finish(score: Int): Int {
        check(!finished) { "The hole is already over" }
        this.score = score
        return score
    }

    companion object {
        const val MAX_STROKES = 8
        const val TIMEOUT_PENALTY = 2
    }
}

/** One player's round over holes with the given [pars]. */
class RoundCard(
    pars: List<Int>,
) {
    val holes: List<HoleCard> = pars.map(::HoleCard)

    /** Scores so far, null for holes not finished. */
    val scores: List<Int?> get() = holes.map { it.score }

    val complete: Boolean get() = holes.all { it.finished }

    val total: Int get() = holes.sumOf { it.score ?: 0 }

    /** Strokes over (positive) or under par across the finished holes. */
    val toPar: Int get() = holes.filter { it.finished }.sumOf { it.score!! - it.par }
}

enum class ScoreTone { GOOD, NEUTRAL, BAD }

data class ScoreName(
    val text: String,
    val tone: ScoreTone,
)

fun scoreName(
    strokes: Int,
    par: Int,
): ScoreName {
    if (strokes == 1) return ScoreName("Hole in one!", ScoreTone.GOOD)
    return when (val difference = strokes - par) {
        in Int.MIN_VALUE..-3 -> ScoreName("Albatross", ScoreTone.GOOD)
        -2 -> ScoreName("Eagle", ScoreTone.GOOD)
        -1 -> ScoreName("Birdie", ScoreTone.GOOD)
        0 -> ScoreName("Par", ScoreTone.NEUTRAL)
        1 -> ScoreName("Bogey", ScoreTone.BAD)
        2 -> ScoreName("Double bogey", ScoreTone.BAD)
        else -> ScoreName("+$difference", ScoreTone.BAD)
    }
}
