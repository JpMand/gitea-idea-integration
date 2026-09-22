package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.UserSearchResult
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

/**
 * Search for users across the whole Gitea instance — unlike `GET /admin/users`, this needs no
 * site-admin rights (confirmed against `gitea-swagger-v2-spec.json`: no `"admin"` tag, no
 * per-endpoint `security` override), so it's safe to use for the Request Review picker's
 * "all instance users" mode.
 *
 * @see <a href="https://gitea.com/api/swagger#/user/userSearch">GET /users/search</a>
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.userSearch(
  q: String? = null,
  page: Int? = null,
  limit: Int? = null,
): UserSearchResult {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("q", q)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("users/search"))
  val request = request(uri).GET().build()
  return rest.loadJsonValue<UserSearchResult>(request).body()
}
