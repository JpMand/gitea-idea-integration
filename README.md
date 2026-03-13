# Gitea Integration for IntelliJ IDEA

![Build](https://github.com/JpMand/gitea-idea-integration/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
Gitea integration plugin for IntelliJ Platform IDEs. This plugin provides seamless integration with Gitea, allowing you to authenticate, manage your Gitea accounts, and work with Git repositories hosted on Gitea instances directly from your IDE.

## Features

### Authentication
- **Token-based Authentication**: Secure authentication using Gitea access tokens
- **Multiple Account Support**: Manage multiple Gitea accounts from different servers
- **Account Persistence**: Your accounts are securely stored and persisted across IDE sessions
- **Git Integration**: Automatic authentication for Git operations on Gitea repositories

### Account Management
- **Easy Account Setup**: Simple dialog to add new Gitea accounts
- **Account Switching**: Quickly switch between different Gitea accounts
- **Token Management**: Update tokens without recreating accounts
- **Default Account**: Set a default account per project

### Server Support
- **Custom Servers**: Connect to any Gitea instance (self-hosted or cloud)
- **Sub-path Support**: Full support for Gitea instances hosted on sub-paths (e.g., `https://example.com/gitea`)
- **HTTP/HTTPS**: Support for both secure and insecure connections

## Requirements

- IntelliJ IDEA 2025.3.1 or later
- Git plugin enabled
- Gitea server access with a valid access token

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
