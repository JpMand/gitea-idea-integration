package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class GiteaPRFileStatusEnum {
    added,
    modified,
    deleted,
    renamed,
    copied,
    changed,

    @JsonEnumDefaultValue
    unchanged
}

