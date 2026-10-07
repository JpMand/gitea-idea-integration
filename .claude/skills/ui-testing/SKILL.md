---
name: ui-testing
description: Set up and drive the Gitea plugin in a real sandbox IDE from a headless cloud container - local Gitea 1.27 in Docker with test data, IDE with the Remote-Robot server on a virtual display, click/type/screenshot driver. Use when verifying or screenshotting UI changes, reproducing a UI bug, or preparing the environment to build and test the plugin.
---

# UI verification in a cloud session

How to check the plugin's UI in a real IDE from a headless Claude Code cloud container (no human at
the screen): a local Gitea in Docker with test data, the sandbox IDE on a virtual display, and a
small driver that clicks, types and takes screenshots. Nothing here runs in CI.

## Environment facts

- **Platform:** IntelliJ IDEA 2026.2.1 (`platformVersion` in `gradle.properties`). Gradle downloads
  it on the first build (about 1 GB, several minutes).
- **JDK:** the build needs Java 25. The container's default is Java 21; the foojay toolchain
  resolver in `settings.gradle.kts` downloads Temurin 25 on the first build, so nothing to install.
- **Base image already has:** Docker (daemon not started), Xvfb, Go, Python 3, `jq`.
  `setup-env.sh` adds `xdotool`, ImageMagick (`import`), `x11-utils` and `openbox`.
- **Network:** besides Maven Central / Gradle / JetBrains hosts for the build, UI testing needs
  Gitea's registry `docker.gitea.com` (Docker Hub as fallback), `packages.jetbrains.team` (Remote-Robot plugin) and
  `proxy.golang.org` (to build `tea`). If one is blocked, the environment's network policy must allow it.
