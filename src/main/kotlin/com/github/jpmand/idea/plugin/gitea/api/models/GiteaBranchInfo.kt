package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryPath
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PRBranchInfo

class GiteaBranchInfo(
    val label: String,
    val ref: String,
    val sha: String,
    val repoId: Long,
    val repoPath: GiteaRepositoryPath
){
    companion object {
        fun fromDto(dto: PRBranchInfo): GiteaBranchInfo {
            return GiteaBranchInfo(
                label = dto.label ?: "",
                ref = dto.ref ?: "",
                sha = dto.sha ?: "",
                repoId = dto.repo?.id ?: 0L,
                repoPath = GiteaRepositoryPath(owner = dto.repo?.owner?.login ?: "", repository = dto.repo?.name ?: "")
            )
        }
    }
}
