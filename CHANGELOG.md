<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitea Integration Changelog

## [Unreleased]

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
