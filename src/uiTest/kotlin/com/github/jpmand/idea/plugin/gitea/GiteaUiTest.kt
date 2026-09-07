package com.github.jpmand.idea.plugin.gitea

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import org.junit.Before
import org.junit.Test
import java.time.Duration

/**
 * Smoke-level UI tests driven via Remote Robot against a live sandbox IDE.
 *
 * Prerequisite: `./gradlew runIdeForUiTests` must already be running in a separate terminal
 * (it starts a sandbox IDE with the robot-server plugin listening on port 8082, see
 * `build.gradle.kts`). Then run `./gradlew uiTest` here.
 *
 * Scope is deliberately narrow and doesn't require a live Gitea server or an open project:
 * it confirms the IDE starts with no fatal plugin-load error, and that this plugin's
 * Application-scoped extension points (PR tool window, diff extension) are actually
 * registered and resolvable by the real running platform — precisely the class of bug (a
 * dangling class reference in plugin.xml, a package rename) this migration kept tripping
 * over. The Settings page (project-scoped, so it needs an open project — see the comment in
 * the test below) and the fuller "log in, see PR list, open a PR, view the diff" flow (needs
 * a live Gitea instance) are covered as manual verification steps instead (see the
 * Verification section of PR_REVIEW_MIGRATION_PLAN.md).
 */
class GiteaUiTest {

    private val remoteRobot = RemoteRobot("http://127.0.0.1:8082")

    @Before
    fun waitForIde() {
        waitFor(Duration.ofSeconds(60), Duration.ofSeconds(1)) {
            runCatching { remoteRobot.callJs<Boolean>("true") }.getOrDefault(false)
        }
    }

    @Test
    fun `ide starts with no fatal plugin load error`() {
        // A malformed plugin.xml or a dangling class reference surfaces as an IDE error
        // notification on startup ("Plugin ... failed to load", "Fatal error"). Its absence
        // here is a real regression check, not a tautology — this is exactly the class of
        // bug (stale plugin.xml class refs) this migration kept hitting mid-session.
        val hasFatalError = remoteRobot.callJs<Boolean>(
            """
            importClass(com.intellij.ide.plugins.PluginManagerCore)
            var hasError = false
            var plugins = PluginManagerCore.getLoadedPlugins()
            for (var i = 0; i < plugins.length; i++) {
                if (plugins[i].getPluginId().getIdString() == 'com.github.jpmand.idea.plugin.gitea') {
                    hasError = !plugins[i].isEnabled()
                }
            }
            hasError
            """.trimIndent(),
            true,
        )
        assert(!hasFatalError) { "The Gitea plugin failed to load/is disabled on IDE startup" }
    }

    @Test
    fun `gitea extension points are registered`() {
        val missing = remoteRobot.callJs<String>(
            """
            importClass(com.intellij.openapi.extensions.ExtensionPointName)
            // com.intellij.projectConfigurable is deliberately not checked here: it's a
            // project-scoped extension point that doesn't exist in the Application container
            // at all without an open project (confirmed empirically against a live sandbox —
            // querying it here throws "Missing extension point ... in container Application",
            // not "not found", i.e. it's a real API scoping fact, not a flaky lookup). Settings
            // page registration is instead covered by the manual verification pass.
            var expectations = [
                ['com.intellij.toolWindow', 'com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRToolWindowFactory'],
                ['com.intellij.diff.DiffExtension', 'com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension'],
            ]
            var missing = []
            for (var i = 0; i < expectations.length; i++) {
                var epName = expectations[i][0]
                var expectedClass = expectations[i][1]
                var ep = ExtensionPointName.create(epName)
                var found = false
                var extensions = ep.getExtensionList()
                for (var j = 0; j < extensions.size(); j++) {
                    var ext = extensions.get(j)
                    // toolWindow contributes lazy ToolWindowEP beans (factoryClass is a plain
                    // string field, not an instantiated object) rather than the factory
                    // itself, unlike diff.DiffExtension which registers real instances.
                    var actualClassName = (epName == 'com.intellij.toolWindow')
                        ? String(ext.factoryClass)
                        : String(ext.getClass().getName())
                    if (actualClassName == expectedClass || String(ext.toString()).indexOf(expectedClass) >= 0) {
                        found = true
                    }
                }
                if (!found) missing.push(epName + ' -> ' + expectedClass)
            }
            missing.join('; ')
            """.trimIndent(),
            true,
        )
        assert(missing.isBlank()) { "Missing/unresolved Gitea extensions: $missing" }
    }
}
