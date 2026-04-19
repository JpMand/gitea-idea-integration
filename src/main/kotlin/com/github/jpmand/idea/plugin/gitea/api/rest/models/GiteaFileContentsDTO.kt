package com.github.jpmand.idea.plugin.gitea.api.rest.models

import java.util.Base64

class GiteaFileContentsDTO(
    val type: String,
    val encoding: String?,
    val content: String?,
    val downloadUrl: String?,
    val name: String,
    val path: String,
    val sha: String?,
    val size: Long,
) {
    fun decodeContent(): String? {
        if (encoding != "base64" || content == null) return null
        return try {
            Base64.getDecoder().decode(content.replace("\n", "").replace("\r", ""))
                .toString(Charsets.UTF_8)
        } catch (_: Exception) {
            null // binary or non-UTF-8 content
        }
    }
}
