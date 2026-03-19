package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * Domain-level representation of a changed file status in a pull request.
 * Mirrors [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPRFileStatusEnum]
 * but lives in the domain layer so domain objects do not depend on the REST layer.
 */
enum class GiteaFileStatus {
    ADDED,
    MODIFIED,
    DELETED,
    RENAMED,
    COPIED,
    CHANGED,
    UNCHANGED,
    UNKNOWN
}

