package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.rest.models.commit.GiteaCommitDTO
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UnstableApiUsage")
class GiteaPRChangesViewModel(
    private val cs: CoroutineScope,
    val prNumber: Int,
    private val repository: GiteaPRRepository,
) : CodeReviewChangesViewModel<GiteaCommitDTO> {

    private val _commits = MutableStateFlow<List<GiteaCommitDTO>>(emptyList())
    private val _selectedCommitIndex = MutableStateFlow(-1)

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    override val reviewCommits: SharedFlow<List<GiteaCommitDTO>> =
        _commits.shareIn(cs, SharingStarted.Eagerly, replay = 1)

    override val selectedCommitIndex: SharedFlow<Int> =
        _selectedCommitIndex.shareIn(cs, SharingStarted.Eagerly, replay = 1)

    override val selectedCommit: SharedFlow<GiteaCommitDTO?> =
        combine(_commits, _selectedCommitIndex) { commits, idx ->
            if (idx == -1) null else commits.getOrNull(idx)
        }.shareIn(cs, SharingStarted.Eagerly, replay = 1)

    init {
        cs.launch(Dispatchers.IO) {
            try {
                val loaded = repository.loadCommits(prNumber)
                withContext(Dispatchers.Main) { _commits.value = loaded }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e
            }
        }
    }

    override fun selectCommit(index: Int) {
        val size = _commits.value.size
        _selectedCommitIndex.value = index.coerceIn(-1, maxOf(-1, size - 1))
    }

    override fun selectNextCommit() {
        val size = _commits.value.size
        val cur = _selectedCommitIndex.value
        if (cur < size - 1) selectCommit(cur + 1)
    }

    override fun selectPreviousCommit() {
        val cur = _selectedCommitIndex.value
        if (cur > -1) selectCommit(cur - 1)
    }

    /** Returns the full SHA — displayed text should be shortened in [CommitPresentation]. */
    override fun commitHash(commit: GiteaCommitDTO): String = commit.sha
}
