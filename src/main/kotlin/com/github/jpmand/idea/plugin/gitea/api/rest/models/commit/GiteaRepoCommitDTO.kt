package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

/**
 * RepoCommit contains information of a commit in the context of a repository.
 */
open class GiteaRepoCommitDTO(
  val author: GiteaCommitUserDTO,
  val committer: GiteaCommitUserDTO,
  val message: String,
  val tree: GiteaCommitMetaDTO,
  val url: String,
  val verification: GiteaPayloadCommitVerificationDTO
) {
}