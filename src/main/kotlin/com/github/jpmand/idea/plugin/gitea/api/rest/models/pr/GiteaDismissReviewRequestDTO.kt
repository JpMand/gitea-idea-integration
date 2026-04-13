package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

/** POST body for `POST /repos/{owner}/{repo}/pulls/{index}/reviews/{id}/dismissals`. */
open class GiteaDismissReviewRequestDTO(val message: String)

