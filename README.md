# Gitea PR & Code Review

![Build](https://github.com/JpMand/gitea-idea-integration/workflows/Build/badge.svg)

> [!NOTE]
> This project is developed with heavy use of AI assistance, by someone still learning Kotlin and
> the IntelliJ Platform. Most of its structure follows the official
> [GitHub](https://github.com/JetBrains/intellij-community/tree/master/plugins/github) and
> [GitLab](https://github.com/JetBrains/intellij-community/tree/master/plugins/gitlab) plugins.
> Reviews, issues and contributions are very welcome.

<!-- Plugin description -->
Review Gitea pull requests in your IDE: browse, comment, approve and merge, and clone
repositories from your Gitea servers.

**Pull requests**
- A Pull Requests tool window for the project's Gitea remote, filterable by state, author and label
- Details, commits, status checks and changed files for each pull request
- A conversation timeline where you can reply to, edit, resolve and delete comments
- Review in the diff viewer or directly in the editor: draft comments, suggested changes that can
  be applied in one click, and approve / request changes / comment verdicts
- Merge (with a choice of method), close, reopen, check out the branch, and request reviewers

**Accounts and repositories**
- Personal access token login, with several accounts across several servers
- Tokens are kept in the IDE password safe and supplied to Git for HTTPS operations
- A Gitea tab in *Get from Version Control* for cloning
- Open in browser and copy link for files, lines and commits

**Requirements:** a Gitea 1.27 or later server (self-hosted, on a sub-path, HTTP or HTTPS) and the
bundled Git plugin.

[Source code and issue tracker](https://github.com/JpMand/gitea-idea-integration)
<!-- Plugin description end -->

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
