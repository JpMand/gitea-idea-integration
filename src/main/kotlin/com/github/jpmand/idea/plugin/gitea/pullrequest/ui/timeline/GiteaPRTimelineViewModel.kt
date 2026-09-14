package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
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
            try {
                _mentionCandidates.value = repository.loadPossibleAuthors()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means no mention completion, not an error banner.
            }
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

    fun reload() {
        loadJob?.cancel()
        loadJob = cs.launch(Dispatchers.IO) {
            _items.value = ComputedResult.loading()
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
}
