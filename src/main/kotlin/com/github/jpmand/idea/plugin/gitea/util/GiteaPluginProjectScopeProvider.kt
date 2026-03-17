package com.github.jpmand.idea.plugin.gitea.util

import com.intellij.collaboration.async.PluginScopeProviderBase
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
internal class GiteaPluginProjectScopeProvider(parentCs: CoroutineScope) : PluginScopeProviderBase(parentCs) {
}