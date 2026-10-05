package com.chunkzero.example.lobby

import com.chunkzero.example.generated.BackendTypes.Shared.Leaderboards.LeaderboardArgs.Board
import com.chunkzero.example.generated.BackendTypes.Shared.Leaderboards.LeaderboardResultItem
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LeaderboardsTest {
    @Test
    fun `values carry the board's unit`() {
        assertEquals("31 strokes", formatValue(Board.course_record, 31))
        assertEquals("1 ace", formatValue(Board.holes_in_one, 1))
        assertEquals("1,250 coins", formatValue(Board.coins, 1250))
    }

    @Test
    fun `boards list ten ranks and fill missing ones with dashes`() {
        val rows = listOf(LeaderboardResultItem("Ana", 1, 28), LeaderboardResultItem("Ben", 2, 30))
        val lines =
            PlainTextComponentSerializer
                .plainText()
                .serialize(
                    renderLeaderboard(Board.course_record, rows),
                ).lines()

        assertEquals(
            listOf(
                "COURSE RECORD",
                "Oakmere Links · fewest strokes",
                "",
                "1. Ana  28 strokes",
                "2. Ben  30 strokes",
                "3. ---",
            ),
            lines.take(6),
        )
        assertEquals("10. ---", lines.last())
    }
}
