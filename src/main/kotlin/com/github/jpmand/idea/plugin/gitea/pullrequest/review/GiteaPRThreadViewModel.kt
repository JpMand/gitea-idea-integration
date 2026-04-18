package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread

/**
 * ViewModel for a single review thread (a group of [GiteaPRCommentViewModel]s at one diff location).
 *
 * Resolution state and outdated detection are anchored to the first comment by ID — the same
 * comment that is targeted by resolve/unresolve API calls via [GiteaPRDiscussionsViewModels].
 */
class GiteaPRThreadViewModel(
    val thread: GiteaReviewThread,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
) {
    /** Synthetic thread ID — equals the anchor comment's server ID. */
    val id: Long get() = thread.id

    val path: String? get() = thread.path

    /** 1-indexed head-file line; null for base-only threads. */
    val newLine: Int? get() = thread.newLine

    /** 1-indexed base-file line; null for head-only threads. */
    val oldLine: Int? get() = thread.oldLine

    val isResolved: Boolean get() = thread.isResolved

    /**
     * True if the anchor comment was made on an older commit than the current head.
     * Outdated threads are visually distinguished and cannot be replied to.
     */
    val isOutdated: Boolean
        get() {
            val anchor = thread.comments.firstOrNull() ?: return false
            return anchor.originalCommitId != null && anchor.originalCommitId != anchor.commitId
        }

    val commentVMs: List<GiteaPRCommentViewModel> = thread.comments.map(::GiteaPRCommentViewModel)

    /** Resolves this thread via the API. Triggers a full thread list reload. */
    suspend fun resolve() = discussionsVm.resolveThread(id)

    /** Unresolves this thread via the API. Triggers a full thread list reload. */
    suspend fun unresolve() = discussionsVm.unresolveThread(id)
}
