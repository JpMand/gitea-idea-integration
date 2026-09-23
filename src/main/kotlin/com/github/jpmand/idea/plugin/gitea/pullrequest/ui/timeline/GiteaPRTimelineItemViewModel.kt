package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.*
import java.util.*

/**
 * One rendered row of the PR activity timeline. Mirrors the bundled GitLab plugin's
 * `GitLabMergeRequestTimelineItem` / GitHub's `GHPRTimelineItem`: a per-kind view model the
 * component factory dispatches on. Built from the domain [GiteaTimelineItem]s by [toItemViewModels],
 * which additionally folds runs of consecutive commits into a single [Commits] block.
 */
sealed interface GiteaPRTimelineItemViewModel {
    val actor: GiteaUser?
    val timestamp: Date

    data class Comment(
        val id: Long,
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val body: String?,
        val htmlUrl: String?,
        /** Null, or equal to [timestamp], when never edited. */
        val updatedAt: Date? = null,
    ) : GiteaPRTimelineItemViewModel {
        val edited: Boolean get() = updatedAt != null && updatedAt != timestamp
    }

    data class Commits(
        val commits: List<GiteaTimelineItem.Commit>,
    ) : GiteaPRTimelineItemViewModel {
        override val actor: GiteaUser? get() = commits.firstOrNull()?.actor
        override val timestamp: Date get() = commits.last().timestamp
        val rawActor : String? get() = commits.firstOrNull()?.rawAuthor
    }

    data class Review(
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val state: GiteaReviewState,
        val body: String?,
        val htmlUrl: String?,
        val threads: List<GiteaReviewThread>,
    ) : GiteaPRTimelineItemViewModel

    data class Event(
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val kind: GiteaTimelineItem.Event.Kind,
        val label: GiteaLabel?,
        val user: GiteaUser?,
        val oldValue: String?,
        val newValue: String?,
    ) : GiteaPRTimelineItemViewModel
}

fun List<GiteaTimelineItem>.toItemViewModels(): List<GiteaPRTimelineItemViewModel> {
    val result = mutableListOf<GiteaPRTimelineItemViewModel>()
    val pendingCommits = mutableListOf<GiteaTimelineItem.Commit>()

    fun flushCommits() {
        if (pendingCommits.isNotEmpty()) {
            result += GiteaPRTimelineItemViewModel.Commits(pendingCommits.toList())
            pendingCommits.clear()
        }
    }

    for (item in this) {
        when (item) {
            is GiteaTimelineItem.Commit -> pendingCommits += item
            is GiteaTimelineItem.Comment -> {
                flushCommits()
                result += GiteaPRTimelineItemViewModel.Comment(item.id, item.actor, item.timestamp, item.body, item.htmlUrl, item.updatedAt)
            }
            is GiteaTimelineItem.Review -> {
                flushCommits()
                result += GiteaPRTimelineItemViewModel.Review(
                    item.actor, item.timestamp, item.state, item.body, item.htmlUrl, item.threads,
                )
            }
            is GiteaTimelineItem.Event -> {
                flushCommits()
                result += GiteaPRTimelineItemViewModel.Event(
                    item.actor, item.timestamp, item.kind, item.label, item.user, item.oldValue, item.newValue,
                )
            }
        }
    }
    flushCommits()
    return result
}
