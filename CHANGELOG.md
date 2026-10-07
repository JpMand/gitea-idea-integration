<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Gitea PR & Code Review Changelog

## [Unreleased]

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
- Read-only browsing of public repositories' pull requests without an account, with Gitea servers
  detected from the project's Git remotes
- Requires a Gitea 1.27 or later release and IntelliJ IDEA 2026.2; dev builds and release
  candidates can be allowed in Settings > Version Control > Gitea. Forgejo isn't supported

[Unreleased]: https://github.com/JpMand/gitea-idea-integration/commits/main
