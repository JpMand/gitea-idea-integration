package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
@State(name = "GiteaDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
class GiteaProjectDefaultAccountHolder(project: Project, parentCs: CoroutineScope) :
  PersistentDefaultAccountHolder<GiteaAccount>(
    project,
    parentCs.childScope(GiteaProjectDefaultAccountHolder::javaClass.name)
  ) {
  override fun accountManager() = service<GiteaAccountManager>()
  override fun notifyDefaultAccountMissing() {
    TODO("Not yet implemented")
  }
}