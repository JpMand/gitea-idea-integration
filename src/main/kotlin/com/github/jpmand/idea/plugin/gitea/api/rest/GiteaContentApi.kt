package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaFileContentsDTO
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative
import java.net.URLEncoder

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getFileContents(
    owner: String,
    repo: String,
    filepath: String,
    ref: String,
): GiteaFileContentsDTO {
    val encodedPath = filepath.split("/").joinToString("/") {
        URLEncoder.encode(it, Charsets.UTF_8).replace("+", "%20")
    }
    val baseUri = server.restApiUri().resolveRelative("repos/$owner/$repo/contents/$encodedPath")
    val uri = GiteaUriUtil.QueryBuilder().addParam("ref", ref).build(baseUri)
    val request = request(uri).GET().build()
    return rest.loadJsonValue<GiteaFileContentsDTO>(request).body()
}
