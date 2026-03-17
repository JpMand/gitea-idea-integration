package com.github.jpmand.idea.plugin.gitea.authentication.extensions

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.collaboration.async.childScope
import com.intellij.collaboration.util.serviceGet
import com.intellij.openapi.components.Service
import git4idea.remote.hosting.http.HostedGitAuthenticationFailureManager
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
class GiteaGitAuthenticationFailureManager(parentCs: CoroutineScope) : HostedGitAuthenticationFailureManager<GiteaAccount>(
  serviceGet<GiteaAccountManager>(),
  parentCs.childScope(GiteaGitAuthenticationFailureManager::class)
)