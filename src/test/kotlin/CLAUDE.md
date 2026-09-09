# Unit tests (`src/test/kotlin`)

- Tests extend **no base class** for pure unit tests (`GiteaJsonGiteaUserTest`, `GiteaServerPathTest`).
- JSON tests call `GiteaJsonDeSerializer.fromJson(StringReader(json), Dto::class.java)` directly.
- Integration tests that need the platform use JUnit4 + IntelliJ `TestFrameworkType.Platform` (declared in `build.gradle.kts`).
- Test data fixtures (JSON payloads) live in `src/test/testData/` (e.g., `pull_request_list.json`). Use real Gitea API responses from the [Swagger spec](https://gitea.com/swagger.v1.json) as fixtures.

Docs: [Tests and Fixtures](https://plugins.jetbrains.com/docs/intellij/tests-and-fixtures.html), [Light and Heavy Tests](https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html), [Test Project and Testdata Directories](https://plugins.jetbrains.com/docs/intellij/test-project-and-testdata-directories.html), [Testing FAQ](https://plugins.jetbrains.com/docs/intellij/testing-faq.html).

UI integration tests (Starter/Driver) live in `src/integrationTest/kotlin` and have their own `CLAUDE.md`.
