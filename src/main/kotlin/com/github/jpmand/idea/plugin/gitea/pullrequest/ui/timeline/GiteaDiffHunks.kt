package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

private val HUNK_HEADER = Regex("""^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)$""")

/**
 * Repairs the `diff_hunk` Gitea stores with a review comment so the platform's patch parser can read it.
 *
 * Gitea trims a comment's hunk to the lines just before the commented one, and counts a
 * `\ No newline at end of file` marker as a diff line while doing so. The result can open with an
 * orphan marker and carry a header whose starts and counts don't match its body; the parser then
 * yields no lines and the comment shows no diff. The end of each header range is still right,
 * because Gitea computes the header backwards from the commented line.
 *
 * Like GitLab (`DiscussionOnDiff#truncated_diff_lines`), the context restarts after every marker
 * that comes before the last line, so it never starts on one; a marker after the last line still
 * belongs to it and stays. The header is rebuilt from the kept lines, keeping its range ends.
 * A hunk without such a marker comes back unchanged.
 */
internal fun normalizeDiffHunk(diffHunk: String): String {
    val lines = diffHunk.trimEnd('\n').split('\n')
    val match = HUNK_HEADER.matchEntire(lines.first()) ?: return diffHunk
    val (oldStart, oldCount, newStart, newCount, section) = match.destructured
    val body = lines.drop(1)

    val lastContent = body.indexOfLast { !it.startsWith('\\') }
    if (lastContent < 0 || body.take(lastContent).none { it.startsWith('\\') }) return diffHunk
    val kept = body.subList(body.subList(0, lastContent).indexOfLast { it.startsWith('\\') } + 1, body.size)

    val keptOld = kept.count { !it.startsWith('+') && !it.startsWith('\\') }
    val keptNew = kept.count { !it.startsWith('-') && !it.startsWith('\\') }
    val oldEnd = oldStart.toInt() + (oldCount.ifEmpty { "1" }).toInt()
    val newEnd = newStart.toInt() + (newCount.ifEmpty { "1" }).toInt()
    val header = "@@ -${rangeStart(oldEnd, keptOld)},$keptOld +${rangeStart(newEnd, keptNew)},$keptNew @@$section"
    return (listOf(header) + kept).joinToString("\n")
}

/** An empty range names the line before it, as `git diff` does (`@@ -2,0 +3,1 @@`). */
private fun rangeStart(endExclusive: Int, count: Int): Int =
    if (count == 0) (endExclusive - 1).coerceAtLeast(0) else endExclusive - count
