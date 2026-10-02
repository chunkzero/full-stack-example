package dev.chunkzero.example.ui

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import java.net.URI
import java.util.UUID

object ResourcePack {
    private val hash =
        requireNotNull(javaClass.getResourceAsStream("/pack.sha1")).bufferedReader().use {
            it.readText().trim()
        }
    private val url = System.getenv("PACK_URL") ?: "http://127.0.0.1:8080/full-stack-example.zip"

    fun request(): ResourcePackRequest =
        ResourcePackRequest
            .resourcePackRequest()
            .packs(ResourcePackInfo.resourcePackInfo(UUID.nameUUIDFromBytes(hash.toByteArray()), URI.create(url), hash))
            .replace(true)
            .required(true)
            .build()
}
