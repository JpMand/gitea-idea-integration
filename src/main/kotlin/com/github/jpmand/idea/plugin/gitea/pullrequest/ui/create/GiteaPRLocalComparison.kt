package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.VcsCommitMetadata
import git4idea.GitRevisionNumber
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.history.GitHistoryUtils
import git4idea.util.GitFileUtils

private val LOG = logger<GiteaPRLocalComparison>()

/**
 * What a pull request from a head ref into a base ref would contain, read from the local git
 * repository — so it works before the head branch is pushed, as in the GitHub plugin.
 */
class GiteaPRLocalComparison(
    val mergeBase: String,
    val headSha: String,
    /** Newest first, as `git log` lists them. */
    val commits: List<VcsCommitMetadata>,
    /** Base (the merge base) to head. */
    val changes: List<Change>,
) {
    companion object {
        /**
         * Compares [headRef] against [baseRef] in [root]. Blocking: call it on a background thread.
         * Null when the two have no common history.
         */
        fun compute(project: Project, root: VirtualFile, baseRef: String, headRef: String): GiteaPRLocalComparison? {
            val mergeBase = GitHistoryUtils.getMergeBase(project, root, baseRef, headRef)?.asString() ?: return null
            val headSha = GitRevisionNumber.resolve(project, root, headRef).asString()
            val commits = collectCommits(project, root, "$mergeBase..$headSha")
            val changes = if (commits.isEmpty()) emptyList() else GitChangeUtils.getDiff(project, root, mergeBase, headSha, null).toList()
            LOG.debug("$baseRef..$headRef: merge base $mergeBase, ${commits.size} commits, ${changes.size} changed files")
            return GiteaPRLocalComparison(mergeBase, headSha, commits, changes)
        }

        /** The commits in [range], newest first, as `git log` lists them. Blocking. */
        private fun collectCommits(project: Project, root: VirtualFile, range: String): List<VcsCommitMetadata> {
            val ids = GitHistoryUtils.collectTimedCommits(project, root, range).map { it.id }
            if (ids.isEmpty()) return emptyList()
            // Loads the listed commits only (--no-walk), in no particular order.
            val byId = GitHistoryUtils.collectCommitsMetadata(project, root, *ids.map { it.asString() }.toTypedArray()).orEmpty().associateBy { it.id }
            return ids.mapNotNull(byId::get)
        }

        /** The changes of one commit against its first parent. Blocking. */
        fun commitChanges(project: Project, root: VirtualFile, commit: VcsCommitMetadata): List<Change> {
            val parent = commit.parents.firstOrNull()?.asString() ?: return emptyList()
            return GitChangeUtils.getDiff(project, root, parent, commit.id.asString(), null).toList()
        }

        /**
         * Whether [headRef] merges into [baseRef] without conflicts, by a `git merge-tree` dry run (the
         * check the Details tab does too). Null when git can't tell, e.g. a git too old for the
         * two-argument `merge-tree`. Blocking.
         */
        fun mergesCleanly(project: Project, root: VirtualFile, baseRef: String, headRef: String): Boolean? {
            val handler = GitLineHandler(project, root, GitCommand.MERGE_TREE).apply {
                setSilent(true)
                addParameters(baseRef, headRef)
            }
            val result = Git.getInstance().runCommand(handler)
            return when (result.exitCode) {
                0 -> true
                1 -> false
                else -> null.also { LOG.debug("merge-tree $baseRef $headRef exited with ${result.exitCode}") }
            }
        }

        /** The content of [path] at [ref], or null when it doesn't exist there. Blocking. */
        fun readFile(project: Project, root: VirtualFile, ref: String, path: String): String? =
            try {
                GitFileUtils.getFileContent(project, root, ref, path).toString(Charsets.UTF_8)
            } catch (_: VcsException) {
                null
            }
    }
}
