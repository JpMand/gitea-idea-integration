# Fix DTO migration & rebuild read-only PR review UI

## Context

This plugin provides Gitea Pull Request code review integration for IntelliJ IDEA, modeled on the official bundled GitHub/GitLab plugins (built on `intellij.platform.collaborationTools`). Work was previously done in a Claude Desktop session that isn't available to this conversation, and the repo was left in a broken, half-finished state on branch `feature/pr-code-review`.

**What happened:** the last real commit (`325cfb8`) contained a fairly complete PR review feature (tool window, PR list with filters, PR detail tab, diff viewer with inline comment display, review-submission UI — 22 files). On top of that, the working tree currently has:
- Those 22 PR-review files **staged for deletion**.
- ~200 new files added under `api/rest/dto/` — a generated port of the Gitea Swagger v2 spec (broad coverage: PRs, issues, releases, actions, orgs, etc.), replacing an older 44-file hand-rolled DTO layer at `api/rest/models/**`.
- Only 4 files wired to the new DTOs (`api/models/{GiteaBranchInfo,GiteaLabel,GiteaPullRequest,GiteaReview}.kt`), and they call `fromDto` factories (`GiteaUser.fromDto`, `GiteaReviewState.fromDto`) that don't exist. **Verified directly**: `GiteaPullRequest.fromDto` (api/models/GiteaPullRequest.kt:53,55,56) assigns `PullRequest.kt`'s `kotlinx.datetime.LocalDateTime?` fields straight into `java.util.Date`/`Date?` — a real type error, not just a missing-method issue. The REST call functions (e.g. `GiteaPullRequestApi.kt`) still return the *old* DTOs, so the new DTOs aren't reachable from any live API call yet.
- A generator defect confirmed by direct read: `api/rest/dto/MergePullRequestOption.kt` line 31 contains literal HTML-entity text `` &#x60; Do&#x60; `` instead of backticks around the nested enum name — a hard syntax error.

