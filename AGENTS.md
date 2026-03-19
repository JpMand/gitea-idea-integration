# AGENTS.md – Gitea IntelliJ Plugin

## Project Overview
IntelliJ Platform plugin that integrates Gitea (self-hosted Git) into JetBrains IDEs.
Generated from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template), which provides a preconfigured Gradle build, CI pipeline, signing workflow, and changelog tooling out of the box.
Modelled after the official [GitHub](https://github.com/JetBrains/intellij-community/tree/master/plugins/github) and [GitLab](https://github.com/JetBrains/intellij-community/tree/master/plugins/gitlab) plugins in [intellij-community](https://github.com/JetBrains/intellij-community) repository. When unsure how something should work, consult those reference implementations.

### Gitea API Contract
The **Gitea Swagger v1 specification** at <https://gitea.com/swagger.v1.json> is the single source of truth for:
- Every endpoint path and HTTP method when writing `suspend` extension functions in `api/rest/`
- Every response JSON shape when creating or extending DTOs in `api/rest/models/`
- Field names and their types (resolve snake_case names to camelCase DTO constructor parameters)

Always open the spec before adding a new REST call or a new DTO field.

---

## Key Commands
```bash
./gradlew build          # Compile + package plugin jar
./gradlew test           # Run all tests
./gradlew runIde         # Launch IDE sandbox with plugin loaded
./gradlew runIdeForUiTests  # Sandbox wired for robot-server UI tests (port 8082)
./gradlew publishPlugin  # Requires PUBLISH_TOKEN env var
```
All version/platform coordinates live in `gradle.properties` (not `build.gradle.kts`).

---

## Plugin Template Scaffold
This project was bootstrapped from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/README.md). The template provides preconfigured:

