package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.ui.codereview.editor.CodeReviewInEditorViewModel
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.collaboration.util.ComputedResult
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
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
import kotlinx.coroutines.launch

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
    private val project: Project,
    parentCs: CoroutineScope,
    private val prNumber: Int,
    /** The PR's current head SHA — a thread's anchor comment carrying a different `commitId`
     * means the diff it was anchored to is no longer the latest one, i.e. it's "outdated". */
    val headSha: String,
    private val repository: GiteaPRRepository,
) : CodeReviewInEditorViewModel {

    private val settings: GiteaPullRequestsSettings get() = project.service()

    /** Whether the diff editor should highlight lines that carry review comments. */
    val highlightDiffLines: StateFlow<Boolean> = settings.highlightDiffLinesInEditorState

    /** The signed-in account's login — gates inline-comment edit/delete/reply controls to a
     * comment's own author, same as the Timeline's `currentUserLogin`. */
    val currentUserLogin: String get() = repository.accountLogin

    companion object {
        val CONTEXT_KEY: Key<GiteaPRDiscussionsViewModels> = Key.create("gitea.pr.discussions.vm")
    }

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    /** Avatar icons for comment/reply authors in the diff-editor review UI. */
    val avatars: IconsProvider<GiteaUser> =
        CachingIconsProvider(AsyncImageIconsProvider(cs, GiteaImageLoader(repository.api)))

    /** The signed-in account's own profile, loaded once — used for the reply composer's avatar. */
    private val _currentUser = MutableStateFlow<GiteaUser?>(null)
    val currentUser: StateFlow<GiteaUser?> = _currentUser.asStateFlow()

    /** Repo collaborators, loaded once for `@`-mention completion in reply composers — see
     * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.mention.GiteaMentionCompletionContributor]. */
    private val _mentionCandidates = MutableStateFlow<List<GiteaUser>>(emptyList())
    val mentionCandidates: StateFlow<List<GiteaUser>> = _mentionCandidates.asStateFlow()

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
        cs.launch(Dispatchers.IO) {
            try {
                _currentUser.value = repository.currentUser()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means the reply composer stays hidden.
            }
        }
        cs.launch(Dispatchers.IO) {
            try {
                _mentionCandidates.value = repository.loadPossibleAuthors()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means no mention completion, not an error banner.
            }
        }
    }

    /** Re-fetches all review comments from the API and rebuilds the thread list. */
    fun reload() {
        _reloadTrigger.value++
    }

    // ── CodeReviewInEditorViewModel ───────────────────────────────────────

    private val _discussionsViewOption = MutableStateFlow(settings.diffReviewViewOption)
    override val discussionsViewOption: StateFlow<DiscussionsViewOption> = _discussionsViewOption.asStateFlow()
    override fun setDiscussionsViewOption(viewOption: DiscussionsViewOption) {
        _discussionsViewOption.value = viewOption
        settings.diffReviewViewOption = viewOption
    }

    /** Always false — Gitea plugin does not track local-branch sync state. */
    override val updateRequired: StateFlow<Boolean> = MutableStateFlow(false)

    /** No-op — `updateRequired` is always false so this button is never enabled. */
    override fun updateBranch() = Unit

    // Draft-comment accumulation for new (not-yet-submitted) review comments is a separate,
    // larger piece of this milestone — not present here yet.

    // ── Resolve / unresolve / reply ─────────────────────────────────────────

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

    /** Replies to the thread identified by [threadId] (its anchor comment's id) and reloads. */
    suspend fun replyToThread(threadId: Long, body: String) {
        repository.replyToComment(prNumber, threadId, body)
        reload()
    }

    /** Edits an inline review comment's body (own comments only — gated by [currentUserLogin] at
     * the call site, same as the Timeline) and reloads. */
    suspend fun editComment(commentId: Long, body: String) {
        repository.editComment(commentId, body)
        reload()
    }

    /** Deletes an inline review comment (own comments only) and reloads. */
    suspend fun deleteComment(commentId: Long) {
        repository.deleteComment(commentId)
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
