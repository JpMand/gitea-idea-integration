package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.renderMarkdownAsHtml
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Renders Gitea-flavoured markdown to sanitized HTML via the server's `/markdown` endpoint
 * (the same renderer Gitea's own web UI uses for issue/PR/comment bodies), with a small
 * app-wide cache so reopening a PR / re-rendering the same body doesn't re-hit the server.
 *
 * Never throws: callers get `null` on any failure and are expected to keep showing their
 * escaped-plain-text fallback.
 */
@Service(Service.Level.APP)
internal class GiteaMarkdownService {

    private data class CacheKey(val server: String, val context: String, val markdown: String)

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<CacheKey, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, String>?): Boolean = size > 500
        }
    )

    @Suppress("UnstableApiUsage")
    suspend fun render(api: GiteaApi, repoContext: String, markdown: String): String? {
        if (markdown.isBlank()) return null
        val key = CacheKey(api.server.toString(), repoContext, markdown)
        cache[key]?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                api.renderMarkdownAsHtml(context = repoContext, mode = "comment", text = markdown)
            }.also { cache[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            thisLogger().warn("Failed to render markdown (context=$repoContext)", e)
            null
        }
    }
}
