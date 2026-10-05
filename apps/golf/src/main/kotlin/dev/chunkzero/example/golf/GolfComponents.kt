package dev.chunkzero.example.golf

import dev.chunkzero.example.world.GameWorld
import dev.chunkzero.runtime.Component

/** The Oakmere course, parsed once per JVM; each golf session loads its own copy. */
@Component(Component.Scope.PROCESS)
fun oakmere(): GameWorld = GameWorld.read("oakmere")

@Component(Component.Scope.PROCESS)
fun oakmereCourse(world: GameWorld): Course = Course.from(world.marks)
