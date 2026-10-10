package com.chunkzero.example.ui

import com.chunkzero.example.ui.generated.PlayView
import com.chunkzero.window.Click
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

data class CourseSummary(
    val name: String,
    val par: Int,
    val holes: Int,
)

data class CourseRecord(
    val holder: String,
    val strokes: Int,
)

class PlayMenu(
    player: Player,
    private val course: CourseSummary,
    initialRecord: CourseRecord?,
    initialPersonalBest: Int?,
    initialPlayerCount: Int,
    private val onTeeOff: () -> Unit,
    private val onShop: () -> Unit,
    private val onProfile: () -> Unit,
) : PlayView(player) {
    var record: CourseRecord? by state(initialRecord)
    var personalBest: Int? by state(initialPersonalBest)
    var playerCount: Int by state(initialPlayerCount)

    override fun course(): Component = text(course.name.uppercase())

    override fun courseInfo(): Component = text("${course.holes} holes  ·  Par ${course.par}")

    override fun record(): Component {
        val record = record ?: return text("No record yet", UiColors.MUTED)
        if (record.holder == player.username) return text("You hold the record", UiColors.GOLD)
        return formatRound(record.strokes, course.par).append(text(" · ${record.holder}", UiColors.MUTED))
    }

    override fun best(): Component =
        personalBest?.let { formatRound(it, course.par) } ?: text("Not played yet", UiColors.MUTED)

    override fun players(): Component = text("$playerCount on the course")

    override fun onTeeOff(click: Click) = onTeeOff.invoke()

    override fun onShop(click: Click) = onShop.invoke()

    override fun onProfile(click: Click) = onProfile.invoke()
}
