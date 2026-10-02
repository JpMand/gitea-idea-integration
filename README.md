# Gitea Integration for IntelliJ IDEA

![Build](https://github.com/JpMand/gitea-idea-integration/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

> [!NOTE]  
> This is a project that makes use of AI during development and probabily not as efficiently or as adequately as it should (mostly because I barely know kotlin and know almost nothing about the IntelliJ Platform libraries).
> 
> This means that this is not good quality code, most of the code is done by copying (or attempting to do so) the structure and flow of [Github](https://github.com/JetBrains/intellij-community/tree/master/plugins/github) and [Gitlab](https://github.com/JetBrains/intellij-community/tree/master/plugins/gitlab) plugins.
>
> Any help is greately appreciated

<!-- Plugin description -->
Gitea integration plugin for IntelliJ Platform IDEs. Authenticate to one or more Gitea servers,
clone repositories, and review pull requests without leaving the IDE.

## Features

### Authentication & accounts
- Token-based authentication, multiple accounts across multiple servers
- Accounts persisted locally; tokens stored in the OS credential store
- Automatic credential supply for Git HTTPS operations on Gitea repositories
- A default account per project

### Repositories
- "Gitea" tab in _Get from Version Control_ to browse and clone your repositories
- Open-in-browser and copy-link actions for files, commits and lines

### Pull requests
- Pull Requests tool window for the project's Gitea remote, with state, author, label and sort
  filters
- Per-PR details: description, participants, labels, status checks, commits and a
  directory-grouped changes tree
- Conversation tab: comments, reviews with their inline threads and diff previews, and state
  changes; reply, edit, delete, resolve and comment from there
- Diff viewer with inline review threads
- Review in the diff or directly in the editor (when the local branch is the PR's head branch):
  draft comments, submit with approve / request changes / comment, suggested changes you can
  apply with one click
- Merge (with method choice), close/reopen, check out the PR branch, request reviewers
- Create pull requests from the current or any local branch: base branch, title and description
  (prefilled from the commits and the PR template), WIP, reviewers and labels, with a preview of
  the commits and changes; unpushed branches are pushed on create

### Server support
- Any Gitea instance (self-hosted or cloud), including instances on a sub-path
  (e.g. `https://example.com/gitea`), custom ports, and HTTP or HTTPS
- Minimum supported Gitea version: **1.27**

## Requirements

- IntelliJ IDEA 2026.2.x
- Git plugin enabled
- A Gitea (1.27+) server and a personal access token

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "gitea-idea-integration"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/JpMand/gitea-idea-integration/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Getting Started

### Setting up Authentication

1. **Generate a Gitea Access Token**:
   - Log in to your Gitea instance
   - Navigate to **Settings** > **Applications**
   - Generate a new access token with at least `read:user` scope
   - Copy the generated token

2. **Add Account in IDE**:
   - Open **Settings/Preferences** > **Version Control** > **Gitea**
   - Click the **+** button to add a new account
   - Enter your Gitea server URL (e.g., `https://gitea.example.com`)
   - Paste your access token
   - Click **Log In**

3. **Use with Git**:
   - Clone a repository from your Gitea server
   - The plugin will automatically authenticate Git operations
   - You can select which account to use when prompted

### Managing Accounts

- **Add Account**: Settings > Version Control > Gitea > **+** button
- **Update Token**: Settings > Version Control > Gitea > Select account > **pencil icon**
- **Remove Account**: Settings > Version Control > Gitea > Select account > **-** button
- **Set Default**: In the account chooser dialog, check "Set as default for this project"

## Development

### Building from Source

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Running the Plugin

```bash
./gradlew runIde
```

## Architecture

The plugin follows the same architectural patterns as the official GitHub and GitLab plugins for IntelliJ IDEA:

- **Account Management**: `GiteaAccountManager` handles account storage and retrieval
- **Authentication**: Token-based authentication via `GiteaLoginUtil`
- **API Client**: REST API client built on IntelliJ's collaboration tools framework
- **Git Integration**: `GiteaHttpAuthDataProvider` provides authentication for Git operations
- **UI Components**: Account settings panel, login dialogs, and account chooser

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the terms specified in the LICENSE file.

## Acknowledgments

- Built using the [IntelliJ Platform Plugin Template][template]
- Inspired by the official GitHub and GitLab plugins for IntelliJ IDEA
- Gitea API documentation: https://docs.gitea.com/development/api-usage

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
