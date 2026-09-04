package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaServerMetadata
import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.api.GiteaVersion
import com.github.jpmand.idea.plugin.gitea.api.rest.checkIsGiteaServer
import com.github.jpmand.idea.plugin.gitea.api.rest.getServerVersion
import com.intellij.openapi.components.serviceAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    override val earliestSupportedVersion: GiteaVersion = GiteaVersion(1, 27, 1)

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
    val dto =
        try {
            api.getServerVersion()
        }catch (e: Throwable) {
            throw e
        }
    val version = GiteaVersion.fromString(dto.version ?: "0")
    val metadata = GiteaServerMetadata(version)
    return metadata
}
