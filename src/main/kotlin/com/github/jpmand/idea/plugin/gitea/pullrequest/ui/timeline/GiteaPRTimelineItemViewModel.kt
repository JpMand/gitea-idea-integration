package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaLabel
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import java.util.Date

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
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val body: String?,
        val htmlUrl: String?,
    ) : GiteaPRTimelineItemViewModel

    data class Commits(
        val commits: List<GiteaTimelineItem.Commit>,
    ) : GiteaPRTimelineItemViewModel {
        override val actor: GiteaUser? get() = commits.firstOrNull()?.actor
        override val timestamp: Date get() = commits.last().timestamp
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
                result += GiteaPRTimelineItemViewModel.Comment(item.actor, item.timestamp, item.body, item.htmlUrl)
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
