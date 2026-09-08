package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.MutableAccountsListModel

@Suppress("UnstableApiUsage")
class GiteaAccountsListModel : MutableAccountsListModel<GiteaAccount, String>(),
    AccountsListModel.WithDefault<GiteaAccount, String> {
    override var defaultAccount: GiteaAccount? = null
}