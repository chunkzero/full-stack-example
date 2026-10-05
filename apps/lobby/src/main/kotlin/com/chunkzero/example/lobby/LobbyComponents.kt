package com.chunkzero.example.lobby

import com.chunkzero.chunk.runtime.Component
import com.chunkzero.example.golf.Course
import com.chunkzero.example.ui.generated.WindowPack
import com.chunkzero.example.world.GameWorld
import com.chunkzero.window.Windows

/** The clubhouse world, parsed once per JVM; each lobby session loads its own copy. */
@Component(Component.Scope.PROCESS)
fun clubhouse(): GameWorld = GameWorld.read("clubhouse")

@Component(Component.Scope.PROCESS)
fun clubhouseCourse(world: GameWorld): Course = Course.from(world.marks)

@Component(Component.Scope.PROCESS)
fun windows(): Windows = WindowPack.windows()
