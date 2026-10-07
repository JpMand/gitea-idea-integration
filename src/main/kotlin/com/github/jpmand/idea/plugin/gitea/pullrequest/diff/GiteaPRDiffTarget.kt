package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommit
import com.intellij.diff.util.Side

/**
 * Which version of a pull request the PR diff shows: the whole PR at its current head (what it
 * shows by default), a single commit of it, or the whole PR as it was at an older head.
 */
sealed interface GiteaPRDiffTarget {

    /** The whole PR at its current head: merge base..head. */
    data object PullRequest : GiteaPRDiffTarget

    /** One commit of the PR, against its first parent — the changes tree with a commit selected. */
    data class Commit(val sha: String, val parentSha: String) : GiteaPRDiffTarget

    /** The whole PR as it was when [sha] was its head: merge base..[sha]. */
    data class PullRequestAt(val sha: String) : GiteaPRDiffTarget
}

/** New comments are drafted against the current head, so only the PR diff takes them. */
val GiteaPRDiffTarget.allowsNewComments: Boolean get() = this == GiteaPRDiffTarget.PullRequest

/**
 * Whether a review thread belongs in this diff, on [side] (the side its line is on). The PR diff
 * shows every thread. Any other diff shows the file at an older commit, so it shows only the
 * threads of reviews made on that commit, whose line numbers belong to it: on the right, the
 * commit's own version of the file; on the left, the merge base, which a commit's diff only has on
 * its left when the commit's parent is the merge base.
 */
fun GiteaPRDiffTarget.showsThread(reviewCommitId: String?, side: Side, mergeBaseSha: String): Boolean = when (this) {
    GiteaPRDiffTarget.PullRequest -> true
    is GiteaPRDiffTarget.PullRequestAt -> reviewCommitId == sha
    is GiteaPRDiffTarget.Commit -> reviewCommitId == sha && (side == Side.RIGHT || parentSha == mergeBaseSha)
}

/**
 * The diff a review thread on [path] opens in, its line being on [side]: the commit its review was
 * made on, [reviewCommit] — that commit's own diff when it changed the file, otherwise the whole PR
 * as of that commit (the PR diff when it's still the head). An old-side thread needs the merge
 * base on the left, so it takes the commit's diff only when the commit's parent is the merge base.
 * Without a review commit that is part of the PR (none recorded, or gone after a force-push), the
 * thread opens the PR diff.
 *
 * @param commitFiles the files [reviewCommit] changed.
 */
fun reviewThreadDiffTarget(
    reviewCommit: GiteaCommit?,
    commitFiles: List<GiteaPRChangedFile>,
    path: String,
    side: Side,
    headSha: String,
    mergeBaseSha: String,
): GiteaPRDiffTarget {
    if (reviewCommit == null) return GiteaPRDiffTarget.PullRequest
    val parent = reviewCommit.firstParentSha
    val changedByCommit = commitFiles.any { it.filename == path || it.previousFilename == path }
    if (parent != null && changedByCommit && (side == Side.RIGHT || parent == mergeBaseSha)) {
        return GiteaPRDiffTarget.Commit(reviewCommit.sha, parent)
    }
    return if (reviewCommit.sha == headSha) GiteaPRDiffTarget.PullRequest else GiteaPRDiffTarget.PullRequestAt(reviewCommit.sha)
}