**Decisions made with the user, driving this plan:**
1. Finish the DTO migration properly rather than reverting — fully replace `api/rest/models/**` with `api/rest/dto/**` across the whole REST layer (not just PR-scoped).
2. First milestone = **read-only**: PR list w/ filters+search, PR detail tab (title/description/status/labels/commits/CI status), diff viewer with existing review comments shown inline. Merge/close/reopen and comment/review *submission* are an explicit, separate Milestone 2 — not built now.
3. Rebuild by restoring the deleted files via `git show HEAD:<path>` as a strong starting point, adapting to the fixed domain models and trimming out Milestone-2 mutation logic, rather than writing from scratch.
4. `api/rest/pr/GitePullRequestReviewsApi.kt` (deleted) is dead/duplicate code (redundant with `GiteaPullRequestApi.kt`, non-idiomatic naming, one function doesn't even compile) — leave it deleted, do not restore.

Goal of this pass: get the project compiling again on the new DTO foundation, then have a working read-only PR review experience end-to-end in a sandbox IDE against a local Gitea instance.

**Version support requirement (refined by user):** target **Gitea 1.27.3** with compatibility down to **1.27.1**, and IntelliJ IDEA **2026.2.1** as the floor — kept as a **bounded range**, not open-ended, since the plugin leans on `@Suppress("UnstableApiUsage")` collaboration-tools APIs that can change shape between platform releases. Confirmed via `docs.gitea.com/api`'s version selector: 1.27.3 is the current patch of the 1.27 line (others documented: 1.28-dev, 1.26.4, 1.25.5, ...), with no API-surface changes noted between 1.27.1–1.27.3 (patch releases; only an unrelated deprecation notice about legacy `AccessToken`/`Token` auth in favor of `AuthorizationHeaderToken`, which this plugin already uses). Confirmed via web search: IDEA 2026.2 uses platform branch **262** (e.g. build 262.8665.337 for 2026.2.0.1), vs. the current `gradle.properties` floor of branch 261 (2026.1.1) — a real floor raise. The codebase already has an (unenforced) `earliestSupportedVersion = GiteaVersion(1, 27, 0)` in `GiteaServersManager.kt:38`.

## Phase 0 — Raise version floors

**0a. IntelliJ IDEA — floor 2026.2.1, bounded range** (`gradle.properties`):
- `pluginSinceBuild = 262` (was `261`).
- `pluginUntilBuild = 262.*` — deliberately **bounded**, not open-ended, per user's steer: the plugin relies heavily on `@Suppress("UnstableApiUsage")` `com.intellij.collaboration.*` APIs, which have no compatibility guarantee across platform releases. Capping `untilBuild` to the tested major branch (262.x = 2026.2.x) means the plugin fails closed (marketplace won't offer it, `runIde` won't silently misbehave) on an untested future platform version instead of failing open with a possibly-broken unstable API call. Widen this range deliberately in a future pass once each new platform version has actually been verified against it.
- `platformVersion = 2026.2.1` (was `2026.1.1`) — resolved/downloaded by the Gradle IntelliJ Platform plugin to compile and run `runIde`/`runIdeForUiTests` against.
- After bumping, do a full rebuild and re-scan every `@Suppress("UnstableApiUsage")` call site — compile errors here are expected to surface real signature changes, not just noise to suppress again.
- Update `build.gradle.kts`'s `pluginVerification { ides { recommended() } }` section to explicitly pin 2026.2.1 so the Plugin Verifier checks the actual floor, not just whatever `recommended()` resolves to.
- Update the stale version references already noted in `README.md` and `AGENTS.md` (both currently say 2025.3.1) to reflect the new 2026.2.1 floor / 262.* ceiling.

**0b. Gitea — target 1.27.3, compatible down to 1.27.1** (`GiteaServersManager.kt`):
- Bump `earliestSupportedVersion` from `GiteaVersion(1, 27, 0)` to `GiteaVersion(1, 27, 1)` (line 38) — this is the floor. 1.27.3 is the version to actually develop/test against (current patch of the 1.27 line per `docs.gitea.com/api`'s version selector); no code needs a distinct "ceiling" constant since newer patch releases of the same minor line are expected to stay API-compatible (confirmed no notable API changes 1.27.1→1.27.3).
- **Actually wire enforcement**, since none exists today: in the login flow (`GiteLoginUtil.kt`'s `logInViaToken`/`updateToken`, after `checkIsGiteaServer()` succeeds and before returning `LoginResult.Success`), fetch the server's `GiteaServerMetadata` via `CachingGiteaServersManager.getMetadata(api)` and compare against `earliestSupportedVersion` (verify/add `Comparable<GiteaVersion>` on `GiteaVersion` if not already present). If the connected server is older than 1.27.1, surface a clear error through the existing `LoginResult.Failure`/`GiteaLoginErrorStatusPresenter` path (e.g. "Gitea 1.27.1 or later is required, server reports 1.26.x") rather than silently allowing a connection that may 404 on endpoints Phase A/B rely on.
- The Swagger spec in-repo (`gitea-swagger-v2-spec.json`, `info.version: "1.27.0+dev-651-gcb08549242"`) is one dev-snapshot short of 1.27.1 — treat it as accurate for this work given the confirmed absence of API changes across the 1.27.x line; if any Phase A/B endpoint call unexpectedly 404s or returns an unexpected shape against a real 1.27.1–1.27.3 test server, treat that as a signal to re-check the spec assumption rather than a random bug.

**0c. Additional platform-compatibility fixes discovered during implementation (not in original plan):**
- Bumping `platformVersion` to 2026.2.1 broke `com.intellij.dvcs.*`, `com.intellij.util.ui.cloneDialog.*`, and `com.intellij.openapi.vcs.changes.ui.ChangesGroupingSupport` — not because these APIs moved/renamed, but because IDEA 2026.2 modularized further and these classes are no longer implicitly on the compile classpath. Fixed by adding `intellij.platform.vcs.dvcs.impl`, `intellij.platform.vcs.dvcs.impl.shared`, `intellij.platform.vcs.impl`, `intellij.platform.vcs.impl.shared` to `platformBundledModules` in `gradle.properties`.
- The IntelliJ Platform Gradle Plugin `2.13.1` couldn't parse 2026.2.1's module descriptor XML format (`UnknownXmlFieldException` on `ModuleDescriptor.Dependency`'s `module`/`namespace` attribute) — bumped to `2.18.1` (latest as of this work) in `gradle/libs.versions.toml`.
- `jackson-datatype-jsr310` was NOT already on the platform classpath (contrary to the plan's assumption) — added explicitly as `implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")`.
- Platform 2026.2.1's bundled Kotlin stdlib requires JVM 25 bytecode — bumped `kotlin { jvmToolchain(21) }` to `25`.
- Result, verified directly (not assumed): `./gradlew compileKotlin` and `./gradlew test` both pass cleanly, 51/51 tests green, including two new tests (RFC3339 date round-trip via the now-registered `JavaTimeModule`, and a `pull_request_list.json` fixture test against the new `dto.PullRequest`).

## Phase A — Fix the DTO migration

Do these in order; the project will not fully compile until Phase A is complete (attempting to keep every intermediate step green isn't practical given the scale).

**A0. Date-type strategy (do first — touches every DTO).**
Replace `kotlinx.datetime.LocalDateTime` with `java.time.OffsetDateTime` across all `api/rest/dto/**/*.kt` files (mechanical import+type replacement; verify no other symbol collisions). Rationale: Gitea returns RFC3339 timestamps with an offset (not zone-less local time), `java.time` needs no new dependency, and `kotlinx.datetime` isn't currently declared anywhere in `build.gradle.kts`/`libs.versions.toml`. In `api/GiteaJsonDeSerializer.kt`'s `giteaJacksonMapper()`, register `JavaTimeModule` (`com.fasterxml.jackson.datatype.jsr310.JavaTimeModule`) — try it unmodified first; add `jackson-datatype-jsr310` to `build.gradle.kts` only if it fails to resolve (the platform very likely already bundles it alongside `jackson-databind`/`jackson-module-kotlin`). Also add `@JsonEnumDefaultValue` to `PullReview.State.PENDING` so an unrecognized review state degrades gracefully instead of throwing (mirrors the old enum's behavior).

**A1. Fix the `MergePullRequestOption.kt` syntax error** (api/rest/dto/MergePullRequestOption.kt:17,31) — rename the nested enum to a valid identifier (e.g. `Do`) and fix the field reference. After fixing, grep `api/rest/dto/` for `&#x60;` and confirm all remaining hits are inside KDoc comments only (not real code) — this is a generator defect, other files were spot-checked and are comment-only, but re-verify after edits since not all ~200 files were individually inspected.

**A2. Add missing `fromDto` factories in `api/models/**`:**
- `GiteaUser.kt` — widen `id: Int` → `Long` (no other consumer does numeric comparisons on it), add `fromDto(dto: api.rest.dto.User): GiteaUser`.
- `GiteaReviewState.kt` — add `fromDto(state: api.rest.dto.PullReview.State?): GiteaReviewState`. Note the new enum's constants are `REQUESTCHANGES`/`REQUESTREVIEW` (no underscore), not `REQUEST_CHANGES`/`REQUEST_REVIEW` — map carefully.
- `GiteaReviewComment.kt` — add `fromDto(dto: api.rest.dto.PullReviewComment): GiteaReviewComment`, mapping `position`/`originalPosition` → `newLine`/`oldLine` (existing convention) and converting `OffsetDateTime` → `Date` via a small shared `OffsetDateTime.toDate()` extension (add once, e.g. in a new small `api/models/DateConversions.kt`).

**A3. Fix the already-touched domain models to actually compile:**
- `GiteaPullRequest.kt` — change `updatedAt` from `kotlinx.datetime.LocalDateTime` to `java.util.Date` (consistency with `createdAt`/`mergedAt`/`closedAt`), convert every DTO date field via `?.toDate()` in `fromDto`. Check `additions`/`deletions`/`changedFiles`/`reviewComments` (now `Long?` on the DTO vs `Int`/`Int?` on the domain class) — prefer converting at the single UI call site in `GiteaPRListPanel` rather than widening the domain model, unless that call site requires otherwise.
- `GiteaReview.kt` — convert `submittedAt` to `Date` for consistency (currently no consumer depends on `LocalDateTime` specifically).
- `GiteaBranchInfo.kt`, `GiteaLabel.kt` — no changes needed beyond A0 (no date fields, already aligned to `PRBranchInfo`/`Label`).

**A4. Migrate the REST extension-function files off `api/rest/models/**` onto `api/rest/dto/**`:**
`api/rest/GiteaUsersApi.kt`, `GiteaContentApi.kt`, `GiteaRepositoryApi.kt`, `GiteaServerApi.kt`, `GiteaStatusApi.kt`, and `api/rest/pr/GiteaPullRequestApi.kt` (largest — 33 functions). For each, swap the old DTO type for its new `dto.*` equivalent and update `.toXxx()` call sites to the new `fromDto()` factories.
- `GiteaPullRequestApi.kt` specifics: the new `PullRequest.State` enum only has `OPEN`/`CLOSED` (no `ALL`) — for the "all" list filter, omit the `state` query param (or pass the raw string) rather than trying to force it into the enum. `GiteaPullRequestSortEnum` and `GiteaPRFileStatusEnum` have no equivalent in the new spec-generated DTOs (they were query-param/response-string-only) — relocate them out of the deleted `api/rest/models/pr` package into `api/rest/pr/` directly rather than deleting them.

**A5. Delete the old `api/rest/models/**` layer** (42 remaining files after relocating the two enums above) and any now-dead code (`GiteaPullRequestDTO.toPullRequest()`, etc.). Confirm with `grep -r "api.rest.models" src/main/kotlin` → zero hits, and `grep -r "kotlinx.datetime" src/main/kotlin` → zero hits.

## Phase B — Restore and adapt the read-only PR review UI

Restore each file via `git show HEAD:<path>` into its original location, then adapt per group below.

**B1. Restore near-verbatim** (only need Phase-A type fixes to compile):
`pullrequest/data/GiteaPRDataContext.kt`; `pullrequest/ui/list/{GiteaPRListViewModel,GiteaPRListPanel,GiteaPRListQuickFilter}.kt`; `pullrequest/ui/filters/{GiteaPRListSearchPanelFactory,GiteaPRListSearchPanelViewModel,GiteaPRListSearchValue}.kt`; `pullrequest/ui/details/{GiteaPRBranchesViewModel,GiteaPRChangesViewModel}.kt`; `pullrequest/review/{GiteaPRCommentViewModel,GiteaPRThreadViewModel,GiteaPRDiscussionsViewModels}.kt`. For `GiteaPRDiscussionsViewModels.kt`: restore as-is but do not wire `addDraftComment`/`removeDraftComment`/`resolveThread`/`unresolveThread` into any Milestone-1 UI — leave present but unused, cheaper than stripping now given Milestone 2 will need it back.

**B2. Restore with data-layer rewiring:**
`pullrequest/data/GiteaPRRepository.kt` — restore, then repoint every public method's DTO types to the Phase-A equivalents (`EditPullRequestOption`, `MergePullRequestOption`, `CreatePullReviewOptions`, `dto.Commit`, `dto.CombinedStatus`, etc.), and change `loadPullRequests`'s state filter to accept the "all" case (raw string or nullable, since the new enum lacks `ALL`). Keep PR-number parameters as `Int` at this REST boundary (they're URL path segments) — convert from the domain model's `Long` at call sites (`GiteaPRDetailsViewModel`, `GiteaPRDiffViewModel`), a small number of `.toInt()` additions.

**B3. Restore with Milestone-1 scope trimming** (strip mutation logic, keep read-only display):
- `pullrequest/ui/details/GiteaPRDetailsViewModel.kt` / `GiteaPRDetailsPanel.kt` — remove `merge()`/`close()`/`reopen()` and any toolbar wiring to them; keep title/description/status/branches/commits/CI display as-is.
- `pullrequest/diff/{GiteaPRChangedFile,GiteaPRDiffFileViewModel,GiteaPRDiffViewModel,GiteaPRDiffVirtualFile,GiteaPRDiffExtension}.kt` — restore essentially as-is (already read-only); fix `GiteaPRFileStatusEnum` import path per A4, note `ChangedFile.status` is now a raw `String?` not a typed enum on the new DTO.
- `pullrequest/editor/{GiteaPRDiffEditorModel,GiteaPRInlayModel,GiteaPRInlayComponentsFactory}.kt` — restore, then trim: keep only the `Thread` (existing-comment) inlay variant, drop `NewComment`/`DraftComment` variants and the Resolve/Unresolve button in the thread panel.
- `pullrequest/ui/toolwindow/{GiteaPRToolWindowController,GiteaPRToolWindowFactory}.kt` — restore, then strip the `GiteaPRReviewViewModel`/`GiteaPRSubmitReviewPopupHandler`/`onSubmitReview` wiring and the `draftCommentsCount` plumbing into the details panel constructor. Keep list↔details↔diff navigation and `discussionsVm` construction (still needed for read-only inline comments).

**B4. Not restored:** `pullrequest/review/{GiteaPRNewCommentViewModel,GiteaPRDraftComment,GiteaPRReviewViewModel,GiteaPRSubmitReviewPopupHandler}.kt` and `api/rest/pr/GitePullRequestReviewsApi.kt` — Milestone 2, confirmed with user to leave deleted for now.

**B5. `plugin.xml`** — no edits needed; it already references `pullrequest.diff.GiteaPRDiffExtension` and `pullrequest.ui.toolwindow.GiteaPRToolWindowFactory`, which resolve automatically once B3 restores those files at their original package paths.

## Phase C — UI Robot test infrastructure — DONE, verified against a live sandbox

Implemented and validated end-to-end (not just written blind): launched the actual `runIdeForUiTests` sandbox, ran `./gradlew uiTest` against it, and iterated on real failures until both tests passed. Notable things found only by actually running it:
- `remote-robot`/`remote-fixtures` (0.11.23) are hosted on `https://packages.jetbrains.team/maven/p/ij/intellij-dependencies`, not Maven Central — added that repo.
- The `uiTest` source set needs the Kotlin stdlib added explicitly (`kotlin("stdlib")`) since `kotlin.stdlib.default.dependency = false` only gets satisfied for main/test via the IntelliJ Platform dependency.
- Gradle Kotlin DSL type-safe accessors (`uiTestImplementation(...)`) aren't available for a source set created in the same script — used the `"uiTestImplementation"(...)` string-invoke form instead.
- remote-robot's bundled Gson hits `InaccessibleObjectException` on modern JDKs without `--add-opens=java.base/java.lang=ALL-UNNAMED` and `.../java.util=ALL-UNNAMED` on the `uiTest` task.
- `com.intellij.projectConfigurable` is a **project-scoped** extension point — it doesn't exist in the Application container at all without an open project (confirmed via a real `IdeaSideException`, not assumed), so `GiteaSettingsConfigurable`'s registration isn't checked by the automated UI test; it's covered by the manual verification pass instead. `com.intellij.toolWindow` and `com.intellij.diff.DiffExtension` are Application-scoped and are checked automatically.
- `com.intellij.toolWindow` extensions are `ToolWindowEP` beans with a `factoryClass` string field (lazy), not instantiated factory objects — checked that field directly rather than the extension object's own class.

Final result: `GiteaUiTest` (2 tests) passes against a live sandbox; `./gradlew compileKotlin`/`test` still green (52/52) afterward. Documented the two-terminal workflow in `AGENTS.md`.

`build.gradle.kts` already scaffolds the *server* side (`intellijPlatformTesting.runIde.register("runIdeForUiTests")` launches a sandbox IDE with `robotServerPlugin()` listening on `robot-server.port=8082`), but there is no *client* side anywhere in the repo (confirmed: no `remote-robot` dependency, no `uiTest` source set, no test files under any UI-test naming pattern). This phase adds that missing half so `runIdeForUiTests` is actually exercised by real tests, not just launchable.

**C1. Add a dedicated `uiTest` source set and dependencies** in `build.gradle.kts`:
- Register a `uiTest` source set (`sourceSets { create("uiTest") { ... } }`) that extends `test`'s compile/runtime classpath, plus a `uiTest` task of type `Test` wired to it (standard Gradle pattern for a secondary test source set — keeps UI tests out of the normal fast `test` task).
- Add dependencies (scoped to `uiTest`): `com.intellij.remoterobot:remote-robot` and `com.intellij.remoterobot:remote-fixtures` (JetBrains' official UI-test client library, matches the already-present `robotServerPlugin()` server), plus reuse the existing `libs.junit` for the test runner.
- Wire `uiTest` to depend on `runIdeForUiTests` being up before it runs (or document running them as two manual steps — launch sandbox, then run `uiTest` — whichever fits this Gradle IntelliJ Platform plugin version's supported task wiring; check its docs for a built-in "wait for robot server" helper before hand-rolling one).

**C2. Write smoke-level UI tests** under `src/uiTest/kotlin/.../GiteaUiTest.kt` (or similar), connecting via `RemoteRobot("http://127.0.0.1:8082")`. Scope to what's verifiable without a live Gitea server dependency in CI:
- IDE launches and the welcome/project screen is reachable.
- Settings > Version Control > Gitea panel opens and renders (exercises `GiteaSettingsConfigurable` registration).
- The "Gitea Pull Requests" tool window registers and opens, showing the expected empty/"please log in" state when no account is configured (exercises `GiteaPRToolWindowFactory` registration from Phase B without needing a live server).
- If a local Gitea docker instance is reliably available in the dev/CI environment, add one further test that logs in and confirms the PR list tool window populates — otherwise leave this as a documented manual step (Verification section below already covers it) rather than a flaky CI-dependent test.

**C3. Document how to run them** (in `README.md` or `AGENTS.md`, wherever build/test instructions already live): `./gradlew runIdeForUiTests` in one terminal, `./gradlew uiTest` in another (or however C1's task wiring ends up working) — keep this consistent with whatever the IntelliJ Platform Gradle Plugin version in use actually supports out of the box.

## RESOLVED: Jackson version conflict (was blocking MyPluginTest)

Root cause confirmed via `mcp__intellij__get_project_dependencies`: two different `jackson-annotations` jars were on the classpath at once (`jackson-annotations-2.19.0.jar` from our `jackson-datatype-jsr310:2.19.0` dependency's own transitive Jackson, and `jackson-annotations-2.21.jar` from the platform's own bundled Jackson used by `com.jetbrains.remoteDevelopment` and friends). Fixed by excluding the jsr310 artifact's transitive `com.fasterxml.jackson.core` group in `build.gradle.kts`, so it binds purely to the platform's own already-present Jackson classes at runtime:
```kotlin
implementation(libs.jacksonDatatypeJsr310) {
    exclude(group = "com.fasterxml.jackson.core")
}
```
Verified: all 52/52 tests pass now, including `MyPluginTest`.

## Verification

**Compile:**
- `./gradlew compileKotlin` then `./gradlew build`.
- `grep -r "api.rest.models" src/main/kotlin` and `grep -r "kotlinx.datetime" src/main/kotlin` → both zero hits.
- `grep -r "&#x60;" src/main/kotlin/.../api/rest/dto` → remaining hits confined to KDoc comments only.

**Existing tests** (`GiteaServerPathTest`, JSON deserialization tests, `GitePersistentAccountsTest`) should still pass unmodified — run as regression check. Add one new JSON deserialization test that parses a literal RFC3339 timestamp into a DTO with a date field, asserting `OffsetDateTime` parses correctly post-`JavaTimeModule` registration (this is the one risk that compiles fine but could silently break at runtime). Also add a test loading the existing unused fixture `src/test/testData/pull_request_list.json` through `GiteaJsonDeSerializer` into `PullRequest`/a list of them, asserting correct shape — validates the new DTOs against a real Gitea API sample.

**Version floor checks:**
- `./gradlew runIde` should launch IDEA 2026.2.1 (confirm via Help > About in the sandbox) — if the Gradle IntelliJ Platform plugin can't resolve that exact version, fall back to the nearest available 2026.2.x and note the discrepancy.
- Point the plugin at a Gitea 1.27.1 test instance to confirm the version-gate logic in 0b passes at exactly the floor, a 1.27.3 instance to confirm the actively-developed-against version works end-to-end, and (if available) something older (e.g. 1.26.x) to confirm the login flow correctly rejects/warns instead of silently proceeding.
- `./gradlew runIdeForUiTests` + `./gradlew uiTest` (Phase C) pass.

**Manual end-to-end** (`./gradlew runIde` against a local Gitea docker instance with a sample repo/PR):
1. Settings > Version Control > Gitea — add an account against the local instance; confirm login/avatar show correctly (exercises `GiteaUsersApi`/`GiteaUser.fromDto`).
2. Open/clone a repo backed by that Gitea instance.
3. Open the "Gitea Pull Requests" tool window — confirm empty/login state, then PR list loads with title/author/labels/state badges; confirm free-text search and Open/Closed/All quick filters work (including the "all" case).
4. Open a PR — confirm details tab shows title/description/status/branches/commits/CI status, with no merge/close/reopen/comment controls visible.
5. Open the diff view — confirm changed files list (mix of added/modified/deleted/renamed statuses), and confirm pre-existing review comments render as read-only inline bubbles at the correct line.
6. Trigger refresh on both the list and an open PR tab; confirm no exceptions.

## Critical files
- `gradle.properties`, `build.gradle.kts` (pluginVerification block + new uiTest source set) — Phase 0 and Phase C.
- `GiteaServersManager.kt`, `GiteLoginUtil.kt`, `api/GiteaVersion.kt` — Phase 0 (Gitea version gate).
- `api/GiteaJsonDeSerializer.kt`, `api/rest/dto/MergePullRequestOption.kt`, `api/models/{GiteaPullRequest,GiteaUser,GiteaReviewState,GiteaReviewComment}.kt`, `api/rest/pr/GiteaPullRequestApi.kt` — Phase A core.
- `pullrequest/data/GiteaPRRepository.kt`, `pullrequest/ui/toolwindow/GiteaPRToolWindowController.kt` — Phase B core, restored via `git show HEAD:<path>`.
- `src/uiTest/kotlin/.../GiteaUiTest.kt` (new) — Phase C.