- **Test Gitea:** 1.27.3 (the plugin's minimum supported version is 1.27). `gitea-up.sh VERSION`
  runs another one, e.g. `gitea-up.sh 28.0.0` (Gitea numbers releases 28, 29, … after 1.27).
  It pulls `docker.gitea.com/gitea:VERSION` first and falls back to Docker Hub's `gitea/gitea`,
  which can answer `429 Too Many Requests` through the shared proxy.

## Commands

```bash
./gradlew check                    # unit tests (171 at 1.0.0)
./gradlew buildPlugin verifyPlugin # the verifier must report "Compatible" and no internal-API usages
```

Run `./gradlew clean check buildPlugin verifyPlugin` before handing work over.

## Setting up UI verification

Scripts are in `scripts/` next to this file. All are idempotent and keep their state (tokens,
fixture clone, logs, screenshots) in `$GITEA_UI_STATE`, default `~/.gitea-ui`.

```bash
.claude/skills/ui-testing/scripts/setup-env.sh        # packages, dockerd, Xvfb :99 + openbox, tea; restarts gitea if it exists
.claude/skills/ui-testing/scripts/gitea-up.sh         # fresh Gitea + fixture (destroys the previous one)
.claude/skills/ui-testing/scripts/ide.sh start        # builds and starts the sandbox IDE with the robot server on :8082
.claude/skills/ui-testing/scripts/login-and-clone.sh  # first run only: adds alice's account, clones acme/webapp
.claude/skills/ui-testing/scripts/ui.py shot overview # -> ~/.gitea-ui/shots/overview.png, then look at it with Read
.claude/skills/ui-testing/scripts/ide.sh off          # ALWAYS before committing: restores build.gradle.kts
```

After a container restart, run `setup-env.sh` again (Docker, Xvfb and the IDE don't survive it).
The IDE's settings, accounts and opened project live in `.intellijPlatform/sandbox/` and survive
`ide.sh stop/start`. The container has no `libsecret`, so the IDE keeps tokens in memory only:
after an IDE restart, log the accounts in again from Settings > Version Control > Gitea (tokens
are in `~/.gitea-ui/<user>.token`).
If `~/.gitea-ui/*.token` are gone but the `gitea` container survived, recover them from
`docker exec gitea cat /root/.config/tea/config.yml` (the `tok` helper in `fixture.sh`) rather than
rebuilding the fixture.

Expected noise on a fresh start, not caused by the plugin: an "IDE error occurred" balloon from
JetBrains' OS integration daemon and a `PasswordSafeSettings` "Unable to load library 'secret-1'"
error in `idea.log`, a warning about `JAVA_TOOL_OPTIONS` (the container's proxy settings), and
"JCEF sandboxing is not supported" because the IDE runs as root. The IDE log is
`.intellijPlatform/sandbox/gitea/IU-*/log_runIdeForUiTests/idea.log`.

## The fixture (`fixture.sh`)

Users `root` (admin), `alice`, `bob`, `carol`, all with password `Passw0rd!`; each has an API
token in `~/.gitea-ui/<user>.token`. Repository `acme/webapp` with all three as collaborators,
labels (`bug`, `enhancement`, `documentation`), milestone `v1.0`, and PRs:

| PR | State | What it exercises |
|----|-------|-------------------|
| #1 Add greeting service (bob) | open | labels, milestone, assignee, two reviewers, carol's REQUEST_CHANGES review with 4 inline threads (one suggestion, one reply, one resolved), general comments, a success and a pending commit status, a multi-paragraph commit message |
| #2 Refactor config loader (alice) | open | merge conflict with `main` |
| #3 Document how to run the app (carol) | open | review requested |
| #4 Experimental cache (bob) | draft | draft state |
| #5 Log startup to stderr (carol) | closed | closed state |
| #6 Add CI build script (alice) | merged | merged state |
| #7 Handle null in Util.trim (bob) | open | approved by carol |

In the IDE, alice's clone is at `~/IdeaProjects/webapp`. Check out `feature/greeting` there to test
the in-editor review (it works only when the local branch matches a PR's head branch).

## Driving the IDE (`ui.py`)

`ui.py` talks to the Remote-Robot server (Swing component tree by XPath, JavaScript run inside the
IDE via Rhino) and uses `xdotool` for real mouse and keyboard input. Run `ui.py` without
arguments for the command list. Useful ones:

```bash
ui.py tree "^MyDialog"                            # dump the visible component tree, to find XPaths
ui.py click "//div[@class='JButton' and @text='OK']"
ui.py clicktext "//div[@class='JBList']" "Add greeting service"
ui.py js 'java.lang.System.getProperty("idea.version")'   # run code inside the IDE
ui.py shot NAME                                   # full-screen screenshot
```

Rules learned the hard way:

- **Never type without checking focus first.** `xdotool type` goes to whatever has focus. A script
  that expected a dialog once typed an API token into an open editor and overwrote a source file.
  Find or wait for the target (`ui.py wait XPATH`), click it, then type.
- Open Settings and files through `ui.py js` (e.g. `ShowSettingsUtil.getInstance().showSettingsDialog(project, "Gitea")`,
  `FileEditorManager.getInstance(project).openFile(file, true)`), not keyboard shortcuts.
- Run `clear-notifications.sh` before clicks and screenshots; balloons cover the tool windows.
- Prefer XPath lookups to fixed coordinates; coordinates change with window size and zoom.
- `xdotool` clicks on items of a popup list (combo-box drop-downs, the branches popup's submenus)
  often don't register. Open the popup with a click, then pick with the keyboard (`Down`/`Up`,
  `Return`), and click dialog buttons by XPath.
- Look at every screenshot (Read the PNG) before drawing conclusions from it.
- To compare UI variants without rebuilding, gate the code on a system property and flip it with
  `ui.py js 'java.lang.System.setProperty("name", "value")'`, then refresh the view. Remove the
  switch before committing.

## Internal-API check

`verifyPlugin` only checks one IDE build. To see whether a single class or member you use is
`@ApiStatus.Internal`, inspect the platform jar with `javap -v` and look for the
`org/jetbrains/annotations/ApiStatus$Internal` annotation, on the class or on the member.
