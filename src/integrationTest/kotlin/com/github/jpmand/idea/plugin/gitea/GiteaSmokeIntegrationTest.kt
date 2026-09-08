package com.github.jpmand.idea.plugin.gitea

import com.intellij.driver.sdk.getToolWindow
import com.intellij.driver.sdk.isPluginDisabled
import com.intellij.driver.sdk.isPluginLoaded
import com.intellij.driver.sdk.openToolWindow
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.IdeInfoType
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Files
import java.nio.file.Path

/**
 * Smoke-level integration test driven via the IntelliJ Starter/Driver framework
 * (https://plugins.jetbrains.com/docs/intellij/integration-tests.html), replacing the previous
 * Remote Robot-based `GiteaUiTest`. Unlike that setup, there's no separate "leave a sandbox
 * running in another terminal" step: `Starter.newContext(...).runIdeWithDriver()` launches,
 * drives, and tears down the IDE process itself, all from this one JUnit 5 test.
 *
 * Scope is deliberately narrow and doesn't require a live Gitea server (an empty scratch
 * directory as the project is enough — `NoProject` doesn't work here since the "Gitea Pull
 * Requests" tool window is project-scoped and querying it needs a project actually open): it
 * confirms the IDE starts with no fatal plugin-load error, and that this plugin's tool window
 * actually registers and opens — precisely the class of bug (a dangling class reference in
 * plugin.xml, a package rename) this migration kept tripping over. Fuller flows (login, PR list,
 * click-to-open interaction, diff view against real data) need a live Gitea instance and a real
 * repo — see `GiteaPRListInteractionManualTest` and the manual verification steps in
 * `PR_REVIEW_MIGRATION_PLAN.md`.
 */
class GiteaSmokeIntegrationTest {

    private val pluginArchive: Path
        get() = Path.of(
            System.getProperty("path.to.build.plugin")
                ?: error(
                    "path.to.build.plugin system property not set — run via the `integrationTest` " +
                        "Gradle task (which sets it from the buildPlugin output), not this class directly.",
                ),
        )

    @Test
    fun `plugin loads with no fatal error and its tool window opens`() {
        // IdeInfo.Companion.ideaUltimate/defaultIdeaUltimate (from the ide-starter-product-*
        // artifacts) are internal to their own Gradle module and unresolvable from here, so this
        // replicates their lookup directly against the same public Kodein DI container they
        // wrap (each ide-starter-product-* artifact registers its IdeInfo under this tag via a
        // META-INF/services/com.intellij.ide.starter.models.IdeProductInit entry).
        //
        // IDEA_ULTIMATE, not IDEA_COMMUNITY: per JetBrains' unified-distribution plan
        // (https://blog.jetbrains.com/idea/2025/07/intellij-idea-unified-distribution-plan/),
        // Community stopped being published as its own product line as of 2025.3 — every build
        // from 2025.3 onward, including this plugin's 2026.2.x floor, only exists under the "IU"
        // product code now. Confirmed directly against the real release feed
        // (data.services.jetbrains.com/products/releases?code=IU): build 262.9437.185 (2026.2.1,
        // released 2026-08-10) is really there. Querying `code=IC` for it 404s — that was this
        // test's original bug, not a genuine absence of a public 2026.2.x build.
        val ideaUltimate: IdeInfo = di.direct.instance(tag = IdeInfoType.IDEA_ULTIMATE)
        // Pin the exact build the plugin targets (gradle.properties' `platformTestBuildNumber`)
        // — Starter's default product lookup otherwise downloads whatever build its own catalog
        // considers current for the product, which can be well below this plugin's `sinceBuild`
        // floor and makes the plugin correctly refuse to load, failing this test for a reason
        // that has nothing to do with the plugin itself. `withBuildNumber(...)` (rather than
        // `useRelease("2026.2.1")`) pins the literal build number directly — either should work
        // once the product code above is right; this just avoids relying on how `useRelease`
        // maps a marketing version string to a build.
        val scratchProjectDir = Files.createTempDirectory("gitea-smoke-project")
        val testCase = TestCase(ideaUltimate, LocalProjectInfo(scratchProjectDir)).withBuildNumber(PLATFORM_BUILD_NUMBER)

        val run = Starter.newContext("gitea-smoke-test", testCase)
            .apply { PluginConfigurator(this).installPluginFromPath(pluginArchive) }
            // Without this, a never-before-seen project directory triggers the "Trust and Open
            // Project?" modal, which blocks headlessly — the driver then sees zero open projects
            // indefinitely instead of the tool window it's waiting to query.
            .applyVMOptionsPatch { addSystemProperty("idea.trust.all.projects", true) }
            .runIdeWithDriver()
        // useDriver's action block is a `Driver.() -> R` extension lambda (confirmed via the
        // compiler once the Kotlin Gradle plugin version was bumped to understand this
        // dependency's newer metadata — see the version-bump note in build.gradle.kts / the
        // libs.versions.toml kotlin entry), so everything below runs with `Driver` as `this`.
        run.useDriver<Unit> {
            assertTrue(isPluginLoaded(PLUGIN_ID)) { "$PLUGIN_ID did not load" }
            assertFalse(isPluginDisabled(PLUGIN_ID)) { "$PLUGIN_ID is disabled after startup" }

            // useDriver hands over control as soon as the IDE process is responsive, which can
            // be before the scratch project has actually finished opening — querying a
            // project-scoped tool window before that races and throws "No projects are opened".
            waitForProjectOpen()
            openToolWindow(TOOL_WINDOW_ID)
            val toolWindow = getToolWindow(TOOL_WINDOW_ID)
            assertTrue(toolWindow.isVisible()) { "'$TOOL_WINDOW_ID' tool window did not open" }
        }
        run.closeIdeAndWait()
    }

    private companion object {
        const val PLUGIN_ID = "com.github.jpmand.idea.plugin.gitea"
        const val TOOL_WINDOW_ID = "Gitea Pull Requests"
        // Falls back to a literal only if run outside the `integrationTest` Gradle task (which
        // always sets this from gradle.properties' `platformTestBuildNumber`) — keep in sync
        // manually with that property if this ever needs to be used standalone.
        val PLATFORM_BUILD_NUMBER: String = System.getProperty("gitea.test.platformBuildNumber") ?: "262.9437.185"
    }
}
