package com.github.jpmand.idea.plugin.gitea.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryPath
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaRepositoryDTO
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls

data class GiteaRepositoryDetails(
    val id: String,
    val path: GiteaRepositoryPath,
    val name: @Nls String,
    val nameWithNamespace: @Nls String,
    val httpUrl: @NlsSafe String,
    val sshUrl: @NlsSafe String,
    val defaultBranch: String,
    val defaultMergeStyle : String,
    val defaultTargetBranch: String?,
) {
    constructor(path: GiteaRepositoryPath, dto: GiteaRepositoryDTO) : this(
        id = "${dto.id}",
        path = path,
        name = dto.name,
        nameWithNamespace = dto.fullName,
        httpUrl = dto.url,
        sshUrl = dto.sshUrl,
        defaultBranch = dto.defaultBranch,
        defaultMergeStyle = dto.defaultMergeStyle,
        defaultTargetBranch = dto.defaultTargetBranch
    )
}