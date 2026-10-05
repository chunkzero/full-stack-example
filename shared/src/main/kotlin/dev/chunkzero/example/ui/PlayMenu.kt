package dev.chunkzero.example.ui

import dev.chunkzero.example.ui.generated.PlayView
import dev.oglass.window.Click
import net.kyori.adventure.text.Component

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
    private val course: CourseSummary,
    initialRecord: CourseRecord?,
    initialPersonalBest: Int?,
    initialPlayerCount: Int,
    private val onTeeOff: () -> Unit,
    private val onShop: () -> Unit,
    private val onProfile: () -> Unit,
) : PlayView() {
    var record: CourseRecord? by state(initialRecord)
    var personalBest: Int? by state(initialPersonalBest)
    var playerCount: Int by state(initialPlayerCount)

    override fun course(): Component =
        text(fit(course.name.uppercase(), COURSE_WIDTH, UiFont.VANILLA, bold = true), UiColors.GOLD)

    override fun courseInfo(): Component = text("${course.holes} holes  ·  Par ${course.par}", UiColors.MUTED)

    override fun record(): Component =
        record?.let {
            val suffix = " - ${it.strokes}"
            text(
                fit(it.holder, STAT_WIDTH - UiFont.SMALL_CAPS.width(suffix), UiFont.SMALL_CAPS) + suffix,
                UiColors.GOLD,
            )
        }
            ?: text("No record yet", UiColors.MUTED)

    override fun best(): Component = personalBest?.let { text("$it strokes") } ?: text("Not played yet", UiColors.MUTED)

    override fun players(): Component = text(if (playerCount == 1) "1 player" else "$playerCount players")

    override fun onTeeOff(click: Click) = onTeeOff.invoke()

    override fun onShop(click: Click) = onShop.invoke()

    override fun onProfile(click: Click) = onProfile.invoke()

    private companion object {
        const val COURSE_WIDTH = 152
        const val STAT_WIDTH = 100
    }
}
