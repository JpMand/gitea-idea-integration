package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.*
import com.github.jpmand.idea.plugin.gitea.api.rest.checkIsGiteaServer
import com.github.jpmand.idea.plugin.gitea.api.rest.getServerVersion
import com.intellij.openapi.components.serviceAsync
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

interface GiteaServersManager {
    val earliestSupportedVersion: GiteaVersion

    suspend fun checkIsGiteaServer(server: GiteaServerPath): Boolean

    suspend fun getMetadata(api: GiteaApi): GiteaServerMetadata
}

internal class CachingGiteaServersManager(private val serviceCs: CoroutineScope) : GiteaServersManager {

    private val testCache = ConcurrentHashMap<GiteaServerPath, Deferred<Boolean>>()

    private val metadataCache = ConcurrentHashMap<GiteaServerPath, GiteaServerMetadata>()
    private val metadataCacheGuard = Mutex()

    // 1.27 is the first release that exposes the PR review-comment reply endpoint
    // (`POST /repos/{owner}/{repo}/pulls/{index}/comments/{id}/replies`); 1.26 answers it with a
    // 405, which left replying to review threads broken everywhere in the UI. Resolve/unresolve
    // (1.26) and every other endpoint the plugin calls are also present in 1.27. Tested
    // reference: 1.27.3.
    override val earliestSupportedVersion: GiteaVersion = GiteaVersion(1, 27, 0)

    override suspend fun checkIsGiteaServer(server: GiteaServerPath): Boolean =
        testCache.getOrPut(server) {
            serviceCs.async(Dispatchers.IO + CoroutineName("Gitea Server Tester")) {
                serviceAsync<GiteaApiManager>().getUnauthenticatedClient(server).rest.checkIsGiteaServer()
            }
        }.await()

    override suspend fun getMetadata(api: GiteaApi): GiteaServerMetadata =
        withContext(Dispatchers.IO + CoroutineName("Gitea Server Tester")) {
            metadataCacheGuard.withLock {
                val existing = metadataCache[api.server]
                if (existing != null) {
                    return@withLock existing
                }
                val metadata = getServerMetadata(api)
                metadataCache[api.server] = metadata
                metadata
            }
        }
}
private suspend fun getServerMetadata(api: GiteaApi): GiteaServerMetadata {
    val dto = api.getServerVersion()
    // fromString never throws for a non-blank string; an unrecognisable version parses as 0.0.0
    // and fails the floor check rather than crashing the login flow.
    val version = GiteaVersion.fromString(dto.version ?: "0")
    return GiteaServerMetadata(version)
}
