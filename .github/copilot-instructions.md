# GitHub Copilot Instructions

## Project Identity
Kotlin/IntelliJ Platform plugin that integrates Gitea into JetBrains IDEs.
Generated from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
Base package: `com.github.jpmand.idea.plugin.gitea`  
Target platform: IntelliJ IDEA 2025.3.1 (`sinceBuild = 253`), JVM 21, Kotlin 2.x.  
Reference implementations to follow: [GitHub](https://github.com/JetBrains/intellij-community/tree/master/plugins/github) and [GitLab](https://github.com/JetBrains/intellij-community/tree/master/plugins/gitlab) plugins in [intellij-community](https://github.com/JetBrains/intellij-community) repository.

### Gitea API Contract
All REST endpoint paths, HTTP methods, request parameters, and response JSON shapes are defined in the **Gitea Swagger v1 specification**: <https://gitea.com/swagger.v1.json>  
Consult this spec before adding any new `suspend` extension function or DTO field.

---

## Plugin Template Scaffold
This project was bootstrapped from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/README.md). Key scaffold conventions:

- **Version coordinates** (`platformVersion`, `pluginVersion`, `sinceBuild`, `platformBundledPlugins`, `platformBundledModules`) all live in `gradle.properties`, **not** `build.gradle.kts`.
- **Plugin description** is extracted automatically from the `<!-- Plugin description --> … <!-- Plugin description end -->` block in `README.md`.
- **Changelog**: `CHANGELOG.md` (Keep a Changelog format) drives `changeNotes` via the Gradle Changelog Plugin.
- **CI workflows** (`.github/workflows/`):
  - `build.yml` — validate, test, Qodana, `buildPlugin`, `runPluginVerifier`, draft release.
  - `release.yml` — publishes to JetBrains Marketplace on tag push (`PUBLISH_TOKEN` secret required).
  - `run-ui-tests.yml` — robot-server UI tests (port 8082).
- **Signing** uses `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` environment/GitHub secrets.
- **Dependabot** (`.github/dependabot.yml`) keeps Gradle and Actions dependencies current.

---

## Architecture Layers

| Layer | Package suffix | Purpose |
|---|---|---|
| API client | `api/` | HTTP calls, JSON, server path |
| REST DTOs | `api/rest/models/` | Raw Gitea API response shapes |
| Domain models | `api/models/` | Clean objects used outside `api/` |
| REST wrappers | `api/rest/` | `suspend` extension funs on `GiteaApi` |
| Auth | `authentication/` | Account storage, login flows, Git auth providers |
| UI | `ui/` | Settings configurable, clone panel |
| Util | `util/` | Bundle, logging, shared helpers |

### Data flow
```
HTTP response → GiteaUserDTO (api/rest/models/)
                    ↓ .toUser()
              GiteaUser (api/models/)
                    ↓ consumed by UI / services
```
Never pass raw DTOs outside `api/`.

---

## Non-Negotiable Rules

