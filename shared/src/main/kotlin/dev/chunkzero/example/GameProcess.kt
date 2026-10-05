package dev.chunkzero.example

import dev.chunkzero.runtime.ChunkMinestom
import dev.chunkzero.runtime.ChunkProcess
import net.minestom.server.ServerProcess

/** Runs an app's gameplay JVM, serving its sessions until Chunk shuts it down. */
fun runGameProcess() {
    ChunkProcess.connect().use { chunk ->
        ChunkMinestom.attach(chunk, ServerProcess.create()).use { minestom ->
            minestom.start()
            chunk.ready()
            chunk.awaitShutdown()
        }
    }
}
