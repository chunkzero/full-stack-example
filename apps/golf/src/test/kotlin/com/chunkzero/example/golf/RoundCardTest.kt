package com.chunkzero.example.golf

import com.chunkzero.example.golf.physics.Shot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoundCardTest {
    private val putt = Shot(45f, 0.5)

    @Test
    fun `penalties count as strokes but only putts are recorded`() {
        val hole = HoleCard(par = 3)
        hole.shoot(putt)
        assertFalse(hole.outOfBounds())
        hole.shoot(putt)
        assertEquals(3, hole.sink())
        assertEquals(listOf(putt, putt), hole.shots)
    }

    @Test
    fun `a ball still in play at eight strokes is picked up`() {
        val rested = HoleCard(par = 3)
        repeat(7) {
            rested.shoot(putt)
            assertFalse(rested.rested())
        }
        rested.shoot(putt)
        assertTrue(rested.rested())
        assertEquals(8, rested.score)

        val penalised = HoleCard(par = 3)
        repeat(7) { penalised.shoot(putt) }
        assertTrue(penalised.outOfBounds())
        assertEquals(8, penalised.score)
        assertEquals(7, penalised.shots.size)

        val lastPutt = HoleCard(par = 3)
        repeat(8) { lastPutt.shoot(putt) }
        assertTrue(lastPutt.outOfBounds())
        assertEquals(8, lastPutt.score)
    }

    @Test
    fun `timing out scores two over par or strokes taken, capped at eight`() {
        assertEquals(5, HoleCard(par = 3).timeOut())
        val busy =
            HoleCard(par = 3).apply {
                repeat(4) { shoot(putt) }
            }
        assertEquals(6, busy.timeOut())
        assertEquals(4, busy.shots.size)
        val desperate =
            HoleCard(par = 4).apply {
                repeat(7) { shoot(putt) }
            }
        assertEquals(8, desperate.timeOut())
    }

    @Test
    fun `round totals and score to par cover finished holes`() {
        val round = RoundCard(listOf(2, 3, 4))
        round.holes[0].shoot(putt)
        round.holes[0].sink()
        round.holes[1].timeOut()
        assertEquals(listOf(1, 5, null), round.scores)
        assertEquals(6, round.total)
        assertEquals(1, round.toPar)
        assertFalse(round.complete)
    }

    @Test
    fun `scores are named against par`() {
        assertEquals(ScoreName("Hole in one!", ScoreTone.GOOD), scoreName(1, 3))
        assertEquals("Albatross", scoreName(2, 5).text)
        assertEquals("Eagle", scoreName(2, 4).text)
        assertEquals("Birdie", scoreName(2, 3).text)
        assertEquals(ScoreName("Par", ScoreTone.NEUTRAL), scoreName(3, 3))
        assertEquals("Bogey", scoreName(4, 3).text)
        assertEquals("Double bogey", scoreName(5, 3).text)
        assertEquals("Triple bogey", scoreName(6, 3).text)
        assertEquals(ScoreName("+4", ScoreTone.BAD), scoreName(6, 2))
    }
}
