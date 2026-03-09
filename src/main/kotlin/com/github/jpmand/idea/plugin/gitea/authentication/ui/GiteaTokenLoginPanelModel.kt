package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.intellij.collaboration.auth.ui.login.LoginPanelModelBase
import com.intellij.collaboration.auth.ui.login.LoginTokenGenerator
import com.intellij.collaboration.util.URIUtil
import com.intellij.util.UriUtil

class GiteaTokenLoginPanelModel(
    var username: String? = null,
    var uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
) : LoginPanelModelBase(), LoginTokenGenerator {

    override suspend fun checkToken(): String {
        TODO("Not yet implemented")
    }

    override fun canGenerateToken(serverUri: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun generateToken(serverUri: String) {
        TODO("Not yet implemented")
    }

}