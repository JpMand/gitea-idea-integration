package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.*
import com.github.jpmand.idea.plugin.gitea.api.rest.getServerVersion
import com.fasterxml.jackson.core.JacksonException
import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.fileLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val LOG = fileLogger()

interface GiteaServersManager {
    val earliestSupportedVersion: GiteaVersion

    /**
     * Whether [version], as `/api/v1/version` reports it, is a Gitea release this plugin supports:
     * a plain `MAJOR.MINOR.PATCH` (dev builds and release candidates aren't supported) that is 1.27
     * or later. Gitea went from 1.27.x to 28.0.0, so no Gitea release has a major from 2 to 27.
     * Forgejo reports its own version with the Gitea API it is compatible with
     * (`16.0.5+gitea-1.22.0`), which this rejects as well.
     */
    fun isSupported(version: GiteaVersion): Boolean

    /**
     * Whether [server] runs a supported Gitea ([isSupported]), asked without credentials through
     * `GET /api/v1/version` — public on every Gitea, and not served by GitHub or GitLab. Never throws
     * except for cancellation; a server that can't be reached reads as false.
     */
    suspend fun isSupportedGiteaServer(server: GiteaServerPath): Boolean

    suspend fun getMetadata(api: GiteaApi): GiteaServerMetadata
}

internal class CachingGiteaServersManager(private val serviceCs: CoroutineScope) : GiteaServersManager {

    /** Per server: whether it's a supported Gitea, or null when it couldn't be reached. */
    private val testCache = ConcurrentHashMap<GiteaServerPath, Deferred<Boolean?>>()

    private val metadataCache = ConcurrentHashMap<GiteaServerPath, GiteaServerMetadata>()
    private val metadataCacheGuard = Mutex()

    // 1.27 is the first release that exposes the PR review-comment reply endpoint
    // (`POST /repos/{owner}/{repo}/pulls/{index}/comments/{id}/replies`); 1.26 answers it with a
    // 405, which left replying to review threads broken everywhere in the UI. Resolve/unresolve
    // (1.26) and every other endpoint the plugin calls are also present in 1.27. Tested
    // reference: 1.27.3.
    override val earliestSupportedVersion: GiteaVersion = GiteaVersion(1, 27, 0)

    override fun isSupported(version: GiteaVersion): Boolean =
        version.isRelease && version.major !in 2..27 && version >= earliestSupportedVersion

    override suspend fun isSupportedGiteaServer(server: GiteaServerPath): Boolean {
        val test = testCache.getOrPut(server) {
            serviceCs.async(Dispatchers.IO + CoroutineName("Gitea Server Tester")) { testServer(server) }
        }
        val result = test.await()
        // A network failure says nothing about the server: ask again next time.
        if (result == null) testCache.remove(server, test)
        return result == true
    }

    private suspend fun testServer(server: GiteaServerPath): Boolean? =
        try {
            val reported = serviceAsync<GiteaApiManager>().getUnauthenticatedClient(server).getServerVersion().version
            val supported = reported != null && isSupported(GiteaVersion.fromString(reported))
            LOG.info("$server reports version '$reported': ${if (supported) "a supported Gitea" else "not a supported Gitea"}")
            supported
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpStatusErrorException) {
            LOG.info("$server is not a Gitea server (HTTP ${e.statusCode} for /api/v1/version)")
            false
        } catch (e: IOException) {
            // Jackson's parse errors are IOExceptions too, but they mean the server answered.
            if (e is JacksonException) {
                LOG.info("$server is not a Gitea server (its /api/v1/version isn't Gitea's)")
                false
            } else {
                LOG.debug("$server couldn't be reached to tell whether it's a Gitea server", e)
                null
            }
        } catch (e: Exception) {
            LOG.info("$server is not a Gitea server (${e.javaClass.simpleName} for /api/v1/version)")
            false
        }

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
    LOG.info("${api.server} reports version '${dto.version}', read as $version")
    return GiteaServerMetadata(version)
}
