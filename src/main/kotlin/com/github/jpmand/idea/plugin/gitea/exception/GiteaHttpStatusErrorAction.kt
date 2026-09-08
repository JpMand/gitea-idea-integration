package com.github.jpmand.idea.plugin.gitea.exception

import com.github.jpmand.idea.plugin.gitea.authentication.GiteLoginUtil
import com.github.jpmand.idea.plugin.gitea.authentication.GiteaLoginSource
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.Nls
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

internal sealed class GiteaHttpStatusErrorAction(@Nls name : String) : AbstractAction(name) {
    class LogInAgain(
        private val project: Project,
        private val parentScope : CoroutineScope,
        private val account : GiteaAccount,
        private val accountManager: GiteaAccountManager,
        private val loginSource : GiteaLoginSource,
        private val resetAction : () -> Unit = {}
    ) : GiteaHttpStatusErrorAction(CollaborationToolsBundle.message("login.again.action.text")){
        override fun actionPerformed(event: ActionEvent) {
            val parentComponent = event.source as? JComponent ?: return
            val loginResult = GiteLoginUtil.updateToken(
                project,
                parentComponent,
                account,
                loginSource
            ){_, _ -> true}
                .asSafely<GiteLoginUtil.LoginResult.Success>()
        }
    }
}