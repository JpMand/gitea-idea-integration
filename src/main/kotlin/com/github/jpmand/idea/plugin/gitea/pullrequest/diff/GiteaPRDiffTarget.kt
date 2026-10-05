package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.intellij.diff.util.Side

/**
 * Which version of a pull request the PR diff shows: the whole PR at its current head (what it
 * shows by default) or a single commit of it.
 */
sealed interface GiteaPRDiffTarget {

    /** The whole PR at its current head: merge base..head. */
    data object PullRequest : GiteaPRDiffTarget

    /** One commit of the PR, against its first parent — the changes tree with a commit selected. */
    data class Commit(val sha: String, val parentSha: String) : GiteaPRDiffTarget
}

/** New comments are drafted against the current head, so only the PR diff takes them. */
val GiteaPRDiffTarget.allowsNewComments: Boolean get() = this == GiteaPRDiffTarget.PullRequest

/**
 * Whether a review thread belongs in this diff, on [side] (the side its line is on). The PR diff
 * shows every thread. A commit's diff shows the file at that commit, so it shows only the threads
 * of reviews made on that commit, whose line numbers belong to it: on the right, the commit's own
 * version of the file; on the left, the merge base, which a commit's diff only has on its left
 * when the commit's parent is the merge base.
 */
fun GiteaPRDiffTarget.showsThread(reviewCommitId: String?, side: Side, mergeBaseSha: String): Boolean = when (this) {
    GiteaPRDiffTarget.PullRequest -> true
    is GiteaPRDiffTarget.Commit -> reviewCommitId == sha && (side == Side.RIGHT || parentSha == mergeBaseSha)
}
