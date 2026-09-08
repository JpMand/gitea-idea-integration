package com.github.jpmand.idea.plugin.gitea.api

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Repository
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.HostedGitRepositoryConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import java.util.*

@Suppress("UnstableApiUsage")
class GiteaRepositoryConnection internal constructor(
    project: Project,
    private val scope: CoroutineScope,
    override val account: GiteaAccount,
    val currentUser: GiteaUser,
    repositoryDto: Repository?,
    override val repo: GiteaGitRepositoryMapping,
    api: GiteaApi,
    tokenState: Flow<String>
) : HostedGitRepositoryConnection<GiteaGitRepositoryMapping, GiteaAccount> {

    val id: String = UUID.randomUUID().toString()

    override suspend fun close() {
        try {
            (scope.coroutineContext[Job] ?: error("Missing job")).cancelAndJoin()
        } catch (_: Exception) {
        }
    }

    override suspend fun awaitClose() {
        try {
            (scope.coroutineContext[Job] ?: error("Missing job")).join()
        } catch (_: Exception) {
        }
    }
}