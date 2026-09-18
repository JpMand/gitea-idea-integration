package com.github.jpmand.idea.plugin.gitea.api

/**
 * Default page size for Gitea list endpoints. Gitea caps server-side at `MAX_RESPONSE_ITEMS`
 * (50 by default), so asking for more than that per page is pointless.
 */
const val GITEA_PAGE_SIZE: Int = 50

/**
 * Fetches every page of a Gitea list endpoint by requesting 1-based pages until a page comes
 * back shorter than [pageSize] (Gitea has no reliable total-count header across versions).
 * [maxPages] bounds the work for pathological cases.
 *
 * [load] receives the 1-based page number and must request exactly [pageSize] items.
 */
suspend fun <T> loadAllGiteaPages(
  pageSize: Int = GITEA_PAGE_SIZE,
  maxPages: Int = 40,
  load: suspend (page: Int) -> Collection<T>,
): List<T> {
  val all = ArrayList<T>()
  var page = 1
  while (page <= maxPages) {
    val batch = load(page)
    all += batch
    if (batch.size < pageSize) break
    page++
  }
  return all
}
