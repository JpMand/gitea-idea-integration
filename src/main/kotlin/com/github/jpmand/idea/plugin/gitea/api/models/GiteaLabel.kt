package com.github.jpmand.idea.plugin.gitea.api.models

data class GiteaLabel(
    val id: Int,
    val name: String,
    val color: String,
    val description: String
)

