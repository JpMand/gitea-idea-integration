package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.*
import com.github.jpmand.idea.plugin.gitea.api.rest.getServerVersion
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettings
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
     * Whether [version], as `/api/v1/version` reports it, is a Gitea this plugin supports: 1.27 or
     * later, and a plain `MAJOR.MINOR.PATCH` release unless [acceptPreReleases] (dev builds such as
     * `1.27.0+dev-…`, release candidates such as `1.27.0-rc0`). Gitea went from 1.27.x to 28.0.0, so
     * no Gitea has a major from 2 to 27. Forgejo reports its own version with the Gitea API it is
     * compatible with (`16.0.5+gitea-1.22.0`), which is rejected either way.
     */
    fun isSupported(version: GiteaVersion, acceptPreReleases: Boolean): Boolean

    /** [isSupported] with the application setting [GiteaSettings.acceptPreReleaseVersions]. */
    fun isSupported(version: GiteaVersion): Boolean =
        isSupported(version, GiteaSettings.getInstance().acceptPreReleaseVersions)

    /**
     * The version [server] reports, asked without credentials through `GET /api/v1/version` — public
     * on every Gitea, and not served by GitHub or GitLab — or null when it doesn't answer like Gitea
     * or can't be reached. Cached per server, except for a server that couldn't be reached. Never
     * throws except for cancellation.
     */
    suspend fun getReportedVersion(server: GiteaServerPath): GiteaVersion?

    /** Whether [server] runs a supported Gitea: [getReportedVersion], then [isSupported]. */
    suspend fun isSupportedGiteaServer(server: GiteaServerPath): Boolean =
        getReportedVersion(server)?.let { isSupported(it) } == true

    suspend fun getMetadata(api: GiteaApi): GiteaServerMetadata
}

internal class CachingGiteaServersManager(private val serviceCs: CoroutineScope) : GiteaServersManager {

    /** Per server: the version it reports, or why there is none. The version, not whether it's
     * supported, so that a change of [GiteaSettings.acceptPreReleaseVersions] needs no new request. */
    private val testCache = ConcurrentHashMap<GiteaServerPath, Deferred<ServerTest>>()

    private sealed interface ServerTest {
        /** The server answered: with its version, or null when it isn't a Gitea server. */
        data class Answered(val version: GiteaVersion?) : ServerTest

        /** A network failure, which says nothing about the server. */
        data object Unreachable : ServerTest
    }

    private val metadataCache = ConcurrentHashMap<GiteaServerPath, GiteaServerMetadata>()
    private val metadataCacheGuard = Mutex()

    // 1.27 is the first release that exposes the PR review-comment reply endpoint
    // (`POST /repos/{owner}/{repo}/pulls/{index}/comments/{id}/replies`); 1.26 answers it with a
    // 405, which left replying to review threads broken everywhere in the UI. Resolve/unresolve
    // (1.26) and every other endpoint the plugin calls are also present in 1.27. Tested
    // reference: 1.27.3.
    override val earliestSupportedVersion: GiteaVersion = GiteaVersion(1, 27, 0)

    override fun isSupported(version: GiteaVersion, acceptPreReleases: Boolean): Boolean =
        (version.isRelease || acceptPreReleases && !version.isForgejo) &&
            version.major !in 2..27 && version >= earliestSupportedVersion

    override suspend fun getReportedVersion(server: GiteaServerPath): GiteaVersion? {
        val test = testCache.getOrPut(server) {
            serviceCs.async(Dispatchers.IO + CoroutineName("Gitea Server Tester")) { testServer(server) }
        }
        return when (val result = test.await()) {
            // Ask again next time.
            ServerTest.Unreachable -> null.also { testCache.remove(server, test) }
            is ServerTest.Answered -> result.version
        }
    }

    private suspend fun testServer(server: GiteaServerPath): ServerTest =
        try {
            val reported = serviceAsync<GiteaApiManager>().getUnauthenticatedClient(server).getServerVersion().version
            LOG.info("$server reports version '$reported'")
            ServerTest.Answered(reported?.let { GiteaVersion.fromString(it) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpStatusErrorException) {
            LOG.info("$server is not a Gitea server (HTTP ${e.statusCode} for /api/v1/version)")
            ServerTest.Answered(null)
        } catch (e: IOException) {
            // Jackson's parse errors are IOExceptions too, but they mean the server answered.
            if (e is JacksonException) {
                LOG.info("$server is not a Gitea server (its /api/v1/version isn't Gitea's)")
                ServerTest.Answered(null)
            } else {
                LOG.debug("$server couldn't be reached to tell whether it's a Gitea server", e)
                ServerTest.Unreachable
            }
        } catch (e: Exception) {
            LOG.info("$server is not a Gitea server (${e.javaClass.simpleName} for /api/v1/version)")
            ServerTest.Answered(null)
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