### 1. `plugin.xml` registration
Every new `applicationService`, `projectService`, extension, or action **must** be added to
`src/main/resources/META-INF/plugin.xml`. Omitting it causes silent failures — the component
simply won't load.  
Docs: [Plugin Configuration File](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html) · [Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html) · [Extensions](https://plugins.jetbrains.com/docs/intellij/plugin-extensions.html) · [Actions](https://plugins.jetbrains.com/docs/intellij/plugin-actions.html)

### 2. `@Suppress("UnstableApiUsage")`
`intellij.platform.collaborationTools` is internal API. Any file that imports from:
- `com.intellij.collaboration.api.*`
- `com.intellij.collaboration.auth.*`
- `git4idea.remote.hosting.*`

…requires `@Suppress("UnstableApiUsage")` at class or function level, not just on the import.

### 3. DTO / domain split
- Add REST response fields to a class in `api/rest/models/` (suffix `DTO`).
- Field names and types come directly from the [Gitea Swagger spec](https://gitea.com/swagger.v1.json).
- Add a `.toXxx()` conversion method on the DTO that produces the domain object.
- Domain objects live in `api/models/` (no `DTO` suffix).
- Example: `GiteaUserDTO.toUser()` → `GiteaUser`.

### 4. JSON field mapping
`GiteaJsonDeSerializer` uses Jackson `SNAKE_CASE` property naming and `ANY` field visibility
(reads constructor parameters directly). Rules:
- Name DTO constructor parameters in **camelCase** — Jackson maps `avatar_url` → `avatarUrl` automatically.
- Do **not** add `@JsonProperty` for standard snake_case Gitea API fields.
- Unknown JSON properties are silently ignored (`FAIL_ON_UNKNOWN_PROPERTIES = false`).
- Unknown enum values fall back to the default value (`READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE = true`).

### 5. i18n — no raw strings in UI
All user-visible strings must live in `src/main/resources/messages/GiteaBundle.properties`.
Access them with:
```kotlin
GiteaBundle.message("my.key")            // eager
GiteaBundle.messagePointer("my.key")     // lazy (for action text, etc.)
```
Docs: [Plugin User Experience](https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html)

### 6. Threading model
Git4Idea `GitHttpAuthDataProvider` callbacks arrive on a **background thread**.
- Bridge to coroutines: `runBlockingMaybeCancellable { … }`
- Switch to EDT for UI: `withContext(Dispatchers.EDT + ModalityState.any().asContextElement())`
- Annotate background-thread entry points with `@RequiresBackgroundThread`.
- Annotate EDT entry points with `@RequiresEdt`.

---

## Adding a New REST API Call

1. Open the [Gitea Swagger spec](https://gitea.com/swagger.v1.json) and find the endpoint — note the path, HTTP method, and response schema.
2. Create (or extend) the DTO in `api/rest/models/` using the schema's field names mapped to camelCase constructor parameters.
3. Create (or reuse) a `suspend` extension function on `GiteaApi` in `api/rest/`:
```kotlin
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.listOrgRepos(org: String): List<GiteaRepositoryDTO> {
    val uri = server.restApiUri().resolveRelative("orgs/$org/repos")
    val request = request(uri).GET().build()
    return rest.loadJsonValue<List<GiteaRepositoryDTO>>(request).body()
}
```
4. If callers outside `api/` need it, add a `.toXxx()` method and a domain class in `api/models/`.

---

## Platform Services

| What you need | How to get it |
|---|---|
| Account list / tokens | `service<GiteaAccountManager>()` |
| API client for an account | `service<GiteaApiManager>().getClient(account.server, token)` |
| Unauthenticated client | `service<GiteaApiManager>().getUnauthenticatedClient(server)` |
| Known Git repositories | `project.service<GiteaRepositoriesManager>().knownRepositoriesState` |
| Project default account | `project.service<GiteaProjectDefaultAccountHolder>()` |

Services declared in `plugin.xml` use interface/implementation pairs —
always inject the **interface** via `service<GiteaAccountManager>()`, not the impl class.  
Docs: [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html) · [Plugin Dependencies](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html)

---

## Testing

- **Pure unit tests** (JSON, server path parsing): no base class, plain JUnit 4.
  ```kotlin
  @Test fun `parses avatar_url`() {
      val dto = GiteaJsonDeSerializer.fromJson(StringReader(json), GiteaUserDTO::class.java)
      assertEquals("https://…", dto!!.avatarUrl)
  }
  ```
- **Platform integration tests**: extend nothing, but declare `testFramework(TestFrameworkType.Platform)` in `build.gradle.kts` (already present).
- Test fixtures (JSON payloads): `src/test/testData/`. Use real Gitea API response examples from the [Swagger spec](https://gitea.com/swagger.v1.json).
- Docs: [Tests and Fixtures](https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html) · [Light and Heavy Tests](https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html) · [Test Project and Testdata Directories](https://plugins.jetbrains.com/docs/intellij/test-project-and-testdata-directories.html) · [Testing FAQ](https://plugins.jetbrains.com/docs/intellij/testing-faq.html)

---

## Build & Run

```bash
./gradlew build              # compile + jar
./gradlew test               # unit tests
./gradlew runIde             # IDE sandbox
./gradlew runIdeForUiTests   # sandbox with robot-server on port 8082
```
Version coordinates (`platformVersion`, `pluginVersion`, `sinceBuild`) are in **`gradle.properties`**, not `build.gradle.kts`.

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
