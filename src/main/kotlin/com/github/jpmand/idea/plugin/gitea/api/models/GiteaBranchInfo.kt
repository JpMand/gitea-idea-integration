package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryPath

data class GiteaBranchInfo(
    val label: String,
    val ref: String,
    val sha: String,
    val repoId: Int,
    val repoPath: GiteaRepositoryPath
)
