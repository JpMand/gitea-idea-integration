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

    /** The comment id to target when replying — Gitea's reply endpoint parents a reply to one
     * specific comment, so continuing the conversation should target the most recent comment in
     * the thread (not always the anchor) for the reply chain to thread correctly. */
    val lastCommentId: Long get() = thread.comments.lastOrNull()?.id ?: id

    val path: String? get() = thread.path

    /** 1-indexed head-file line; null for base-only threads. */
    val newLine: Int? get() = thread.newLine

    /** 1-indexed base-file line; null for head-only threads. */
    val oldLine: Int? get() = thread.oldLine

    val isResolved: Boolean get() = thread.isResolved

    /**
     * True if the line this thread is anchored to has changed since it was commented on (see
     * [com.github.jpmand.idea.plugin.gitea.api.models.isAnchorOutdated]). Outdated threads are
     * visually distinguished and cannot be replied to — same definition as the Timeline.
     */
    val isOutdated: Boolean get() = thread.isOutdated

    /** The commits its comments' reviews were made on — see [GiteaReviewThread.reviewCommitIds]. */
    val reviewCommitIds: Set<String> get() = thread.reviewCommitIds

    val commentVMs: List<GiteaPRCommentViewModel> = thread.comments.map(::GiteaPRCommentViewModel)

    /** Resolves this thread via the API. Triggers a full thread list reload. */
    suspend fun resolve() = discussionsVm.resolveThread(id)

    /** Unresolves this thread via the API. Triggers a full thread list reload. */
    suspend fun unresolve() = discussionsVm.unresolveThread(id)
}
