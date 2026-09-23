package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Label

class GiteaLabel(
    val id: Long,
    val name: String,
    val color: String
){
    companion object {
        fun fromDto(dto: Label): GiteaLabel {
            return GiteaLabel(
                id = dto.id ?: 0L,
                name = dto.name ?: "",
                color = dto.color ?: ""
            )
        }
    }
}
