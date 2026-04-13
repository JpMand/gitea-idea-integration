package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class GiteaPRFileStatusEnum {
    ADDED,
    MODIFIED,
    DELETED,
    RENAMED,
    COPIED,
    CHANGED,

    @JsonEnumDefaultValue
    UNCHANGED
}