| Concern | Detail |
|---|---|
| Build | `build.gradle.kts` with IntelliJ Platform Gradle Plugin; all version coordinates in `gradle.properties` |
| Plugin description | Extracted automatically from the `<!-- Plugin description --> … <!-- Plugin description end -->` block in `README.md` |
| Changelog | `CHANGELOG.md` drives `changeNotes` via the Gradle Changelog Plugin ([Keep a Changelog](https://keepachangelog.com) format) |
| CI – build | `.github/workflows/build.yml` – validate, test, Qodana scan, `buildPlugin`, `runPluginVerifier`, draft GitHub release |
| CI – release | `.github/workflows/release.yml` – publishes to JetBrains Marketplace when a version tag is pushed (requires `PUBLISH_TOKEN` secret) |
| CI – UI tests | `.github/workflows/run-ui-tests.yml` – robot-server UI tests |
| Signing | Controlled via `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` GitHub secrets |
| Dependency updates | `.github/dependabot.yml` keeps Gradle plugins and GitHub Actions current |

---

## Architecture

```
api/                         ← HTTP client + JSON layer
  GiteaApi.kt                  Bearer-token auth via IntelliJ HttpApiHelper
  GiteaApiManager.kt           Factory: getClient(server, token) / getUnauthenticatedClient()
  GiteaJsonDeSerializer.kt     Jackson singleton (SNAKE_CASE, ANY field visibility)
  GiteaServerPath.kt           Parses server URL; restApiUri() appends /api/v1/
  rest/                        Thin suspend-fun API wrappers (GiteaUsersApi, etc.)
  rest/models/                 DTOs deserialized from Gitea REST responses
  models/                      Domain model objects (converted from DTOs via .toUser() etc.)

authentication/
  account/
    GiteaAccount.kt            Data class (server + name + id); XML-serialized
    GiteaAccountManager.kt     Interface + PersistentGiteaAccountManager (PasswordSafe)
    GitePersistentAccounts.kt  Application-level XML state persistence
  extensions/
    GiteaSilentHttpAuthDataProvider  Tries token silently (no UI) – registered first
    GiteaHttpAuthDataProvider        Falls back with interactive login dialog
  ui/                          Account settings panel + login dialogs (TokenLoginDialog)

ui/
  GiteaSettingsConfigurable.kt  Settings > VCS > Gitea panel
  clone/                        Clone-from-Gitea UI (ViewModel + Component)

util/
  GiteaBundle.kt               i18n wrapper; all UI strings via GiteaBundle.message("key")
```

---

## Critical Conventions

**Plugin registration**: Every service, extension, and action must be declared in `src/main/resources/META-INF/plugin.xml`. Forgetting this is the most common reason a new component silently does nothing. See: [Plugin Configuration File](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html), [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html), [Plugin Extensions](https://plugins.jetbrains.com/docs/intellij/plugin-extensions.html), [Plugin Actions](https://plugins.jetbrains.com/docs/intellij/plugin-actions.html).

**Unstable API suppression**: The `intellij.platform.collaborationTools` module is internal API. Any file using `HttpApiHelper`, `AccountManagerBase`, `TokenLoginDialog`, etc. needs `@Suppress("UnstableApiUsage")`.

**DTO ↔ Domain split**: REST responses land in `api/rest/models/` (e.g., `GiteaUserDTO`). Call `.toXxx()` to convert to a domain object in `api/models/`. Never pass raw DTOs outside the `api/` layer. Use the [Gitea Swagger spec](https://gitea.com/swagger.v1.json) to verify field names and types before creating a DTO.

**JSON mapping**: `GiteaJsonDeSerializer` uses Jackson with `SNAKE_CASE` strategy and `ANY` field visibility (constructor params, not getters). DTO fields are named in camelCase; Jackson resolves `avatar_url` → `avatarUrl` automatically. Do **not** add `@JsonProperty` for standard snake_case fields.

**i18n**: All user-visible strings belong in `src/main/resources/messages/GiteaBundle.properties`. Access via `GiteaBundle.message("key")` or `GiteaBundle.messagePointer("key")`. See: [Plugin User Experience](https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html).

**Coroutines / threading**: Git4Idea callbacks run on background threads (`@RequiresBackgroundThread`). Bridge to coroutines with `runBlockingMaybeCancellable { }`. UI work must switch via `withContext(Dispatchers.EDT + ModalityState.any().asContextElement())`.

---

## Adding a New REST API Call

1. Look up the endpoint in the [Gitea Swagger spec](https://gitea.com/swagger.v1.json) to confirm the path, method, and response schema.
2. Create (or extend) the DTO in `api/rest/models/` matching the response schema fields (camelCase constructor params).
3. Write a `suspend` extension function on `GiteaApi` in `api/rest/`:
   ```kotlin
   @Suppress("UnstableApiUsage")
   suspend fun GiteaApi.listOrgRepos(org: String): List<GiteaRepositoryDTO> {
       val uri = server.restApiUri().resolveRelative("orgs/$org/repos")
       val request = request(uri).GET().build()
       return rest.loadJsonValue<List<GiteaRepositoryDTO>>(request).body()
   }
   ```
4. If callers outside `api/` need the data, add `.toXxx()` on the DTO and a domain class in `api/models/`.

---

## Testing Patterns
- Tests extend **no base class** for pure unit tests (`GiteaJsonGiteaUserTest`, `GiteaServerPathTest`).
- JSON tests call `GiteaJsonDeSerializer.fromJson(StringReader(json), Dto::class.java)` directly.
- Integration tests that need the platform use JUnit4 + IntelliJ `TestFrameworkType.Platform` (declared in `build.gradle.kts`).
- Test data fixtures (JSON payloads) live in `src/test/testData/` (e.g., `pull_request_list.json`). Use real Gitea API responses from the [Swagger spec](https://gitea.com/swagger.v1.json) as fixtures.
- See: [Tests and Fixtures](https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html), [Light and Heavy Tests](https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html), [Test Project and Testdata Directories](https://plugins.jetbrains.com/docs/intellij/test-project-and-testdata-directories.html), [Testing FAQ](https://plugins.jetbrains.com/docs/intellij/testing-faq.html).

---

## Dependency Notes
- `Git4Idea` is a **bundled** plugin dependency (not external); declared in `platformBundledPlugins` in `gradle.properties`. See: [Plugin Dependencies](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html).
- `intellij.platform.collaborationTools` is a **bundled module**; declared in `platformBundledModules`.
- Plugin targets IntelliJ IDEA `2025.3.1` (`platformVersion`), `sinceBuild = 253`.
- Kotlin stdlib is **not** bundled (`kotlin.stdlib.default.dependency = false`); the platform provides it.

---

## Official Reference Documentation

| Topic | Link |
|---|---|
| Plugin structure | <https://plugins.jetbrains.com/docs/intellij/plugin-structure.html> |
| Plugin content | <https://plugins.jetbrains.com/docs/intellij/plugin-content.html> |
| Plugin actions | <https://plugins.jetbrains.com/docs/intellij/plugin-actions.html> |
| Plugin extensions | <https://plugins.jetbrains.com/docs/intellij/plugin-extensions.html> |
| Plugin services | <https://plugins.jetbrains.com/docs/intellij/plugin-services.html> |
| Plugin configuration file | <https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html> |
| Plugin dependencies | <https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html> |
| Plugin user experience | <https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html> |
| Tests and fixtures | <https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html> |
| Light and heavy tests | <https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html> |
| Test project & testdata dirs | <https://plugins.jetbrains.com/docs/intellij/test-project-and-testdata-directories.html> |
| Testing FAQ | <https://plugins.jetbrains.com/docs/intellij/testing-faq.html> |
| Gitea Swagger v1 spec | <https://gitea.com/swagger.v1.json> |
| IntelliJ Platform Plugin Template | <https://github.com/JetBrains/intellij-platform-plugin-template> |
