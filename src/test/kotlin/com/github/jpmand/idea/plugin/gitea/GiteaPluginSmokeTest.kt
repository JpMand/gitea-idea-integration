package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Loads the plugin in a headless IDE and resolves its registered services. Catches broken
 * plugin.xml wiring (missing `<dependencies><module>`, wrong service interface/impl pairs)
 * before it reaches users.
 */
class GiteaPluginSmokeTest : BasePlatformTestCase() {

  fun testApplicationServicesResolve() {
    val app = ApplicationManager.getApplication()
    assertNotNull(app.service<GiteaAccountManager>())
    assertNotNull(app.service<GiteaApiManager>())
    assertNotNull(app.service<GiteaServersManager>())
  }

  fun testProjectServicesResolve() {
    assertNotNull(project.service<GiteaRepositoriesManager>())
  }
}
