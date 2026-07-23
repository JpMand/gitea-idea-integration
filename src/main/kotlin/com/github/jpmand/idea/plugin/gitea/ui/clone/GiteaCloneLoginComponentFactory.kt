package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.authentication.GiteaLoginErrorStatusPresenter
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneLoginViewModel
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.async.nestedDisposable
import com.intellij.collaboration.auth.ui.AccountsPanelFactory.Companion.addWarningForPersistentCredentials
import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.collaboration.auth.ui.login.TokenLoginInputPanelFactory
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.util.bindDisabledIn
import com.intellij.collaboration.ui.util.bindVisibilityIn
import com.intellij.ide.IdeBundle
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
internal object GiteaCloneLoginComponentFactory {

    fun create(
        cs: CoroutineScope,
        loginVm: GiteaCloneLoginViewModel,
        cloneVm: GiteaCloneViewModel
    ): JComponent {
        val loginModel = loginVm.tokenLoginModel
        val titlePanel = JBUI.Panels.simplePanel().apply {
            val title = JBLabel(GiteaBundle.message("clone.dialog.login.title"), UIUtil.ComponentStyle.LARGE).apply {
                font = JBFont.label().biggerOn(5.0f)
            }
            addToLeft(title)
        }
        val loginButton = JButton(CollaborationToolsBundle.message("clone.dialog.button.login.mnemonic")).apply {
            bindDisabledIn(cs, loginModel.loginState.map { it is LoginModel.LoginState.Connecting })
        }
        val backLink =
            LinkLabel<Unit>(IdeBundle.message("button.back"), null) { _, _ -> cloneVm.switchToRepositoryList() }.apply {
                bindVisibilityIn(cs, loginVm.accounts.map { it.isNotEmpty() })
            }

        val loginInputPanel = TokenLoginInputPanelFactory(loginModel).createIn(
            cs,
            serverFieldDisabled = false,
            tokenNote = "",
            errorPresenter = GiteaLoginErrorStatusPresenter(cs, loginModel),
            footer = {
                row("") {
                    cell(loginButton)
                    cell(backLink)

                    addWarningForPersistentCredentials(
                        cs,
                        service<GiteaAccountManager>().canPersistCredentials,
                        ::panel
                    )
                        .align(AlignX.RIGHT)
                }
            }
        ).apply {
            border = JBUI.Borders.empty(8, 0, 0, 35)
            registerValidators { cs.nestedDisposable() }
        }

        loginButton.addActionListener {
            cs.launch {
                CollaborationToolsUIUtil.validateAndApplyAction(loginInputPanel) {
                    loginModel.login()
                }
            }
        }

        return VerticalListPanel().apply {
            border = JBEmptyBorder(UIUtil.getRegularPanelInsets())
            add(titlePanel)
            add(loginInputPanel)
        }
    }
}
