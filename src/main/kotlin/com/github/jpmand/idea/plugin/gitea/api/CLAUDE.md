# `api/` — Gitea HTTP client + JSON layer

## Adding a New REST API Call

1. Look up the endpoint in the in-repo `gitea-swagger-v2-spec.json` to confirm the path, method, and response schema.
2. Create (or extend) the DTO in `api/rest/dto/` matching the response schema fields (camelCase constructor params, every field nullable with a default).
3. Write a `suspend` extension function on `GiteaApi` in `api/rest/` (PR endpoints go in `api/rest/pr/`):
   ```kotlin
   @Suppress("UnstableApiUsage")
   suspend fun GiteaApi.listOrgRepos(org: String): List<Repository> {
       val uri = server.restApiUri().resolveRelative("orgs/$org/repos")
       val request = request(uri).GET().build()
       return rest.loadJsonList<Repository>(request).body()
   }
   ```
4. If callers outside `api/` need the data, add `.toXxx()` on the DTO and a domain class in `api/models/`.

See the DTO↔domain, JSON-mapping, and `@Suppress("UnstableApiUsage")` rules under "Critical Conventions" in the root `AGENTS.md`.
