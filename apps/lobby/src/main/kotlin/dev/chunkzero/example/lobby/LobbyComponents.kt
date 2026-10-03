package dev.chunkzero.example.lobby

import dev.chunkzero.example.golf.Course
import dev.chunkzero.example.world.GameWorld
import dev.chunkzero.runtime.Component

/** The clubhouse world, parsed once per JVM; each lobby session loads its own copy. */
@Component(Component.Scope.PROCESS)
fun clubhouse(): GameWorld = GameWorld.read("clubhouse")

@Component(Component.Scope.PROCESS)
fun clubhouseCourse(world: GameWorld): Course = Course.from(world.marks)
