package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.intellij.openapi.components.Service
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells every open review surface of a PR (Details tab and diff, the regular editor's review mode)
 * that its review threads or pending review changed. Each surface holds its own
 * [GiteaPRDiscussionsViewModels], so without this a thread resolved or replied to in the diff
 * still showed its old state in the editor until something else reloaded it.
 */
@Service(Service.Level.PROJECT)
class GiteaPRReviewChanges {

    /** [prNumber] changed; [source] is the view model that changed it (it has already reloaded). */
    data class Change(val prNumber: Int, val source: Any)

    private val _changes = MutableSharedFlow<Change>(extraBufferCapacity = 16)
    val changes: SharedFlow<Change> = _changes.asSharedFlow()

    fun notifyChanged(prNumber: Int, source: Any) {
        _changes.tryEmit(Change(prNumber, source))
    }
}
