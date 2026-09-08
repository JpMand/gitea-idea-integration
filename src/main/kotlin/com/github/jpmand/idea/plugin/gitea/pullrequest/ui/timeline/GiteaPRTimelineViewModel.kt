package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
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

/** Read-only view model for a PR's activity timeline (Conversation). */
class GiteaPRTimelineViewModel(
    parentCs: CoroutineScope,
    project: Project,
    val pr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    val number: String = "#${pr.number}"
    val title: String = pr.title
    val descriptionMarkdown: String? = pr.body?.takeIf { it.isNotBlank() }
    val author = pr.author
    val createdAt: Date = pr.createdAt

    /** Milestone-2 stub — the editor renders, submit pops "not implemented yet". */
    val newCommentVm = GiteaPRSubmittableTextViewModel(project, cs, GiteaBundle.message("pull.request.action.comment"))

    private val _items = MutableStateFlow<ComputedResult<List<GiteaPRTimelineItemViewModel>>?>(null)
    val items: StateFlow<ComputedResult<List<GiteaPRTimelineItemViewModel>>?> = _items.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
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
