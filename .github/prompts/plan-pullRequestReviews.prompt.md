# Plan: Pull Request Reviews Feature Implementation

The `pullrequest/` directory is empty and needs a complete implementation. Following the GitLab/GitHub plugin architecture, this feature adds: REST DTOs + API wrappers → domain models → a reactive project service → tool window with list/details/timeline/diff/review panels using `intellij.platform.collaborationTools` abstractions.

---

## Steps

### 1. Add review/comment/file DTOs

Add to [`api/rest/models/pr/`](../../src/main/kotlin/com/github/jpmand/idea/plugin/gitea/api/rest/models/pr):

- `GiteaPullRequestReviewDTO` — maps `/repos/{owner}/{repo}/pulls/{index}/reviews` response fields (`id`, `user`, `body`, `state` (APPROVED | REQUEST_CHANGES | COMMENT | PENDING), `submittedAt`, `stale`, `official`, `dismissed`, `commitId`)
- `GiteaPullRequestReviewCommentDTO` — inline diff comment on a review (`id`, `user`, `body`, `createdAt`, `updatedAt`, `path`, `diffHunk`, `originalPosition`, `position`, `commitId`, `originalCommitId`)
- `GiteaIssueCommentDTO` — timeline comment on the PR via `/repos/{owner}/{repo}/issues/{index}/comments` (`id`, `user`, `body`, `createdAt`, `updatedAt`)
- `GiteaPullRequestFileDTO` — changed file from `/repos/{owner}/{repo}/pulls/{index}/files` (`sha`, `filename`, `status` (added | modified | deleted | renamed), `additions`, `deletions`, `changes`, `rawUrl`, `contentsUrl`, `patch`)
- `GiteaCreatePullRequestReviewRequestDTO` — POST body for creating a review (`body`, `commitId`, `event`, `comments: List<GiteaCreatePullRequestReviewCommentDTO>`)
- `GiteaCreatePullRequestReviewCommentDTO` — inline comment within a review creation request (`path`, `body`, `newPosition`)

All DTO fields follow the project convention: camelCase constructor parameters, no `@JsonProperty` for standard snake_case Gitea API fields (Jackson `SNAKE_CASE` strategy handles the mapping automatically).

---

### 2. Add domain models

Add to [`api/models/`](../../src/main/kotlin/com/github/jpmand/idea/plugin/gitea/api/models):

- `GiteaPullRequest` — domain object converted from `GiteaPullRequestDTO` (carries `number`, `title`, `body`, `state`, `author: GiteaUser`, `head`/`base` branch names, `htmlUrl`, `createdAt`, `updatedAt`, `labels`, `assignees`, `requestedReviewers`, `reviewComments`, `merged`, `draft`)
- `GiteaPullRequestReview` — from `GiteaPullRequestReviewDTO` (`id`, `author: GiteaUser`, `body`, `state`, `submittedAt`, `dismissed`)
- `GiteaPullRequestComment` — unified from both `GiteaIssueCommentDTO` and `GiteaPullRequestReviewCommentDTO` (common: `id`, `author: GiteaUser`, `body`, `createdAt`, `updatedAt`; inline-only: `path`, `diffHunk`, `position`)
- `GiteaChangedFile` — from `GiteaPullRequestFileDTO` (`filename`, `status`, `additions`, `deletions`, `changes`, `patch`)

Add `.toXxx()` extension on each DTO class. Add `.toPullRequest()` to `GiteaPullRequestDTO`.

---

### 3. Add REST extension functions

Extend [`api/rest/GiteaPullRequestApi.kt`](../../src/main/kotlin/com/github/jpmand/idea/plugin/gitea/api/rest/GiteaPullRequestApi.kt) with new `suspend` functions on `GiteaApi`:

