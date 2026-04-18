package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.util.ComputedResult
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Central ViewModel for the review discussion layer of a single PR.
 *
 * Responsibilities:
 * - Loads and groups review comments into synthetic [GiteaReviewThread]s
 * - Manages in-memory draft comments accumulated before review submission
 * - Provides resolve/unresolve operations (API call → automatic reload)
 *
 * Scoped to the PR panel lifetime (same scope as the diff VM).
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiscussionsViewModels(
    parentCs: CoroutineScope,
    private val prNumber: Int,
    private val repository: GiteaPRRepository,
) {
    companion object {
        val CONTEXT_KEY: Key<GiteaPRDiscussionsViewModels> = Key.create("gitea.pr.discussions.vm")
    }

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    // ── Threads ───────────────────────────────────────────────────────────

    private val _reloadTrigger = MutableStateFlow(0)

    private val _threads = MutableStateFlow<ComputedResult<List<GiteaPRThreadViewModel>>?>(null)
    val threads: StateFlow<ComputedResult<List<GiteaPRThreadViewModel>>?> = _threads.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            _reloadTrigger.collectLatest {
                _threads.value = ComputedResult.loading()
                try {
                    val threadList = repository.loadThreads(prNumber)
                    val threadVms = threadList.map { GiteaPRThreadViewModel(it, this@GiteaPRDiscussionsViewModels) }
                    _threads.value = ComputedResult.success(threadVms)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _threads.value = ComputedResult.failure(e)
                }
            }
        }
    }

    /** Re-fetches all review comments from the API and rebuilds the thread list. */
    fun reload() {
        _reloadTrigger.value++
    }

    // ── Draft comments ────────────────────────────────────────────────────

    private val _draftComments = MutableStateFlow<List<GiteaPRDraftComment>>(emptyList())
    val draftComments: StateFlow<List<GiteaPRDraftComment>> = _draftComments.asStateFlow()

    private val nextLocalId = AtomicLong(1)

    /**
     * Adds a new draft comment and returns its [GiteaPRDraftComment.localId].
     *
     * @param newPosition 1-indexed head (new-file) line; null for base-only comments.
     * @param oldPosition 1-indexed base (old-file) line; null for head-only comments.
     */
    fun addDraftComment(path: String, newPosition: Int?, oldPosition: Int?, body: String): Long {
        val id = nextLocalId.getAndIncrement()
        _draftComments.update { it + GiteaPRDraftComment(id, path, newPosition, oldPosition, body) }
        return id
    }

    /** Removes the draft comment with the given [localId]. No-op if not found. */
    fun removeDraftComment(localId: Long) {
        _draftComments.update { it.filter { c -> c.localId != localId } }
    }

    // ── Resolve / unresolve ───────────────────────────────────────────────

    /**
     * Resolves the anchor comment of the thread identified by [threadId]
     * (= anchor comment ID = thread's synthetic ID) and reloads the thread list.
     */
    suspend fun resolveThread(threadId: Long) {
        repository.resolveComment(threadId)
        reload()
    }

    /**
     * Unresolves the anchor comment of the thread identified by [threadId] and reloads.
     */
    suspend fun unresolveThread(threadId: Long) {
        repository.unresolveComment(threadId)
        reload()
    }

    // ── Lookup helpers ────────────────────────────────────────────────────

    /**
     * Returns all loaded threads for a specific file path, sorted by line number.
     * Returns empty list if threads are not yet loaded.
     */
    fun threadsForPath(path: String): List<GiteaPRThreadViewModel> {
        val loaded = _threads.value?.result?.getOrNull() ?: return emptyList()
        return loaded
            .filter { it.path == path }
            .sortedWith(compareBy(nullsLast()) { it.newLine ?: it.oldLine })
    }
}
