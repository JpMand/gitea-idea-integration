package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaCombinedStatusDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaCommitStatusDTO
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

/** GET /repos/{owner}/{repo}/commits/{ref}/status — combined CI status for a ref. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCombinedStatus(
    owner: String,
    repo: String,
    ref: String,
): GiteaCombinedStatusDTO {
    val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/commits/$ref/status")
    val request = request(uri).GET().build()
    return rest.loadJsonValue<GiteaCombinedStatusDTO>(request).body()
}

/** GET /repos/{owner}/{repo}/commits/{ref}/statuses — paginated list of commit statuses. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListCommitStatuses(
    owner: String,
    repo: String,
    ref: String,
    page: Int? = null,
    limit: Int? = null,
): List<GiteaCommitStatusDTO> {
    val uri = GiteaUriUtil.QueryBuilder()
        .addParam("page", page)
        .addParam("limit", limit)
        .build(server.restApiUri().resolveRelative("repos/$owner/$repo/commits/$ref/statuses"))
    val request = request(uri).GET().build()
    return rest.loadJsonList<GiteaCommitStatusDTO>(request).body()
}
