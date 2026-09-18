package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.models.mentionCandidates
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.intellij.collaboration.util.ComputedResult
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Read-only view model for a PR's activity timeline (Conversation). Reviews render here but
 * aren't authored here — review submission lives in the future Diff/Review-mode live editor.
 */
class GiteaPRTimelineViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    val pr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    val number: String = "#${pr.number}"
    val title: String = pr.title
    val descriptionMarkdown: String? = pr.body?.takeIf { it.isNotBlank() }
    val author = pr.author
    val createdAt: Date = pr.createdAt

    /**
     * Posts a new top-level timeline comment and appends it to the already-loaded list, instead
     * of reloading the whole timeline — a full [reload] flips [items] through a loading state,
     * making every existing item disappear and reappear for a moment.
     */
    val newCommentVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
        val comment = repository.createComment(pr.number.toInt(), body)
        val currentList = _items.value?.result?.getOrNull()
        if (currentList != null) {
            _items.value = ComputedResult.success(currentList + comment)
        } else {
            reload()
        }
    }

    private val _items = MutableStateFlow<ComputedResult<List<GiteaPRTimelineItemViewModel>>?>(null)
    val items: StateFlow<ComputedResult<List<GiteaPRTimelineItemViewModel>>?> = _items.asStateFlow()

    /** Repo collaborators, loaded once for `@`-mention completion in comment editors — see
     * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.mention.GiteaMentionCompletionContributor]. */
    private val _mentionCandidates = MutableStateFlow<List<GiteaUser>>(emptyList())
    val mentionCandidates: StateFlow<List<GiteaUser>> = _mentionCandidates.asStateFlow()

    /** The signed-in account's own profile, loaded once — used for the "leave a comment" field's
     * avatar so it reflects whoever is actually signed in, not [author] (the PR's opener). */
    private val _currentUser = MutableStateFlow<GiteaUser?>(null)
    val currentUser: StateFlow<GiteaUser?> = _currentUser.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
        cs.launch(Dispatchers.IO) {
            val collaborators = try {
                repository.loadPossibleAuthors()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup still leaves pr.mentionCandidates() usable.
                emptyList()
            }
            _mentionCandidates.value = (collaborators + pr.mentionCandidates()).distinctBy { it.login }
        }
        cs.launch(Dispatchers.IO) {
            try {
                _currentUser.value = repository.currentUser()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means the comment field stays hidden until retried.
            }
        }
    }

    /**
     * Re-fetches the whole timeline from the server. Only shows the "loading" state when there's
     * nothing on screen yet ([_items] is still `null`) — a reload with existing content keeps
     * that content visible until the new list is ready, then swaps in one step, instead of
     * flashing back to "loading conversation" for every refresh.
     */
    fun reload() {
        loadJob?.cancel()
        loadJob = cs.launch(Dispatchers.IO) {
            if (_items.value == null) _items.value = ComputedResult.loading()
            try {
                val items = repository.loadTimeline(pr.number.toInt()).toItemViewModels()
                _items.value = ComputedResult.success(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _items.value = ComputedResult.failure(e)
            }
        }
    }

    // ── In-place updates ──────────────────────────────────────────────────
    // Reply/resolve/unresolve/edit/delete all patch the already-loaded item list directly instead
    // of calling reload() — a full re-fetch not only wastes a network round-trip for a change this
    // client already knows the result of, it would also flip every other already-rendered item
    // through reload()'s loading state along with it.

    /** Appends a reply to the review thread it belongs to. */
    fun appendReply(threadId: Long, comment: GiteaReviewComment) = updateItems { items ->
        items.map { item ->
            if (item !is GiteaPRTimelineItemViewModel.Review) return@map item
            item.copy(threads = item.threads.map { thread ->
                if (thread.id == threadId) thread.copy(comments = thread.comments + comment) else thread
            })
        }
    }

    /** Toggles a review thread's resolved state. */
    fun updateThreadResolved(threadId: Long, resolved: Boolean) = updateItems { items ->
        items.map { item ->
            if (item !is GiteaPRTimelineItemViewModel.Review) return@map item
            item.copy(threads = item.threads.map { thread ->
                if (thread.id == threadId) thread.copy(isResolved = resolved) else thread
            })
        }
    }

    /** Updates a comment's body wherever it lives — a top-level comment, or one inside a review
     * thread. [editComment] returns no updated object, so [updatedAt] is the client's own
     * "now", same as what the server would set anyway. */
    fun updateCommentBody(commentId: Long, body: String, updatedAt: Date) = updateItems { items ->
        items.map { item ->
            when (item) {
                is GiteaPRTimelineItemViewModel.Comment ->
                    if (item.id == commentId) item.copy(body = body, updatedAt = updatedAt) else item
                is GiteaPRTimelineItemViewModel.Review -> item.copy(threads = item.threads.map { thread ->
                    thread.copy(comments = thread.comments.map { c ->
                        if (c.id == commentId) c.copy(body = body, updatedAt = updatedAt) else c
                    })
                })
                else -> item
            }
        }
    }

    /** Removes a comment wherever it lives — a top-level comment (the whole item disappears), or
     * one inside a review thread (just that comment does). */
    fun removeComment(commentId: Long) = updateItems { items ->
        items.mapNotNull { item ->
            when (item) {
                is GiteaPRTimelineItemViewModel.Comment -> if (item.id == commentId) null else item
                is GiteaPRTimelineItemViewModel.Review -> item.copy(threads = item.threads.map { thread ->
                    thread.copy(comments = thread.comments.filterNot { it.id == commentId })
                })
                else -> item
            }
        }
    }

    private fun updateItems(transform: (List<GiteaPRTimelineItemViewModel>) -> List<GiteaPRTimelineItemViewModel>) {
        val current = _items.value?.result?.getOrNull() ?: return
        _items.value = ComputedResult.success(transform(current))
    }
}
