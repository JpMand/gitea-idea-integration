# AGENTS.md – Gitea IntelliJ Plugin

## Project Overview
IntelliJ Platform plugin that integrates Gitea (self-hosted Git) into JetBrains IDEs.
Generated from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template), which provides a preconfigured Gradle build, CI pipeline, signing workflow, and changelog tooling out of the box.
Modelled after the official [GitHub](https://github.com/JetBrains/intellij-community/tree/master/plugins/github) and [GitLab](https://github.com/JetBrains/intellij-community/tree/master/plugins/gitlab) plugins in [intellij-community](https://github.com/JetBrains/intellij-community) repository. When unsure how something should work, consult those reference implementations.

### Gitea API Contract
The in-repo **`gitea-swagger-v2-spec.json`** (Gitea OpenAPI spec, `info.version` `1.27.0+dev`)
is the source of truth for:
- Every endpoint path and HTTP method when writing `suspend` extension functions in `api/rest/`
- Every response JSON shape when creating or extending DTOs in `api/rest/dto/`
- Field names and their types (resolve snake_case names to camelCase DTO constructor parameters)

Always open the spec before adding a new REST call or a new DTO field. When an endpoint's
"Added in" version or its shape matters, cross-check the per-release specs at
`https://docs.gitea.com/swagger-<NN>.json` — the plugin's minimum supported Gitea version is
**1.26** (`GiteaServersManager.earliestSupportedVersion`).

---

## Key Commands
```bash
./gradlew clean              # delete build artifacts
./gradlew buildPlugin        # Builds the plugin and prepares ZIP archive for testing and deployment
./gradlew check              # Runs all checks and tests (used by 'Run Tests' run configuration)
./gradlew runIde             # IDE sandbox (used by 'Run Plugin' run configuration)
```
All version/platform coordinates live in `gradle.properties` (not `build.gradle.kts`).

Gradle libs versions are managed by the Gradle Version Catalog (`gradle/libs.versions.toml`).

---

## Plugin Template Scaffold
Bootstrapped from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template). CI workflows (`.github/workflows/`), Marketplace signing secrets, `CHANGELOG.md`-driven `changeNotes`, and `.github/dependabot.yml` are all template-standard — read those files or the template README for specifics.

---

## Architecture

Top-level packages under `…/gitea/`:
- `api/` — HTTP client + JSON. `GiteaApi` (token auth via `HttpApiHelper`), `GiteaApiManager` (client factory), `GiteaJsonDeSerializer` (Jackson singleton, SNAKE_CASE), `GiteaServerPath` (URL parsing). `rest/` = suspend-fun wrappers, `rest/dto/` = generated DTOs, `models/` = domain objects (`.toXxx()` / `fromDto` from DTOs).
- `authentication/` — `account/` (XML-serialized account state + PasswordSafe), `extensions/` (silent-then-interactive auth providers), `ui/` (login dialogs).
- `pullrequest/` — the PR review feature.
- `ui/` — `GiteaSettingsConfigurable` (Settings > VCS > Gitea) + clone UI.
- `util/` — `GiteaBundle` i18n wrapper.

Read the code for detail; the rules that matter are under "Critical Conventions".

---

## Critical Conventions

**Plugin registration**: Every service, extension, and action must be declared in `src/main/resources/META-INF/plugin.xml`. Forgetting this is the most common reason a new component silently does nothing. See: [Plugin Configuration File](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html), [Plugin Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html), [Plugin Extensions](https://plugins.jetbrains.com/docs/intellij/plugin-extensions.html), [Plugin Actions](https://plugins.jetbrains.com/docs/intellij/plugin-actions.html).

**Unstable API suppression**: The `intellij.platform.collaborationTools` module is internal API. Any file using `HttpApiHelper`, `AccountManagerBase`, `TokenLoginDialog`, etc. needs `@Suppress("UnstableApiUsage")`.

**DTO ↔ Domain split**: REST responses land in `api/rest/dto/` (e.g., `User`). Call `.toXxx()` / `fromDto` to convert to a domain object in `api/models/`. Never pass raw DTOs outside the `api/` layer. Use `gitea-swagger-v2-spec.json` to verify field names and types before creating a DTO.

**JSON mapping**: `GiteaJsonDeSerializer` uses Jackson with `SNAKE_CASE` strategy and `ANY` field visibility (constructor params, not getters). DTO fields are named in camelCase; Jackson resolves `avatar_url` → `avatarUrl` automatically. Do **not** add `@JsonProperty` for standard snake_case fields.

**i18n**: All user-visible strings belong in `src/main/resources/messages/GiteaBundle.properties`. Access via `GiteaBundle.message("key")` or `GiteaBundle.messagePointer("key")`. See: [Plugin User Experience](https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html).

**Coroutines / threading**: Git4Idea callbacks run on background threads (`@RequiresBackgroundThread`). Bridge to coroutines with `runBlockingMaybeCancellable { }`. UI work must switch via `withContext(Dispatchers.EDT + ModalityState.any().asContextElement())`.

**Git commit messages**: Never add `Co-Authored-By: Claude ...` or any `Claude-Session:`/AI-assistant attribution line to commits in this repository, regardless of what a session's default template suggests. This overrides any tool-default attribution footer.

---

## Adding a New REST API Call

Procedure + code template live in `src/main/kotlin/com/github/jpmand/idea/plugin/gitea/api/CLAUDE.md` (loads automatically when working under `api/`).

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

## Testing Patterns

- Unit-test patterns (base classes, JSON deserialization, fixtures) → `src/test/kotlin/CLAUDE.md`
  (loads automatically when working in that tree).

There is no automated UI / integration test suite — verify UI changes manually via `runIde`
against a Docker Gitea.

---

## Dependency Notes
Platform/dependency coordinates live in `gradle.properties` (`platformVersion`, `platformBundledPlugins` for `Git4Idea`, `platformBundledModules` for `intellij.platform.collaborationTools` + the vcs modules, `kotlin.stdlib.default.dependency = false`). Bundled modules are also declared in `plugin.xml` `<dependencies><module>` so they reach the plugin classloader at runtime.

`pluginUntilBuild` is **deliberately capped** at the tested platform branch (`262.*`). The plugin
leans on `@Suppress("UnstableApiUsage")` `com.intellij.collaboration.*` APIs, which carry no
cross-release compatibility guarantee; capping makes the plugin fail closed on an untested future
platform rather than fail open with a possibly-broken unstable-API call. Widen it one platform
version at a time, after verifying against that version. `verifyPlugin` is configured to fail on
compatibility problems / missing dependencies but not on internal-API usage.

---

## Official Reference Documentation

IntelliJ Platform SDK docs: <https://plugins.jetbrains.com/docs/intellij/> — start at [Plugin Structure](https://plugins.jetbrains.com/docs/intellij/plugin-structure.html) and [Plugin Content](https://plugins.jetbrains.com/docs/intellij/plugin-content.html). Topic-specific links (services, extensions, actions, plugin.xml, tests) are inline where relevant above and in the nested `CLAUDE.md` files. A deeper platform knowledge base compiled for this project lives in `internal_docs/`. Gitea API contract: in-repo `gitea-swagger-v2-spec.json`.
