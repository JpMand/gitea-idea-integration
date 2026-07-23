package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.github.jpmand.idea.plugin.gitea.authentication.GiteaLoginSource
import com.github.jpmand.idea.plugin.gitea.exception.GiteaHttpStatusErrorAction
import com.intellij.collaboration.async.childScope
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.ApiStatus
import javax.swing.Action

interface GiteaAccountViewModel {
    fun loginAction(loginSource: GiteaLoginSource) : Action
}

@ApiStatus.Internal
@Suppress("UnstableApiUsage")
internal class GiteaAccountViewModelImpl(
    private val project: Project,
    parentCs: CoroutineScope,
    private val account: GiteaAccount,
    private val accountManager: GiteaAccountManager
) : GiteaAccountViewModel {
    private val cs: CoroutineScope = parentCs.childScope("Gitea Account VM")

    override fun loginAction(loginSource: GiteaLoginSource): Action {
        return GiteaHttpStatusErrorAction.LogInAgain(project, cs, account, accountManager, loginSource)
    }
}