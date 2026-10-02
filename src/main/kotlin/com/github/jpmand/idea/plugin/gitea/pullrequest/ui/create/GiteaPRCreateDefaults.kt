package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create

/**
 * Where Gitea looks for a pull request template, in its own order (`pullRequestTemplateCandidates`
 * in Gitea's `routers/web/repo/compare.go`): the first one present on the base branch is used.
 */
internal val PR_TEMPLATE_CANDIDATES: List<String> = listOf("", ".gitea/", ".github/").flatMap { dir ->
    listOf("PULL_REQUEST_TEMPLATE", "pull_request_template").flatMap { name ->
        listOf("md", "yaml", "yml").map { ext -> "$dir$name.$ext" }
    }
}

/** A commit's message, split the way git does: the first line, and the rest after the blank line. */
internal data class GiteaCommitMessage(val subject: String, val body: String)

internal fun GiteaCommitMessage(fullMessage: String): GiteaCommitMessage {
    val lines = fullMessage.trim().lines()
    return GiteaCommitMessage(lines.firstOrNull().orEmpty().trim(), lines.drop(1).joinToString("\n").trim())
}

/**
 * The title and description a new pull request starts with. With one commit, its subject and body,
 * as the GitHub plugin does; otherwise the head branch's name made readable ("feature/add-cache" →
 * "Add cache"). A [template] goes first in the description, the commit body after it.
 */
internal fun defaultTitleAndDescription(
    headBranch: String,
    commits: List<GiteaCommitMessage>,
    template: String?,
): Pair<String, String> {
    val single = commits.singleOrNull()
    val title = single?.subject?.takeIf { it.isNotBlank() } ?: humanizeBranchName(headBranch)
    val description = listOfNotNull(template?.trim()?.takeIf { it.isNotEmpty() }, single?.body?.takeIf { it.isNotEmpty() })
        .joinToString("\n\n")
    return title to description
}

/** "feature/add-cache_layer" → "Add cache layer": the last path segment, separators as spaces. */
internal fun humanizeBranchName(branch: String): String {
    val words = branch.substringAfterLast('/').replace('-', ' ').replace('_', ' ').trim()
    return words.replaceFirstChar { it.uppercaseChar() }.ifEmpty { branch }
}

private val WIP_PREFIX = Regex("""^\s*(WIP:|\[WIP])\s*""", RegexOption.IGNORE_CASE)

/**
 * [title] marked as work in progress: Gitea has no draft flag and treats a title starting with one
 * of its WIP prefixes as a draft. Keeps a prefix the title already has.
 */
internal fun withWipPrefix(title: String): String =
    if (WIP_PREFIX.containsMatchIn(title)) title.trim() else "WIP: ${title.trim()}"
