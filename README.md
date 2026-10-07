# Gitea PR & Code Review

![Build](https://github.com/JpMand/gitea-idea-integration/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/34787.svg)](https://plugins.jetbrains.com/plugin/34787)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/34787.svg)](https://plugins.jetbrains.com/plugin/34787)

> [!WARNING]  
> This is a project that makes heavy use of AI during development and probably not as efficiently or as adequately as it should (mostly because I barely know kotlin and know almost nothing about the IntelliJ Platform libraries).
> 
> This means that this is not good quality code, most of the code is done by following (or attempting to do so) the structure and flow of [Github](https://github.com/JetBrains/intellij-community/tree/master/plugins/github) and [Gitlab](https://github.com/JetBrains/intellij-community/tree/master/plugins/gitlab) plugins.
>
> Any help is greatly appreciated.

<!-- Plugin description -->
Review Gitea pull requests in your IDE: browse, comment, approve and merge, and clone
repositories from your Gitea servers.

**Pull requests**
- A Pull Requests tool window for the project's Gitea remote, filterable by state, author and label
- No account needed to read: pull requests of public repositories can be browsed read-only, and
  logging in from the conversation turns on commenting and reviewing
- Details, commits, status checks and changed files for each pull request
- A conversation timeline where you can reply to, edit, resolve and delete comments
- Review in the diff viewer or directly in the editor: draft comments and approve / request changes / comment verdicts
  - Also supports an implementation of suggested changes, which can be applied in one click[^1]
- Merge, close, reopen, check out the branch, and request reviewers

**Accounts and repositories**
- Personal access token login, with several accounts across several servers
- Tokens are kept in the IDE password safe and supplied to Git for HTTPS operations
- A Gitea tab in *Get from Version Control* for cloning
- Open in browser and copy link for files, lines and commits

**Requirements:** a Gitea 1.27 or later server (self-hosted, on a sub-path, HTTP or HTTPS) and the
bundled Git plugin.

> Currently other compatible services (Forgejo, Gogs, etc.) are not supported. This is to simplify maintenance and set plugin focus only on Gitea.

[Source code and issue tracker](https://github.com/JpMand/gitea-idea-integration)

[^1]: Suggested changes are not supported in Gitea, their implementation is made with usage of a custom HTML comment blocks. See [GiteaSuggestionUtil.kt](src/main/kotlin/com/github/jpmand/idea/plugin/gitea/pullrequest/review/GiteaSuggestionUtil.kt) for details.
<!-- Plugin description end -->

## Screenshots

<picture>
    <source media="(prefers-color-scheme: dark)" srcset="./assets/01-pr-list-dark.png">
    <source media="(prefers-color-scheme: light)" srcset="./assets/01-pr-list-light.png">
    <img alt="Pull Request List & Conversation" src="/assets/01-pr-list-light.png">
</picture>

<picture>
    <source media="(prefers-color-scheme: dark)" srcset="./assets/diff-review-dark.png">
    <source media="(prefers-color-scheme: light)" srcset="./assets/diff-review-light.png">
    <img alt="Code Review in Diff" src="/assets/diff-review-light.png">
</picture>

## Installation

- From the IDE: <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd>, search for
  "Gitea PR & Code Review", then <kbd>Install</kbd>.
- From disk: download the [latest release](https://github.com/JpMand/gitea-idea-integration/releases/latest)
  and use <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install Plugin from Disk...</kbd>.

The plugin targets IntelliJ IDEA 2026.2.

## Getting started

1. In Gitea, open **Settings** > **Applications** and generate an access token with the
   `read:user`, `write:repository` and `write:issue` scopes.
2. In the IDE, open **Settings** > **Version Control** > **Gitea**, click **+**, enter the server URL
   (e.g. `https://gitea.example.com`) and the token, then click **Log In**.
3. Open a project whose Git remote is on that server. The **Gitea PR** tool window
   appears, and Git operations over HTTPS use the account's token.

Without an account, the tool window still appears for a project whose Git remote is on a Gitea
server (detected through the server's `/api/v1/version`), showing public repositories' pull requests
read-only. Use **Log in to comment…** in a conversation to add an account for that server.

Only Gitea releases are accepted. To use a dev build or a release candidate, turn on **Accept Gitea
pre-release versions** in **Settings** > **Version Control** > **Gitea**.

To update a token or remove an account, select it in **Settings** > **Version Control** > **Gitea**.

## Development

```bash
./gradlew check        # tests and checks
./gradlew buildPlugin  # plugin ZIP in build/distributions
./gradlew runIde       # sandbox IDE with the plugin
```

Architecture and conventions are described in [AGENTS.md](AGENTS.md). Pull requests are welcome.

## License

[MIT](LICENSE)

## Acknowledgments

- Built from the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Modelled on the official GitHub and GitLab plugins in
  [intellij-community](https://github.com/JetBrains/intellij-community)
- [Gitea API documentation](https://docs.gitea.com/development/api-usage)
