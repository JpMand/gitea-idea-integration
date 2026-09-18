<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitea Integration Changelog

## [Unreleased]

### Added

- Pull request review, read-write: a tool window with a PR list (status/label/author/sort
  filters), per-PR detail tabs (changes tree, activity timeline), a REST-based diff viewer, and
  mark-as-viewed
- Review authoring: comment on lines in the diff editor or directly in the regular project editor
  (for any file belonging to a PR whose head branch matches the current local branch), start a
  pending review, submit with a verdict (approve / request changes / comment), resolve/unresolve
  threads, reply to/edit/delete your own comments, and a Cancel Review action to discard local
  drafts or delete a pending server-side review
- "Suggested Change" (GitHub-style, emulated client-side — Gitea has no native equivalent): create
  a suggestion straight from a local edit via a colored gutter bar, encoded as a hidden diff block
  in the comment body so it still renders as an ordinary syntax-highlighted diff in Gitea's own web
  UI; apply a suggestion with one click, which navigates to the edited location
- Full activity-timeline event coverage (labels, milestones, assignees, cross-references, commits,
  reviews with their inline threads) with review-comment diff-context previews
- @-mention autocomplete for repo collaborators in comment editors
- Merge (with method choice and delete-branch confirmation), close/reopen, and checkout-branch
  actions on a PR
- Right-click context menus on PR-list rows and timeline items
- "Gitea" tab in _Get from Version Control_ for cloning
- Open-in-browser / copy-link actions and an annotation-gutter action
- Declared notification group so Gitea notifications can be muted in Settings

### Changed

- Target platform is now IntelliJ IDEA 2026.2.1 (`since-build` 262, compiled against Java 25)
- REST DTOs regenerated from the Gitea Swagger v2 spec; `java.time.OffsetDateTime` for dates
- Minimum supported Gitea version is **1.26** (was an unreleased `1.27` snapshot that rejected
  every real server); tolerant `/version` parsing that no longer fails login on RC / dev / Forgejo
  version strings
- Unknown enum values in API responses deserialise to null instead of failing the whole response
- Account list storage no longer roams via Settings Sync (tokens never roamed; the list on its
  own left a second machine with accounts but no credentials)
- `plugin.xml` now declares its bundled platform module dependencies explicitly
- Comment/description bodies render markdown client-side (`convertMarkdownToHtml`) instead of an
  earlier server-rendered approach that was slow and prone to breaking
- Dropped remaining internal-API usage across account scopes, repository discovery, the clone
  flow, and the changes tree, keeping Marketplace plugin verification clean

### Fixed

- Account-chooser dialog rendered without its description text or "set as default" checkbox
- HTTPS clone URLs ending in `.git` were parsed as a repository literally named `<name>.git`
- Two inconsistent "account already exists" checks unified to one host + port + path,
  protocol-insensitive criterion
- Timeline reply/resolve/edit/delete patch the already-loaded activity list in place instead of
  reloading the whole timeline, and no longer rebuild every item's component on an unrelated
  update — which used to discard an in-progress reply composer's unsent text elsewhere on the page
- Editing an already-drafted line comment twice no longer risks silently reverting it to the
  pre-edit text
- Re-authenticating an existing account with a token that belongs to a *different* account no
  longer silently relabels the account under the new identity
- Commits that reference the PR from elsewhere are shown in the activity timeline again (were
  incorrectly treated as duplicates of the PR's own pushed commits)
- A non-Gitea HTTP response (e.g. a proxy error page) is no longer misread as a supported server
  version
- Auto-discovered Gitea servers now respect the git remote's own scheme instead of always assuming
  https, so a plain-HTTP self-hosted instance gets working "Open on Gitea"/"Copy link" actions
- A duplicate review toolbar/gutter bar could appear in the regular project editor after certain
  unrelated PR-context updates
- Two crash-risk edge cases in diff/suggestion rendering (a diff preview for a hunk near the start
  of a file; a suggestion gutter bar on a range touching the end of the file)
- Mutation endpoints (resolve, submit review, merge, comment edit/delete, etc.) now surface the
  same friendly error messages as read endpoints on auth/permission failures, instead of a raw
  HTTP exception
- Branch checkout from a PR's own changes branch, and local-commit author attribution, corrected
- Stale account-context avatars in the Conversation tab after switching accounts; a leaked
  Commit/CommitStatus DTO reaching UI code instead of a domain model

## [0.0.1] - 2026-03-13

### Added

- Initial Gitea integration plugin implementation
- Token-based authentication for Gitea servers
- Multiple account support
- Account management UI (add, update, remove accounts)
- Persistent account storage using IntelliJ's XML serialization
- Git HTTP authentication provider for seamless Git operations
- Account chooser dialog for selecting accounts per project
- Default account per project support
- Support for custom Gitea servers (self-hosted instances)
- Full support for Gitea instances on sub-paths (e.g., `https://example.com/gitea`)
- HTTP and HTTPS connection support
- REST API client using IntelliJ's collaboration tools framework
- User information retrieval from Gitea API
- Settings panel for account management in IDE preferences
- Internationalized UI strings (GiteaBundle)
- Comprehensive test suite:
  - Server path URI construction and sub-path handling tests
  - JSON deserialization tests for Gitea API responses
  - XML serialization/deserialization tests for account persistence
  - Date/time parsing tests with various formats
- Qodana code quality checks integration
- CI/CD pipeline with automated builds and tests

### Technical Implementation

- Clean separation between DTO (Data Transfer Objects) and domain models
- `GiteaUserDTO` for JSON deserialization with Jackson
- `GiteaUser` as clean domain model implementing `AccountDetails` and `CodeReviewUser`
- `GiteaAccount` with proper XML serialization annotations
- `GiteaServerPath` with robust URI construction handling edge cases
- `GiteaApiManager` for API client management
- `GiteaAccountManager` for account lifecycle management
- `GiteaHttpAuthDataProvider` for Git integration
- `GiteaLoginUtil` for authentication workflows
- `GiteaTokenLoginPanelModel` for login UI
- Service registrations in plugin.xml:
  - Application services: Account management, API management, Settings
  - Project services: Scope provider, Default account holder, Auth failure manager, Core service
  - Git4Idea extension: HTTP auth data provider

### Fixed

- Sub-path URI construction for Gitea servers on custom paths
- XML serialization tag case sensitivity (`<Server>` vs `<server>`)
- Date/time parsing using StdDateFormat for flexible format support
- Removed unused code symbols for cleaner codebase
- Made internal methods private for better encapsulation

[Unreleased]: https://github.com/JpMand/gitea-idea-integration/compare/0.0.1...HEAD
[0.0.1]: https://github.com/JpMand/gitea-idea-integration/commits/0.0.1
