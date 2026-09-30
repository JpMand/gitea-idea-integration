# CLAUDE.md – Gitea IntelliJ Plugin

Project conventions, architecture and commands are in `AGENTS.md`, imported here so Claude Code
loads it too:

@AGENTS.md

The rest of this file adds what is specific to working as Claude Code on this repo.

## Branches

- `main` and `releases/*` are protected: change them only through PRs.
- `releases/ide-<version>` (e.g. `releases/ide-2026.2`) carries a release for one IDE line;
  the next platform line (2026.3) breaks APIs the plugin uses, so each line gets its own branch.
- The plugin's version and IDE range are in `gradle.properties`. `CHANGELOG.md` feeds the
  plugin's change notes, and the README section between the `Plugin description` markers
  becomes its description: keep both true to the code.

## Before handing work over

1. `./gradlew clean check buildPlugin verifyPlugin`: all tests pass, and the verifier says
   "Compatible" with no internal-API usages.
2. No `@ApiStatus.Internal` platform API, including overloads and nested classes. The
   verifier only checks one IDE build, so check anything new from `com.intellij.collaboration.*`
   with `javap -v` on the platform jar (see the `ui-testing` skill, "Internal-API check").
3. User-visible strings are in `GiteaBundle.properties`. A message that takes `{n}` arguments
   goes through `MessageFormat`, so its apostrophes are written `''`; a message without
   arguments keeps a single `'`.
4. For UI changes: verify in the sandbox IDE (`.claude/skills/ui-testing/`), look at the
   screenshots, then run `scripts/ide.sh off` so `build.gradle.kts` is back to normal.
5. Re-read the diff for leftovers: temporary switches, debug output, local-only build changes.

## Cloud session notes

- A fresh container knows nothing from earlier sessions; this file, `AGENTS.md` and the
  `ui-testing` skill are the shared memory. Add to them when you learn something the next
  session would otherwise have to rediscover.
- JDK 25 and the IDE are downloaded by Gradle on the first build (several minutes); the
  container's default Java 21 is fine for running Gradle.
- Outbound traffic goes through a proxy. If a download fails, read `/root/.ccr/README.md`
  before trying workarounds.
