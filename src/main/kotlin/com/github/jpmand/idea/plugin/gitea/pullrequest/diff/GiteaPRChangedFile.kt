package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ChangedFile
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CommitAffectedFiles
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum

data class GiteaPRChangedFile(
    val filename: String,
    val previousFilename: String?,
    val status: GiteaPRFileStatusEnum?,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
)

fun ChangedFile.toChangedFile() = GiteaPRChangedFile(
    filename = filename.orEmpty(),
    previousFilename = previousFilename,
    status = status.toFileStatus(),
    additions = additions?.toInt() ?: 0,
    deletions = deletions?.toInt() ?: 0,
    changes = changes?.toInt() ?: 0,
)

/** For a single-commit view: the Gitea commit endpoint only carries filename + status. */
fun CommitAffectedFiles.toChangedFile() = GiteaPRChangedFile(
    filename = filename.orEmpty(),
    previousFilename = null,
    status = status.toFileStatus(),
    additions = 0,
    deletions = 0,
    changes = 0,
)

private fun String?.toFileStatus(): GiteaPRFileStatusEnum? =
    this?.let { s -> GiteaPRFileStatusEnum.entries.firstOrNull { it.name.equals(s, ignoreCase = true) } }

/**
 * The files a raw unified diff (`git diff` output, as Gitea's compare endpoint returns it) changes,
 * in diff order. Names and status come from each file's extended header (`new file mode`,
 * `deleted file mode`, `rename from`/`to`, `copy from`/`to`, `---`/`+++`), falling back to its
 * `diff --git` line (a binary or mode-only change has nothing else); line counts from its hunks.
 */
fun parseDiffChangedFiles(diff: String): List<GiteaPRChangedFile> {
    val files = mutableListOf<GiteaPRChangedFile>()
    var current: DiffFileHeader? = null
    for (line in diff.lineSequence()) {
        val file = current
        when {
            line.startsWith("diff --git ") -> {
                file?.let { files += it.toChangedFile() }
                current = DiffFileHeader(diffGitPaths(line.removePrefix("diff --git ")))
            }
            file == null -> Unit
            file.inHunks -> when {
                line.startsWith("+") -> file.additions++
                line.startsWith("-") -> file.deletions++
            }
            line.startsWith("@@") -> file.inHunks = true
            line.startsWith("new file mode") -> file.status = GiteaPRFileStatusEnum.ADDED
            line.startsWith("deleted file mode") -> file.status = GiteaPRFileStatusEnum.DELETED
            line.startsWith("rename from ") -> { file.oldPath = unquoteGitPath(line.removePrefix("rename from ")); file.status = GiteaPRFileStatusEnum.RENAMED }
            line.startsWith("rename to ") -> file.newPath = unquoteGitPath(line.removePrefix("rename to "))
            line.startsWith("copy from ") -> { file.oldPath = unquoteGitPath(line.removePrefix("copy from ")); file.status = GiteaPRFileStatusEnum.COPIED }
            line.startsWith("copy to ") -> file.newPath = unquoteGitPath(line.removePrefix("copy to "))
            line.startsWith("--- ") -> diffSidePath(line.removePrefix("--- "), "a/")?.let { file.oldPath = it }
            line.startsWith("+++ ") -> diffSidePath(line.removePrefix("+++ "), "b/")?.let { file.newPath = it }
        }
    }
    current?.let { files += it.toChangedFile() }
    return files
}

private class DiffFileHeader(paths: Pair<String, String>?) {
    var oldPath: String = paths?.first.orEmpty()
    var newPath: String = paths?.second.orEmpty()
    var status = GiteaPRFileStatusEnum.MODIFIED
    var inHunks = false
    var additions = 0
    var deletions = 0

    fun toChangedFile() = GiteaPRChangedFile(
        filename = if (status == GiteaPRFileStatusEnum.DELETED) oldPath else newPath,
        previousFilename = oldPath.takeIf { status == GiteaPRFileStatusEnum.RENAMED || status == GiteaPRFileStatusEnum.COPIED },
        status = status,
        additions = additions,
        deletions = deletions,
        changes = additions + deletions,
    )
}

/** The two paths of a `diff --git a/<old> b/<new>` line, either of them possibly quoted. */
private fun diffGitPaths(rest: String): Pair<String, String>? {
    if (rest.startsWith("\"")) {
        val end = closingQuote(rest) ?: return null
        val old = unquoteGitPath(rest.substring(0, end + 1)).removePrefix("a/")
        val new = unquoteGitPath(rest.substring(end + 1).trimStart()).removePrefix("b/")
        return old to new
    }
    if (rest.endsWith("\"")) {
        val start = rest.lastIndexOf(" \"").takeIf { it >= 0 } ?: return null
        return rest.substring(0, start).removePrefix("a/") to unquoteGitPath(rest.substring(start + 1)).removePrefix("b/")
    }
    // Unquoted: both names are the same unless the file was renamed, which the header lines
    // after this one tell anyway, so try the even split first — a name may contain " b/".
    val n = (rest.length - 5) / 2
    if (rest.length % 2 == 1 && n > 0 && rest.startsWith("a/") && rest.substring(2 + n) == " b/" + rest.substring(2, 2 + n)) {
        return rest.substring(2, 2 + n).let { it to it }
    }
    val split = rest.indexOf(" b/").takeIf { it >= 0 } ?: return null
    return rest.substring(0, split).removePrefix("a/") to rest.substring(split + 3)
}

/** A `---`/`+++` line's path without its [prefix]; null for `/dev/null` (an added or deleted file). */
private fun diffSidePath(value: String, prefix: String): String? {
    val path = unquoteGitPath(value.substringBefore('\t'))
    return if (path == "/dev/null") null else path.removePrefix(prefix)
}

private fun closingQuote(s: String): Int? {
    var i = 1
    while (i < s.length) {
        when (s[i]) {
            '\\' -> i++
            '"' -> return i
        }
        i++
    }
    return null
}

/** Undoes git's C-style quoting of a path with special characters (`"a/caf\303\251"`). */
internal fun unquoteGitPath(path: String): String {
    if (path.length < 2 || !path.startsWith("\"") || !path.endsWith("\"")) return path
    val bytes = java.io.ByteArrayOutputStream()
    val s = path.substring(1, path.length - 1)
    var i = 0
    while (i < s.length) {
        if (s[i] != '\\' || i + 1 >= s.length) {
            // A run of plain characters, whole, so surrogate pairs stay together.
            val end = s.indexOf('\\', i + 1).takeIf { it >= 0 && it + 1 < s.length } ?: s.length
            bytes.writeBytes(s.substring(i, end).toByteArray(Charsets.UTF_8))
            i = end
            continue
        }
        val octal = s.substring(i + 1, minOf(i + 4, s.length))
        if (octal.length == 3 && octal.all { it in '0'..'7' }) {
            bytes.write(octal.toInt(8))
            i += 4
            continue
        }
        val next = s[i + 1]
        bytes.write(
            when (next) {
                'n' -> '\n'.code
                't' -> '\t'.code
                'r' -> '\r'.code
                'a' -> 7
                'b' -> 8
                'f' -> 12
                'v' -> 11
                else -> next.code // \\ and \"
            },
        )
        i += 2
    }
    return bytes.toString(Charsets.UTF_8)
}