| Function | Endpoint |
|---|---|
| `repoGetPullRequest(owner, repo, index)` | `GET /repos/{owner}/{repo}/pulls/{index}` |
| `repoListPullRequestReviews(owner, repo, index)` | `GET /repos/{owner}/{repo}/pulls/{index}/reviews` |
| `repoGetPullRequestReview(owner, repo, index, id)` | `GET /repos/{owner}/{repo}/pulls/{index}/reviews/{id}` |
| `repoGetPullRequestReviewComments(owner, repo, index, id)` | `GET /repos/{owner}/{repo}/pulls/{index}/reviews/{id}/comments` |
| `repoListPullRequestComments(owner, repo, index, since, before, page, limit)` | `GET /repos/{owner}/{repo}/issues/{index}/comments` |
| `repoListPullRequestFiles(owner, repo, index, page, limit)` | `GET /repos/{owner}/{repo}/pulls/{index}/files` |
| `repoListPullRequestCommits(owner, repo, index, page, limit)` | `GET /repos/{owner}/{repo}/pulls/{index}/commits` |
| `repoCreatePullRequestReview(owner, repo, index, body)` | `POST /repos/{owner}/{repo}/pulls/{index}/reviews` |
| `repoCreatePullRequestComment(owner, repo, index, body)` | `POST /repos/{owner}/{repo}/issues/{index}/comments` |
| `repoEditPullRequestComment(owner, repo, commentId, body)` | `PATCH /repos/{owner}/{repo}/issues/comments/{id}` |
| `repoDeletePullRequestComment(owner, repo, commentId)` | `DELETE /repos/{owner}/{repo}/issues/comments/{id}` |
| `repoDeletePullRequestReview(owner, repo, index, id)` | `DELETE /repos/{owner}/{repo}/pulls/{index}/reviews/{id}` |
| `repoDismissPullRequestReview(owner, repo, index, id, message)` | `POST /repos/{owner}/{repo}/pulls/{index}/reviews/{id}/dismissals` |

All functions must carry `@Suppress("UnstableApiUsage")` and use `GiteaUriUtil.QueryBuilder` for optional parameters.

---

### 4. Create `GiteaPullRequestsProjectService`

Create in [`pullrequest/service/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/service):

**`GiteaPullRequestsProjectService`** (`@Service(Level.PROJECT)`, coroutine-scoped):
- Injects `GiteaRepositoriesManager`, `GiteaAccountManager`, `GiteaApiManager`
- Exposes `activeRepoMappingState: StateFlow<GiteaGitRepositoryMapping?>` (derived from `knownRepositoriesState` + IDE's current VCS root)
- Exposes `pullRequestsState: StateFlow<Result<List<GiteaPullRequest>>>` (loads via `repoListPullRequests`, re-triggered when mapping or filters change)
- Provides `refresh()` to force-reload the list
- Provides `getOrLoadApiForActiveRepo(): GiteaApi?` (resolves account + token for the active mapping)

**`GiteaPullRequestDataLoader`** (inner helper or separate class, scoped to a single PR number):
- Lazy-loads and caches per-PR data in `MutableStateFlow`s: `reviewsState`, `commentsState`, `filesState`, `commitsState`
- Exposes `reload()` for each sub-resource
- Created by the service on demand, keyed by `(owner, repo, index)`, discarded when the PR is deselected

> **Prerequisite note**: `GiteaGitRepositoryMapping` (referenced by `GiteaRepositoriesManager`) must be resolved before this step. If it is a stub or missing, create it to implement `HostedGitRepositoryMapping<GiteaServerPath, GiteaRepositoryPath>` (mirroring the GitLab/GitHub equivalent) and tie `GiteaRepositoryCoordinates` into it.

---

### 5. Implement the tool window

Create in [`pullrequest/ui/toolwindow/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/ui/toolwindow):

**`GiteaPRToolWindowFactory`** — implements `ToolWindowFactory` + `DumbAware`:
- Creates the tool window content inside a `CoroutineScope` tied to the content's `Disposable`
- Delegates to `GiteaPRToolWindowController`

