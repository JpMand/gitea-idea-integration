package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * A synthetic grouping of review comments at the same diff location.
 *
 * Gitea has no server-side thread IDs; threads are constructed client-side by grouping
 * [GiteaReviewComment]s that share the same [path] and line ([newLine]/[oldLine]).
 * [id] equals the anchor comment's (first by id) ID and is not a server entity.
 */
data class GiteaReviewThread(
    /** Synthetic ID — equals the anchor [GiteaReviewComment.id] (first by id in the group). */
    val id: Long,
    val path: String?,
    /** 1-indexed line in the head file; null for base-only comments. */
    val newLine: Int?,
    /** 1-indexed line in the base file; null for head-only comments. */
    val oldLine: Int?,
    /** Derived from the anchor comment's resolved state. */
    val isResolved: Boolean,
    val comments: List<GiteaReviewComment>,
    /**
     * True when the line the thread is anchored to has changed since it was commented on — see
     * [isAnchorOutdated]. Computed by the repository (it needs the head file content), so a
     * freshly grouped thread starts out current.
     */
    val isOutdated: Boolean = false,
    /**
     * The commit the thread's review was made on (its `commit_id`) — see [withReviewCommits]. Not
     * a comment's own `commit_id`: Gitea sets that to the last commit that touched the line.
     */
    val reviewCommitId: String? = null,
    /** The commits of the reviews of all its comments — [toThreads] groups the comments at one
     * line, which can come from reviews on different commits. */
    val reviewCommitIds: Set<String> = emptySet(),
)

/**
 * The text of the line a review comment is anchored to, without its diff prefix: Gitea's
 * `diff_hunk` ends at the commented line. Null when there is no hunk, or when the anchor is a
 * removed (base-side) line, which has no head-side counterpart to compare with.
 */
fun GiteaReviewComment.anchoredLineText(): String? {
    val last = diffHunk?.lines()?.lastOrNull { it.isNotEmpty() } ?: return null
    if (last.startsWith("@@") || last.startsWith("-") || last.startsWith("\\")) return null
    return last.drop(1)
}

/**
 * Whether [anchor]'s head-side line no longer holds the text it was commented on, given the PR
 * head's current file content as [headLines].
 *
 * Gitea's API has no per-comment "outdated" flag, and a comment's `commit_id` is the last commit
 * that touched the line (not the head it was made on), so comparing it with the PR head marks
 * almost every thread outdated. Comparing the anchored line's text is what actually changes when
 * a later push rewrites the line. A line number past the end of the file (or a deleted file) also
 * counts as outdated.
 */
fun isAnchorOutdated(anchor: GiteaReviewComment, headLines: List<String>): Boolean {
    val line = anchor.newLine?.takeIf { it > 0 } ?: return false
    val expected = anchor.anchoredLineText() ?: return false
    val actual = headLines.getOrNull(line - 1) ?: return true
    return actual.trimEnd() != expected.trimEnd()
}

/**
 * Groups a flat list of [GiteaReviewComment] into synthetic [GiteaReviewThread]s.
 *
 * Grouping key: `(path, newLine, oldLine)`. This is a heuristic — Gitea has no server-side
 * thread IDs and its `PullReviewComment` carries no reply linkage, so independent conversations
 * at the same location are unavoidably merged into one thread. Resolution state uses the anchor
 * comment (lowest id in the group).
 *
 * All comments are kept, including those with a null path or null lines (review comments not
 * anchored to a diff line): the diff viewer filters those out by path, but the activity timeline
 * still shows them under their review.
 */
fun List<GiteaReviewComment>.toThreads(): List<GiteaReviewThread> =
    groupBy { Triple(it.path, it.newLine, it.oldLine) }
        .map { (_, comments) ->
            val sorted = comments.sortedBy { it.id }
            val anchor = sorted.first()
            GiteaReviewThread(
                id = anchor.id,
                path = anchor.path,
                newLine = anchor.newLine,
                oldLine = anchor.oldLine,
                isResolved = anchor.isResolved,
                comments = sorted,
            )
        }
        .sortedBy { it.id }

/**
 * Sets [GiteaReviewThread.reviewCommitId] on each thread from the review its anchor comment
 * belongs to, among [reviews], and [GiteaReviewThread.reviewCommitIds] from the reviews of all its
 * comments. A review without a commit (e.g. a review request) adds none.
 */
fun List<GiteaReviewThread>.withReviewCommits(reviews: List<GiteaReview>): List<GiteaReviewThread> {
    val commitByReviewId = reviews.mapNotNull { r -> r.commitId?.takeIf { it.isNotBlank() }?.let { r.id to it } }.toMap()
    return map { thread ->
        thread.copy(
            reviewCommitId = thread.comments.firstOrNull()?.reviewId?.let(commitByReviewId::get),
            reviewCommitIds = thread.comments.mapNotNullTo(LinkedHashSet()) { c -> c.reviewId?.let(commitByReviewId::get) },
        )
    }
}
