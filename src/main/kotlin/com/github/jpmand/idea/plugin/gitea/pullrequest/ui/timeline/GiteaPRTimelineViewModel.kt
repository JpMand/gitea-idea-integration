package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.util.ComputedResult
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
    val pr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    val number: String = "#${pr.number}"
    val title: String = pr.title
    val descriptionMarkdown: String? = pr.body?.takeIf { it.isNotBlank() }
    val author = pr.author
    val createdAt: Date = pr.createdAt

    private val _items = MutableStateFlow<ComputedResult<List<GiteaTimelineItem>>?>(null)
    val items: StateFlow<ComputedResult<List<GiteaTimelineItem>>?> = _items.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = cs.launch(Dispatchers.IO) {
            _items.value = ComputedResult.loading()
            try {
                _items.value = ComputedResult.success(repository.loadTimeline(pr.number.toInt()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _items.value = ComputedResult.failure(e)
            }
        }
    }
}
