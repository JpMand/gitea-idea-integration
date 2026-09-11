package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit
import java.util.Date

data class GiteaCommit(
    val sha: String,
    val author: GiteaUser?,
    /** Raw git-commit author name (from the commit metadata), used when [author] — the matched
     * Gitea account — is unavailable. */
    val authorName: String?,
    val messageTitle: String,
    val htmlUrl: String?,
    val createdAt: Date?,
    val firstParentSha: String?,
) {
    companion object {
        fun fromDto(dto: Commit): GiteaCommit {
            val sha = dto.sha.orEmpty()
            return GiteaCommit(
                sha = sha,
                author = dto.author?.let { GiteaUser.fromDto(it) },
                authorName = dto.commit?.author?.name,
                messageTitle = dto.commit?.message?.lineSequence()?.firstOrNull()?.trim().orEmpty()
                    .ifEmpty { sha.take(7) },
                htmlUrl = dto.htmlUrl,
                createdAt = dto.created?.toDate(),
                firstParentSha = dto.parents?.firstOrNull()?.sha,
            )
        }
    }
}