**`GiteaPRToolWindowViewModel`** — project-level coroutine VM:
- Drives which panel is visible (`LoginPrompt | NoRepoDetected | PRList | PRDetails`)
- Observes `GiteaPullRequestsProjectService.activeRepoMappingState` and `GiteaAccountManager.accountsState` to transition between states
- Holds `selectedPRNumber: MutableStateFlow<Int?>` (null → list view; non-null → detail view)

**`GiteaPRToolWindowController`** — bridges factory to VM:
- Uses `com.intellij.collaboration.ui.toolwindow.ReviewTabsController` (or a simpler `ContentManager`-based approach) to swap panels when `GiteaPRToolWindowViewModel` state changes
- Registers tool window action group `"Gitea.PullRequest.ToolWindow"` in the toolbar

---

### 6. Build the PR list panel

Create in [`pullrequest/ui/list/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/ui/list):

**`GiteaPRListFiltersModel`** — holds `state: GiteaStateEnum`, `author: String?`, `label: String?`, `assignee: String?`; emits changes as `StateFlow`

**`GiteaPRListViewModel`** — implements (or delegates to) `com.intellij.collaboration.ui.codereview.list.CodeReviewListViewModel`:
- Collects `GiteaPullRequestsProjectService.pullRequestsState`
- Applies `GiteaPRListFiltersModel` locally or passes as query params
- Exposes `listState: StateFlow<ListState>` (`Loading | Empty | Error | Items`)
- Exposes `select(pr: GiteaPullRequest)` which sets `GiteaPRToolWindowViewModel.selectedPRNumber`

**`GiteaPRListItemComponent`** — single list cell renderer:
- Renders PR number (`#N`), title, author avatar (`GiteaUser.avatarUrl` via `GiteaAccountsDetailsProvider`), state badge (open/closed/merged), label chips, `reviewComments` count

**`GiteaPRListComponent`** — the full list panel:
- Header toolbar with filters (state dropdown, search field) and a Refresh action
- `JBList` with `GiteaPRListItemComponent` cells using `com.intellij.collaboration.ui.codereview.list.CodeReviewListPanel` or equivalent
- Empty/loading/error state panels

---

### 7. Build the PR details + timeline panel

Create in [`pullrequest/ui/details/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/ui/details) and [`pullrequest/ui/timeline/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/ui/timeline):

**`GiteaPRDetailsViewModel`**:
- Accepts a `GiteaPullRequestDataLoader`
- Exposes `StateFlow<GiteaPullRequest>` (title, body, state, metadata)
- Body rendered to HTML via `com.intellij.markdown.utils.MarkdownToHtmlConverter` or the platform Markdown plugin API

**`GiteaPRDetailsComponent`**:
- Top section: title, state badge, `#number`, "Open on Gitea" link, branch info (`head → base`)
- Meta sidebar: author, assignees (avatar list), reviewers (avatar + review state icon), labels, milestone, due date
- Uses `com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsCommitInfoComponentFactory` for commit range info

**`GiteaPRTimelineViewModel`**:
- Collects `commentsState` (issue-level) and `reviewsState` from `GiteaPullRequestDataLoader`
- Merges into a sorted `List<GiteaTimelineItem>` sealed class (`IssueComment`, `ReviewEvent`, `SystemEvent` for merge/close/reopen)
- Exposes `timelineState: StateFlow<List<GiteaTimelineItem>>`

**`GiteaPRTimelineComponent`**:
- Renders items in a vertical scrollable list using `com.intellij.collaboration.ui.codereview.timeline.CodeReviewTimelineUIUtil`
- `IssueComment` → author avatar, body (Markdown), timestamp, edit/delete actions (if current user is author)
- `ReviewEvent` → reviewer avatar, state icon (✓ approved / △ changes requested / 💬 comment), body, inline comment count
- `SystemEvent` → compact line with icon

---

### 8. Build the changed files + inline diff view

Create in [`pullrequest/ui/changes/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/ui/changes):

**`GiteaPRChangesViewModel`**:
- Collects `filesState` from `GiteaPullRequestDataLoader`
- Groups files by directory; exposes `StateFlow<List<GiteaChangedFile>>`
- Tracks which file is currently open in the diff viewer

**`GiteaPRChangesComponent`**:
- File tree / flat list with filename, status icon, `+additions -deletions` stats
- Double-click opens the file diff via `DiffManager.getInstance().showDiff(...)` or `DiffEditorTabFilesManager`

**Inline review comments (deferred / milestone 2)**:
- Hook into IntelliJ diff framework: implement a `DiffExtension` or `EditorGutterIconRenderer` that overlays review comments on diff lines
- Load `GiteaPullRequestReviewComment` objects from `GiteaPullRequestDataLoader.reviewsState` (each review → its comments via `repoGetPullRequestReviewComments`)
- Show comments in a gutter popup (following `GHPRDiffReviewSupportImpl` in the GitHub plugin)

---

### 9. Build the submit review action

Create in [`pullrequest/ui/review/`](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/ui/review):

**`GiteaSubmitReviewViewModel`**:
- Holds `body: MutableStateFlow<String>`, `event: MutableStateFlow<ReviewEvent>` (APPROVED | REQUEST_CHANGES | COMMENT)
- `submit()` coroutine: calls `repoCreatePullRequestReview`, then triggers `GiteaPullRequestDataLoader.reload()` for reviews and the PR itself

**`GiteaSubmitReviewComponent`**:
- Segmented button group for event type (Approve / Request Changes / Comment)
- `JBTextArea` for optional review body
- `Submit` button (disabled while loading)
- Inline validation: APPROVED and REQUEST_CHANGES require the user to not be the PR author

**`GiteaCreateCommentAction`** — toolbar action:
- Opens a popup text field at the bottom of the timeline
- On confirm: calls `repoCreatePullRequestComment`, reloads `commentsState`

---

### 10. Register in `plugin.xml` and `GiteaBundle.properties`

**[`plugin.xml`](../../src/main/resources/META-INF/plugin.xml)** additions:

```xml
<!-- Project service -->
<projectService
    serviceImplementation="...pullrequest.service.GiteaPullRequestsProjectService"/>

<!-- Tool window -->
<toolWindow id="Gitea Pull Requests"
            anchor="right"
            doNotActivateOnStart="true"
            icon="/images/giteaLogo.svg"
            factoryClass="...pullrequest.ui.toolwindow.GiteaPRToolWindowFactory"/>

<!-- Action group for tool window toolbar -->
<group id="Gitea.PullRequest.ToolWindow"
       class="com.intellij.openapi.actionSystem.DefaultActionGroup">
    <action id="Gitea.PullRequest.Refresh"
            class="...pullrequest.ui.GiteaPRRefreshAction"/>
    <action id="Gitea.PullRequest.OpenOnWeb"
            class="...pullrequest.ui.GiteaPROpenOnWebAction"/>
    <separator/>
    <action id="Gitea.PullRequest.SubmitReview"
            class="...pullrequest.ui.review.GiteaSubmitReviewAction"/>
</group>
```

**[`GiteaBundle.properties`](../../src/main/resources/messages/GiteaBundle.properties)** keys to add (representative set):

```properties
pullrequest.toolwindow.title=Gitea Pull Requests
pullrequest.list.empty=No pull requests found
pullrequest.list.loading=Loading pull requests\u2026
pullrequest.list.error=Failed to load pull requests
pullrequest.list.filter.state=State
pullrequest.list.filter.author=Author
pullrequest.list.filter.label=Label
pullrequest.details.open.on.web=Open on Gitea
pullrequest.details.branch.info={0} \u2192 {1}
pullrequest.review.action.approve=Approve
pullrequest.review.action.request.changes=Request Changes
pullrequest.review.action.comment=Comment
pullrequest.review.submit=Submit Review
pullrequest.review.body.placeholder=Leave a review comment\u2026
pullrequest.comment.create=Add Comment
pullrequest.comment.edit=Edit
pullrequest.comment.delete=Delete
pullrequest.changes.files.header=Changed Files ({0})
pullrequest.login.prompt=Log in to Gitea to view pull requests
pullrequest.no.repo=No Gitea repository detected in this project
```

---

### 11. Add tests

Create in [`src/test/kotlin/.../api/json/`](../../src/test/kotlin/com/github/jpmand/idea/plugin/gitea/api/json) (pure JUnit 4, no platform base class):

| Test class | Fixture file | What it validates |
|---|---|---|
| `GiteaJsonPRReviewTest` | `src/test/testData/pr_review.json` | `GiteaPullRequestReviewDTO` fields: `id`, `state`, `body`, `submittedAt`, `user.login` |
| `GiteaJsonPRReviewCommentTest` | `src/test/testData/pr_review_comment.json` | `GiteaPullRequestReviewCommentDTO` fields: `path`, `diffHunk`, `position`, `body` |
| `GiteaJsonIssueCommentTest` | `src/test/testData/issue_comment.json` | `GiteaIssueCommentDTO` fields: `id`, `body`, `createdAt`, `user.login` |
| `GiteaJsonPRFileTest` | `src/test/testData/pr_file.json` | `GiteaPullRequestFileDTO` fields: `filename`, `status`, `additions`, `deletions`, `patch` |

All test fixtures must be taken from real Gitea API responses (reference the [Swagger spec](https://gitea.com/swagger.v1.json) examples). Each test uses `GiteaJsonDeSerializer.fromJson(StringReader(json), Dto::class.java)` and `assertEquals`/`assertNotNull` assertions.

---

## Dependency Map (blocking order)

```
Steps 1-2 (DTOs + domain)
    ↓
Step 3 (REST funs)
    ↓
Step 4 (project service)  ← also needs GiteaGitRepositoryMapping resolved
    ↓
Steps 5-6 (tool window + list)   ← first shippable milestone
    ↓
Step 7 (details + timeline)
    ↓
Steps 8-9 (diff + review actions)
    ↓
Step 10 (plugin.xml + i18n) ← woven throughout, finalized here
Step 11 (tests)             ← can run in parallel from step 1 onward
```

---

## Further Considerations

1. **`GiteaGitRepositoryMapping` gap** — [`GiteaRepositoryCoordinates`](../../src/main/kotlin/com/github/jpmand/idea/plugin/gitea/api/GiteaRepositoryCoordinates.kt) exists but `GiteaGitRepositoryMapping` (referenced by `GiteaRepositoriesManager`) may be a missing or stub class. The project service depends on it. Must create or complete it to implement `HostedGitRepositoryMapping<GiteaServerPath, GiteaRepositoryPath>` before step 4.

2. **Phased delivery** — Steps 1–6 (data layer + service + list) form a shippable v1 with read-only PR browsing. Steps 7–9 (details, diff, review actions) can be a v2 milestone. Inline diff comments (step 8 deferred portion) are the most complex and can be scoped to v3.

3. **Markdown rendering** — PR body and comment bodies should render Markdown. Use `com.intellij.markdown.html.HtmlGeneratorProvider` (if available on platform 253) or fall back to `com.intellij.ui.jcef.JCEFHtmlPanel` with sanitized HTML. Confirm API stability before using.

4. **Pagination** — `repoListPullRequests` and related endpoints are paginated. The list VM should support infinite scroll or a "Load more" button using `page`/`limit` parameters.

5. **Token scope requirement** — Gitea tokens need `read:issue` scope to read PR comments and `write:issue` to post them, in addition to `read:repository`. Update the token generation URL in `GiteLoginUtil.buildNewTokenUrl` to include these scopes, and document in `GiteaBundle.properties`.

