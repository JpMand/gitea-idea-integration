package com.github.jpmand.idea.plugin.gitea.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Disposer
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Owns a project-lifetime coroutine scope (constructor-injected: cancelled with the project and on
 * plugin unload) and hands out child scopes bounded to a shorter-lived [Disposable] — e.g. a dialog,
 * a settings panel or a tool-window controller.
 */
@Service(Service.Level.PROJECT)
internal class GiteaPluginProjectScopeProvider(private val projectScope: CoroutineScope) {

  /** A child of the project scope, cancelled when [disposable] is disposed. */
  fun childScope(
    name: String,
    disposable: Disposable,
    context: CoroutineContext = EmptyCoroutineContext,
  ): CoroutineScope {
    val scope = projectScope.childScope(name, context)
    Disposer.register(disposable) { scope.cancel() }
    return scope
  }
}
