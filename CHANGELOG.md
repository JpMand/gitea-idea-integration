<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitea Integration Changelog

## [Unreleased]

### Added

- Read-only pull request review: tool window with a PR list, per-PR detail tabs, an activity
  timeline, a REST-based diff viewer and a path-based changes tree, and mark-as-viewed
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

### Fixed

- Account-chooser dialog rendered without its description text or "set as default" checkbox
- HTTPS clone URLs ending in `.git` were parsed as a repository literally named `<name>.git`
- Two inconsistent "account already exists" checks unified to one host + port + path,
  protocol-insensitive criterion

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
