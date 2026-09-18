package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CombinedStatus
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

/** GET /repos/{owner}/{repo}/commits/{ref}/status — combined CI status for a ref. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCombinedStatus(
    owner: String,
    repo: String,
    ref: String,
): CombinedStatus {
    val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/commits/$ref/status")
    val request = request(uri).GET().build()
    return rest.loadJsonValue<CombinedStatus>(request).body()
}
