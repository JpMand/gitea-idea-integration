package com.github.jpmand.idea.plugin.gitea

import com.intellij.driver.sdk.openToolWindow
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.jBlist
import com.intellij.driver.sdk.ui.components.elements.textField
import com.intellij.driver.sdk.ui.components.elements.button
import com.intellij.driver.sdk.ui.should
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.present
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.IdeInfoType
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Path

/**
 * Regression test for the click-to-open-PR-details flow, driven via the Starter/Driver
 * framework against a *real* Gitea server and a real cloned repo — replaces the throwaway,
 * never-committed `GiteaManualVerification.kt` this migration used to root-cause the original
 * bug (see PR_REVIEW_MIGRATION_PLAN.md): `GiteaPRListPanel` was discarding the JComponent that
 * `UiDataProvider.wrapComponent(...)` returns, so the list never actually exposed the selected
 * PR to the action system and Enter/double-click silently did nothing.
 *
 * Unlike `GiteaSmokeIntegrationTest`, this needs external state this repo doesn't provision on
 * its own: a local Gitea instance and a cloned sample repo. Tagged `manual` and excluded from
 * the default `integrationTest` run (see the `useJUnitPlatform` config in `build.gradle.kts`) —
 * run explicitly with `./gradlew integrationTest -PincludeManualTests` after bringing up the
 * Docker Gitea instance documented in `PR_REVIEW_MIGRATION_PLAN.md`
 * (`T:\LOCALDATA\PERSONAL\gitea-plugin`, sample repo cloned to `...\sample-repo`, PR #1 "Fix a
 * bug in README" is row index 2 in the default list ordering).
 */
@Tag("manual")
class GiteaPRListInteractionManualTest {

    private val pluginArchive: Path
        get() = Path.of(
            System.getProperty("path.to.build.plugin")
                ?: error("path.to.build.plugin system property not set — run via the `integrationTest` Gradle task."),
        )

    @Test
    fun `double-click on a PR row opens its details as a tool-window tab`() {
        val projectDir = Path.of("T:/LOCALDATA/PERSONAL/gitea-plugin/sample-repo")
        // IDEA_ULTIMATE, not IDEA_COMMUNITY — see the comment on the equivalent lookup in
        // GiteaSmokeIntegrationTest for why (Community stopped shipping as its own product line
        // as of 2025.3).
        val ideaUltimate: IdeInfo = di.direct.instance(tag = IdeInfoType.IDEA_ULTIMATE)
        // See the equivalent note in GiteaSmokeIntegrationTest on why this is pinned.
        val testCase = TestCase(ideaUltimate, LocalProjectInfo(projectDir)).withBuildNumber(PLATFORM_BUILD_NUMBER)

        val run = Starter.newContext("gitea-pr-list-interaction-test", testCase)
            .apply { PluginConfigurator(this).installPluginFromPath(pluginArchive) }
            // Without this, a never-before-seen project directory triggers the "Trust and Open
            // Project?" modal, which blocks headlessly — see the equivalent note in
            // GiteaSmokeIntegrationTest.
            .applyVMOptionsPatch { addSystemProperty("idea.trust.all.projects", true) }
            .runIdeWithDriver()
        // Both useDriver's and ideFrame's action blocks are extension lambdas (`Driver.() -> R`
        // / `IdeaFrameUI.() -> Unit`), so everything below runs with that type as implicit `this`
        // — see the equivalent note in GiteaSmokeIntegrationTest.
        run.useDriver<Unit> {
            // See the equivalent note in GiteaSmokeIntegrationTest — avoids a "No projects are
            // opened" race right after the IDE process becomes responsive.
            waitForProjectOpen()
            openToolWindow(TOOL_WINDOW_ID)

            ideFrame {
                // Log in if this is a fresh sandbox (no account persisted yet). The Driver
                // framework sets field values directly via setText rather than simulating
                // keystrokes, so this doesn't hit the AssertJ-Swing "Invalid key code" bug the
                // old Remote Robot setup had to work around with a clipboard-paste hack for
                // punctuation like the ':' in a URL.
                val serverField = textField(xQuery { byClass("ExtendableTextField") })
                if (serverField.present()) {
                    serverField.text = GITEA_SERVER_URL
                    textField(xQuery { byClass("JBPasswordField") }).text = GITEA_TOKEN
                    button(xQuery { byAccessibleName("Log In") }).click()
                }

                val list = jBlist(xQuery { byClass("JBList") })
                list.shouldBe(present)
                // Population happens after a real REST round trip to the Gitea instance.
                list.should { rawItems.isNotEmpty() }

                // PR details now open as a closeable tab *inside* the tool window (named "#<n>"),
                // not as an editor tab. Double-clicking must open the details content — assert the
                // details title (which repeats the PR row text) becomes visible a second time.
                list.doubleClickItem(PR_ROW_TEXT, false)

                val detailsTitle = x(xQuery { byVisibleText(PR_ROW_TEXT) })
                waitUntil { detailsTitle.present() }
                assertTrue(detailsTitle.present()) {
                    "Double-clicking '$PR_ROW_TEXT' did not open its details tab in the tool window"
                }
            }
        }
        run.closeIdeAndWait()
    }

    private fun waitUntil(timeoutMs: Long = 15_000, intervalMs: Long = 500, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(intervalMs)
        }
    }

    private companion object {
        const val TOOL_WINDOW_ID = "Gitea Pull Requests"
        const val GITEA_SERVER_URL = "http://localhost:3000"
        const val GITEA_TOKEN = "6f1135d4711de59f36c9d271743fcb17246f3dfb"
        const val PR_ROW_TEXT = "Fix a bug in README"
        const val PR_TAB_TEXT = "#1"
        val PLATFORM_BUILD_NUMBER: String = System.getProperty("gitea.test.platformBuildNumber") ?: "262.9437.185"
    }
}
