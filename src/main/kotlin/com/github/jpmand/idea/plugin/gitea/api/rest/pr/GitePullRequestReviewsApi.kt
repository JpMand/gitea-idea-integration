@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.api.rest.pr

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaCreatePullRequestReviewRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewDTO
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

suspend fun GiteaApi.listAllPullRequestReviews(
    owner: String,
    repo: String,
    index: Int,
    page: Int?,
    limit: Int?
): List<GiteaPullRequestReviewDTO> {
    val uri = GiteaUriUtil.QueryBuilder()
        .addParam("page", page)
        .addParam("limit", limit)
        .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews"))
    val request = request(uri).GET().build()
    return rest.loadJsonList<GiteaPullRequestReviewDTO>(request).body()
}

suspend fun GiteaApi.GetPullRequestReview(
    owner: String,
    repo: String,
    index: Int,

)

suspend fun GiteaApi.createPullRequestReview(
    owner: String,
    repo: String,
    index: Int,
    review: GiteaCreatePullRequestReviewRequestDTO
): GiteaPullRequestReviewDTO {
    val uri = GiteaUriUtil.QueryBuilder()
        .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews"))
    val request = request(uri).POST(rest.jsonBodyPublisher(uri, review))
        .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
        .build()
    return rest.loadJsonValue<GiteaPullRequestReviewDTO>(request).body()
}

suspend fun GiteaApi.DeletePullRequestReview(
    owner: String,
    repo: String,
    index: Int,
    reviewId: Int
) : Boolean {
    val uri = GiteaUriUtil.QueryBuilder()
        .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$reviewId"))
    val request = request(uri).DELETE().build()
    return rest.loadJsonValue<Boolean>(request).statusCode() == 204
}