package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class GiteaReviewStateEnum {
    APPROVED,
    REQUEST_CHANGES,
    COMMENT,
    REQUEST_REVIEW,

    @JsonEnumDefaultValue
    PENDING
}

