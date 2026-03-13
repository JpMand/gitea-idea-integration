package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.services.GiteaService
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    fun testProjectService() {
        val giteaService = project.service<GiteaService>()
        assertNotNull(giteaService)
    }
}
