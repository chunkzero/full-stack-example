package com.chunkzero.example.golf

import com.chunkzero.chunk.runtime.Component
import com.chunkzero.example.world.GameWorld

/** The Oakmere course, parsed once per JVM; each golf session loads its own copy. */
@Component(Component.Scope.PROCESS)
fun oakmere(): GameWorld = GameWorld.read("oakmere")

@Component(Component.Scope.PROCESS)
fun oakmereCourse(world: GameWorld): Course = Course.from(world.marks)
