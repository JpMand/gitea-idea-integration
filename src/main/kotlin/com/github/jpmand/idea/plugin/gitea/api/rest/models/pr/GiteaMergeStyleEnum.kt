package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.fasterxml.jackson.annotation.JsonValue

/** Merge strategy sent in the `Do` field of a merge pull-request request. */
enum class GiteaMergeStyleEnum(val apiValue: String) {
    MERGE("merge"),
    REBASE("rebase"),
    REBASE_MERGE("rebase-merge"),
    SQUASH("squash"),
    FAST_FORWARD_ONLY("fast-forward-only"),
    MANUALLY_MERGED("manually-merged");

    @JsonValue
    fun toJson(): String = apiValue
}
