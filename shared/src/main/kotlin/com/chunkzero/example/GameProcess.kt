package com.chunkzero.example

import com.chunkzero.chunk.multistom.ChunkMinestom
import com.chunkzero.chunk.runtime.ChunkProcess
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
