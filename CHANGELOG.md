<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitea PR & Code Review Changelog

## [Unreleased]

## [1.1.0] - 2026-10-07

### Added

- Read-only browsing of public repositories' pull requests without an account: Gitea servers are
  detected from the project's Git remotes, and *Log in to comment…* in the conversation adds an
  account for the server
- Setting to accept Gitea dev builds and release candidates, in Settings > Version Control > Gitea
- Errors from the plugin can be reported to JetBrains Marketplace from the IDE's error dialog

### Changed

- The tool window is now named *Gitea PR*, with new plugin and tool window icons, and pull request
  tabs in the editor get their own icon
- Login and Gitea detection require a Gitea 1.27 or later release; Forgejo isn't supported
- Open in Browser is offered only for remotes on a Gitea server
- Updated Logos & Icons

### Fixed

- With a commit selected in the Details tab, opening a file from the changes tree shows that
  commit's diff instead of the whole pull request's, including files the commit changed that
  aren't in the final diff
- Opening a review thread's file from the conversation shows the diff at the commit the review was
  made on and scrolls to the thread's line, outdated threads included

## [1.0.0-alpha] - 2026-10-05

First public pre-release.

### Added

- Pull Requests tool window for the project's Gitea remote, with state, author and label filters
- Pull request details: description, participants, labels, status checks, commits and changed
  files
- Conversation timeline with replies, edits, resolve/unresolve and deletion of comments
- Code review in the diff viewer or directly in the editor: draft comments, suggested changes
  applied in one click, and approve / request changes / comment verdicts
- Merge with a choice of method, close/reopen, branch checkout and reviewer requests
- @-mention completion in comment editors
- Token login with multiple accounts and servers, including servers on a sub-path or plain HTTP
- Git HTTPS authentication with the account's token
- Gitea tab in *Get from Version Control* for cloning
- Open in browser and copy link for files, lines and commits
- Requires Gitea 1.27 or later and IntelliJ IDEA 2026.2

[Unreleased]: https://github.com/JpMand/gitea-idea-integration/compare/1.1.0...HEAD
[1.1.0]: https://github.com/JpMand/gitea-idea-integration/compare/1.0.0-alpha...1.1.0
[1.0.0-alpha]: https://github.com/JpMand/gitea-idea-integration/commits/1.0.0-alpha
