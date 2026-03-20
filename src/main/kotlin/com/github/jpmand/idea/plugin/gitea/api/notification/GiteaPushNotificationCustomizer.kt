package com.github.jpmand.idea.plugin.gitea.api.notification

import com.intellij.dvcs.push.VcsPushOptionValue
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.push.GitPushNotificationCustomizer
import git4idea.push.GitPushRepoResult
import git4idea.push.findRemoteBranch
import git4idea.push.isSuccessful
import git4idea.repo.GitRepository

private val LOG = logger<GiteaPushNotificationCustomizer>()

@Suppress("UnstableApiUsage")
class GiteaPushNotificationCustomizer(private val project: Project) : GitPushNotificationCustomizer {
  override suspend fun getActions(
    repository: GitRepository,
    pushResult: GitPushRepoResult,
    customParams: Map<String, VcsPushOptionValue>
  ): List<AnAction> {
    if (!pushResult.isSuccessful) return emptyList()
    val remoteBranch = pushResult.findRemoteBranch(repository) ?: return emptyList()
    TODO("Not Yet Implemented")
  }

}