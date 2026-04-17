package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryPath
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaBranchInfo
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaRepositoryDTO

open class GiteaPRBranchInfoDTO(
  val label: String,
  val ref: String,
  val repo: GiteaRepositoryDTO,
  val repoId: Int,
  val sha: String
) {
  fun toBranchInfo(): GiteaBranchInfo = GiteaBranchInfo(
    label = label,
    ref = ref,
    sha = sha,
    repoId = repoId,
    repoPath = GiteaRepositoryPath(repo.owner.login, repo.name)
  )
}