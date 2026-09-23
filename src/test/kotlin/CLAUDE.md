# Unit tests (`src/test/kotlin`)

- Tests extend **no base class** for pure unit tests (`GiteaJsonGiteaUserTest`, `GiteaServerPathTest`).
- JSON tests call `GiteaJsonDeSerializer.fromJson(StringReader(json), Dto::class.java)` directly.
- Integration tests that need the platform use JUnit4 + IntelliJ `TestFrameworkType.Platform` (declared in `build.gradle.kts`).
- Test data fixtures (JSON payloads) live in `src/test/testData/` (e.g., `pull_request_list.json`). Use real Gitea API responses (shapes in `gitea-swagger-v2-spec.json`) as fixtures.

- `GiteaPluginSmokeTest` (`BasePlatformTestCase`) loads the plugin in a headless IDE and resolves
  its registered services — the one heavy test; keep it fast and cheap.

Docs: [Tests and Fixtures](https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html), [Light and Heavy Tests](https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html), [Test Project and Testdata Directories](https://plugins.jetbrains.com/docs/intellij/test-project-and-testdata-directories.html), [Testing FAQ](https://plugins.jetbrains.com/docs/intellij/testing-faq.html).

There is no automated UI / integration test suite (a Starter/Driver attempt was removed as
unbuildable against the current toolchain). Verify UI changes manually via `runIde`.
