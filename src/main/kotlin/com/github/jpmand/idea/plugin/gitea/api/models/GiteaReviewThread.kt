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
    val comments: List<GiteaReviewComment>
)

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
