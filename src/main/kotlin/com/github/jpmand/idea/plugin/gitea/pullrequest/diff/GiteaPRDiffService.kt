@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.intellij.collaboration.ui.codereview.diff.AsyncDiffRequestProcessorFactory
import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Project service that manages [GiteaPRDiffViewModel] instances (one per PR number)
 * and creates [DiffRequestProcessor] objects for the diff editor.
 *
 * Register in plugin.xml:
 * ```xml
 * <projectService serviceImplementation="...GiteaPRDiffService"/>
 * ```
 */
@Service(Service.Level.PROJECT)
class GiteaPRDiffService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    private val vmCache = ConcurrentHashMap<Int, GiteaPRDiffViewModel>()

    /**
     * Returns the cached [GiteaPRDiffViewModel] for [prNumber], or creates a new one.
     * [baseSha] and [headSha] are used only on first creation.
     */
    fun getOrCreateViewModel(
        prNumber: Int,
        baseSha: String,
        headSha: String,
    ): GiteaPRDiffViewModel {
        val prService = project.service<GiteaPullRequestsProjectService>()
        val mapping = prService.activeRepoMappingState.value
        val owner = mapping?.repository?.repositoryPath?.owner ?: ""
        val repo = mapping?.repository?.repositoryPath?.repository ?: ""
        return vmCache.getOrPut(prNumber) {
            GiteaPRDiffViewModel(cs, project, prService, owner, repo, prNumber, baseSha, headSha)
        }
    }

    /**
     * Creates a [DiffRequestProcessor] for all changed files in [prNumber].
     * If [initialRelativePath] is non-empty, the corresponding file is pre-selected.
     */
    fun createDiffRequestProcessor(
        prNumber: Int,
        baseSha: String,
        headSha: String,
        initialRelativePath: String = "",
    ): DiffRequestProcessor {
        val vm = getOrCreateViewModel(prNumber, baseSha, headSha)
        if (initialRelativePath.isNotEmpty()) {
            vm.showChangeForPath(initialRelativePath)
        }
        val vmFlow = MutableStateFlow<GiteaPRDiffViewModel?>(vm)
        return AsyncDiffRequestProcessorFactory.createIn(
            cs = cs,
            project = project,
            diffVmFlow = vmFlow,
            createContext = { emptyList() },
            changePresenter = { changeVm: GiteaPRDiffChangeViewModel ->
                changeVm.change.asPresentableChange()
            }
        )
    }

    /** Invalidates the cached VM for [prNumber] (e.g. after a force-push). */
    fun invalidate(prNumber: Int) {
        vmCache.remove(prNumber)
    }
}
