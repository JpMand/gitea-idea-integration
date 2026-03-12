package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.services.GiteaService
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil

class MyPluginTest : BasePlatformTestCase() {

    fun testProjectService() {
        val giteaService = project.service<GiteaService>()
        assertNotNull(giteaService)
    }
}
