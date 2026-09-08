package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranches
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranchesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Suppress("UnstableApiUsage")
class GiteaPRBranchesViewModel(
    private val cs: CoroutineScope,
    private val prFlow: StateFlow<GiteaPullRequest>,
) : CodeReviewBranchesViewModel {

    override val sourceBranch: StateFlow<String> = prFlow
        .map { it.head.ref }
        .stateIn(cs, SharingStarted.Eagerly, prFlow.value.head.ref)

    // Phase 10: wire to git4idea repository state for real checkout detection
    private val _isCheckedOut = MutableStateFlow(false)
    override val isCheckedOut: SharedFlow<Boolean> = _isCheckedOut

    private val _showBranchesRequests = MutableSharedFlow<CodeReviewBranches>()
    override val showBranchesRequests: SharedFlow<CodeReviewBranches> = _showBranchesRequests

    override fun fetchAndCheckoutRemoteBranch() {
        // Phase 10: implement git checkout of head branch via git4idea
    }

    override fun showBranches() {
        cs.launch {
            val pr = prFlow.value
            _showBranchesRequests.emit(CodeReviewBranches(pr.head.ref, pr.base.ref))
        }
    }
}
