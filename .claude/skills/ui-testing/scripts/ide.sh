#!/usr/bin/env bash
# ide.sh start|stop|off : the sandbox IDE with the Remote-Robot server (http://127.0.0.1:8082) on DISPLAY=:99.
#   start  adds the LOCAL-ONLY runIdeForUiTests task to build.gradle.kts (hidden from git with
#          skip-worktree), (re)builds the plugin, starts the IDE and waits for the robot server
#   stop   kills the IDE
#   off    kills the IDE and restores build.gradle.kts. Run it before committing.
set -euo pipefail
cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
STATE=${GITEA_UI_STATE:-$HOME/.gitea-ui}; mkdir -p "$STATE"
stop() { for p in $(pgrep -f "[j]br/bin/java -Daether" || true); do kill $p; done; sleep 5; }
case ${1:-} in
start)
  if ! grep -q runIdeForUiTests build.gradle.kts; then
    cat >> build.gradle.kts <<'EOB'

// LOCAL-ONLY (UI verification, do not commit): IDE with the Remote-Robot server plugin.
intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Drobot-server.host.public=false",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                        "-Didea.trust.all.projects=true",
                        "-Dide.show.tips.on.startup.default.value=false",
                        // 2026.3, from JetBrains on the Platform forum: the modal welcome screen instead of
                        // the "IntelliJ IDEA Home" frame (temporary, t/5083), and EAP builds in free-tier
                        // mode instead of asking for a JetBrains Account login (t/3007).
                        "-Didea.welcome.screen.non.modal.enabled=false",
                        "-Deap.require.license=release",
                        // EAP builds send exceptions to JetBrains automatically; the container's missing
                        // D-Bus and libsecret would be reported on every start (registry key as a property).
                        "-Dea.auto.report.allowed=false",
                    )
                }
            }
            plugins {
                robotServerPlugin()
            }
        }
    }
}
EOB
    git update-index --skip-worktree build.gradle.kts
  fi
  stop
  (DISPLAY=:99 nohup ./gradlew runIdeForUiTests --console=plain > "$STATE/runide.log" 2>&1 &)
  for i in $(seq 1 120); do curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8082/ 2>/dev/null | grep -q 200 && break; sleep 3; done
  curl -s -o /dev/null http://127.0.0.1:8082/ || { echo "robot server did not come up, see $STATE/runide.log" >&2; exit 1; }
  sleep 20; echo "IDE up (log: $STATE/runide.log)" ;;
stop) stop ;;
off)
  stop
  if grep -q runIdeForUiTests build.gradle.kts; then
    git update-index --no-skip-worktree build.gradle.kts && git checkout -- build.gradle.kts
  fi ;;
*) sed -n '2,6p' "$0"; exit 2 ;;
esac
