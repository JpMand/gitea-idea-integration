package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ContentsResponse
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative
import java.net.URLEncoder
import java.util.Base64

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getFileContents(
    owner: String,
    repo: String,
    filepath: String,
    ref: String,
): ContentsResponse {
    val encodedPath = filepath.split("/").joinToString("/") {
        URLEncoder.encode(it, Charsets.UTF_8).replace("+", "%20")
    }
    val baseUri = server.restApiUri().resolveRelative("repos/$owner/$repo/contents/$encodedPath")
    val uri = GiteaUriUtil.QueryBuilder().addParam("ref", ref).build(baseUri)
    val request = request(uri).GET().build()
    return rest.loadJsonValue<ContentsResponse>(request).body()
}

fun ContentsResponse.decodeContent(): String? {
    if (encoding != "base64" || content == null) return null
    return try {
        Base64.getDecoder().decode(content.replace("\n", "").replace("\r", ""))
            .toString(Charsets.UTF_8)
    } catch (_: Exception) {
        null // binary or non-UTF-8 content
    }
}
