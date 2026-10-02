package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.showCheckBoxPicker
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.showReviewersPicker
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.LoadingLabel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModelFromDetails
import com.intellij.collaboration.ui.codereview.changes.CodeReviewChangeListComponentFactory
import com.intellij.collaboration.ui.codereview.create.CodeReviewTitleDescriptionComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsCommitsComponentFactory
import com.intellij.collaboration.ui.codereview.details.CommitPresentation
import com.intellij.collaboration.ui.setHtmlBody
import com.intellij.collaboration.util.CollectionDelta
import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import git4idea.GitBranch
import git4idea.GitRemoteBranch
import git4idea.ui.branch.MergeDirectionComponentFactory
import git4idea.ui.branch.MergeDirectionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.util.Date
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The "New Pull Request" tab: branches, title and description, WIP, reviewers and labels, a status
 * line saying what creating would do, and below it a preview of the commits and changed files.
 */
@Suppress("UnstableApiUsage")
object GiteaPRCreateComponentFactory {

    fun create(
        cs: CoroutineScope,
        vm: GiteaPRCreateViewModel,
        onOpenPullRequest: (GiteaPullRequest) -> Unit,
        onCancel: () -> Unit,
    ): JComponent {
        val titleEditor = CodeReviewTitleDescriptionComponentFactory.createTitleEditorIn(
            vm.project, cs, vm, GiteaBundle.message("pull.request.create.title.placeholder"),
        )
        val descriptionEditor = CodeReviewTitleDescriptionComponentFactory.createDescriptionEditorIn(
            vm.project, cs, vm, GiteaBundle.message("pull.request.create.description.placeholder"),
        )
        val titleAndDescription = CodeReviewTitleDescriptionComponentFactory.createIn(cs, vm, titleEditor, descriptionEditor)

        val wip = JBCheckBox(GiteaBundle.message("pull.request.create.wip"), vm.isWip.value).apply {
            isOpaque = false
            addActionListener { vm.isWip.value = isSelected }
        }

        val form = VerticalListPanel(JBUI.scale(8)).apply {
            border = JBUI.Borders.empty(12)
            add(directionSelector(cs, vm))
            add(titleAndDescription)
            add(wip)
            add(reviewersRow(cs, vm))
            add(labelsRow(cs, vm))
            add(statusLine(cs, vm, onOpenPullRequest))
            add(buttons(cs, vm, onCancel))
        }

        return OnePixelSplitter(true, "Gitea.PR.Create.Splitter", 0.6f).apply {
            firstComponent = ScrollPaneFactory.createScrollPane(form, true)
            secondComponent = preview(cs, vm)
        }
    }

    // ── Branches ──────────────────────────────────────────────────────────

    private fun directionSelector(cs: CoroutineScope, vm: GiteaPRCreateViewModel): JComponent {
        val model = object : MergeDirectionModel<GiteaGitRepositoryMapping> {
            override val baseRepo: GiteaGitRepositoryMapping = vm.mapping
            override var baseBranch: GitRemoteBranch?
                get() = vm.baseBranch.value
                set(value) = vm.setBaseBranch(value)
            override val headRepo: GiteaGitRepositoryMapping = vm.mapping
            override val headBranch: GitBranch? get() = vm.headBranch.value
            override var headSetByUser: Boolean = false

            override fun setHead(repo: GiteaGitRepositoryMapping?, branch: GitBranch?) = vm.setHeadBranch(branch)

            override fun addAndInvokeDirectionChangesListener(listener: () -> Unit) {
                cs.launch { combine(vm.baseBranch, vm.headBranch) { _, _ -> }.collect { listener() } }
            }

            override fun getKnownRepoMappings(): List<GiteaGitRepositoryMapping> = listOf(vm.mapping)
        }
        return MergeDirectionComponentFactory(
            model,
            { it.baseBranch?.nameForRemoteOperations },
            { it.headBranch?.name },
        ).create()
    }

    // ── Reviewers & labels ────────────────────────────────────────────────

    private fun reviewersRow(cs: CoroutineScope, vm: GiteaPRCreateViewModel): JComponent {
        val names = JBLabel()
        lateinit var link: ActionLink
        link = ActionLink(GiteaBundle.message("pull.request.create.edit")) {
            cs.launch {
                val candidates = vm.loadPossibleReviewers()
                val delta = showReviewersPicker(RelativePoint.getSouthWestOf(link), candidates, vm.reviewers.value) ?: return@launch
                vm.reviewers.value = vm.reviewers.value.applyDelta(delta) { it.login }
            }
        }
        cs.launch {
            vm.reviewers.collect { users -> names.text = users.joinToString { it.login }.ifEmpty { GiteaBundle.message("pull.request.create.none") } }
        }
        return metadataRow(GiteaBundle.message("pull.request.create.reviewers"), names, link)
    }

    private fun labelsRow(cs: CoroutineScope, vm: GiteaPRCreateViewModel): JComponent {
        val names = JBLabel()
        lateinit var link: ActionLink
        link = ActionLink(GiteaBundle.message("pull.request.create.edit")) {
            cs.launch {
                val candidates = vm.loadLabels()
                val delta = showCheckBoxPicker(
                    RelativePoint.getSouthWestOf(link),
                    GiteaBundle.message("pull.request.create.labels.title"),
                    GiteaBundle.message("pull.request.create.apply"),
                    candidates, vm.labels.value,
                    text = { it.name }, key = { it.id },
                ) ?: return@launch
                vm.labels.value = vm.labels.value.applyDelta(delta) { it.id }
            }
        }
        cs.launch {
            vm.labels.collect { labels -> names.text = labels.joinToString { it.name }.ifEmpty { GiteaBundle.message("pull.request.create.none") } }
        }
        return metadataRow(GiteaBundle.message("pull.request.create.labels"), names, link)
    }

    private fun metadataRow(title: String, value: JComponent, edit: JComponent): JComponent =
        HorizontalListPanel(JBUI.scale(8)).apply {
            add(JBLabel(title).apply { foreground = UIUtil.getContextHelpForeground() })
            add(value)
            add(edit)
        }

    /** The current selection after [delta], matched by [key]: the picker's items are fresh copies. */
    private fun <T> List<T>.applyDelta(delta: CollectionDelta<T>, key: (T) -> Any): List<T> {
        val removed = delta.removedItems.mapTo(HashSet(), key)
        val kept = filterNot { key(it) in removed }
        val keptKeys = kept.mapTo(HashSet(), key)
        return kept + delta.newItems.filterNot { key(it) in keptKeys }
    }

    // ── Status & buttons ──────────────────────────────────────────────────

    private const val OPEN_EXISTING = "open-existing"

    private fun statusLine(cs: CoroutineScope, vm: GiteaPRCreateViewModel, onOpenPullRequest: (GiteaPullRequest) -> Unit): JComponent {
        var existing: GiteaPullRequest? = null
        val pane = SimpleHtmlPane(addBrowserListener = false).apply {
            addHyperlinkListener { e ->
                if (e.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED && e.description == OPEN_EXISTING) {
                    existing?.let(onOpenPullRequest)
                }
            }
        }
        cs.launch {
            combine(vm.check, vm.creation, vm.baseBranch, vm.headBranch) { check, creation, base, head ->
                existing = (check?.result?.getOrNull() as? GiteaPRCreateViewModel.Check.AlreadyExists)?.pullRequest
                statusHtml(check, creation, base, head, vm)
            }.collect { pane.setHtmlBody(it) }
        }
        return pane
    }

    private fun statusHtml(
        check: com.intellij.collaboration.util.ComputedResult<GiteaPRCreateViewModel.Check>?,
        creation: GiteaPRCreateViewModel.CreationState?,
        base: GitRemoteBranch?,
        head: GitBranch?,
        vm: GiteaPRCreateViewModel,
    ): String {
        when (creation) {
            GiteaPRCreateViewModel.CreationState.Pushing ->
                return GiteaBundle.message("pull.request.create.pushing", head?.name.orEmpty())
            GiteaPRCreateViewModel.CreationState.Creating ->
                return GiteaBundle.message("pull.request.create.creating")
            is GiteaPRCreateViewModel.CreationState.Failed ->
                return HtmlChunk.text(GiteaBundle.message("pull.request.create.failed", creation.error.localizedMessage.orEmpty()))
                    .wrapWith(HtmlChunk.span().attr("style", "color: #${UIUtil.colorToHex(JBUI.CurrentTheme.Validator.errorBorderColor())}"))
                    .toString()
            null -> Unit
        }
        if (base == null || head == null) return GiteaBundle.message("pull.request.create.select.branches")
        val result = check?.result ?: return GiteaBundle.message("pull.request.create.status.loading")
        val value = result.getOrElse { error ->
            return HtmlChunk.text(GiteaBundle.message("pull.request.create.status.error", error.localizedMessage.orEmpty())).toString()
        }
        return when (value) {
            GiteaPRCreateViewModel.Check.NoCommits ->
                HtmlChunk.text(GiteaBundle.message("pull.request.create.status.no.commits", head.name, base.nameForRemoteOperations)).toString()
            is GiteaPRCreateViewModel.Check.AlreadyExists -> HtmlBuilder()
                .append(GiteaBundle.message("pull.request.create.status.exists"))
                .append(" ")
                .append(HtmlChunk.link(OPEN_EXISTING, "#${value.pullRequest.number} ${value.pullRequest.title}"))
                .toString()
            is GiteaPRCreateViewModel.Check.Ready -> {
                val parts = buildList {
                    if (value.needsPush) add(GiteaBundle.message("pull.request.create.status.push", head.name, vm.mapping.gitRemote.name))
                    when (value.mergesCleanly) {
                        true -> add(GiteaBundle.message("pull.request.create.status.mergeable"))
                        false -> add(GiteaBundle.message("pull.request.create.status.conflicts", base.nameForRemoteOperations))
                        null -> Unit
                    }
                }
                HtmlChunk.text(parts.joinToString(" · ")).toString()
            }
        }
    }

    private fun buttons(cs: CoroutineScope, vm: GiteaPRCreateViewModel, onCancel: () -> Unit): JComponent {
        val create = JButton(GiteaBundle.message("pull.request.create.button")).apply {
            with(CollaborationToolsUIUtil) { isDefault = true }
            addActionListener { vm.create() }
        }
        cs.launch {
            combine(vm.check, vm.creation, vm.titleText) { check, creation, title ->
                check?.result?.getOrNull() is GiteaPRCreateViewModel.Check.Ready &&
                    (creation == null || creation is GiteaPRCreateViewModel.CreationState.Failed) &&
                    title.isNotBlank()
            }.collect { create.isEnabled = it }
        }
        val cancel = ActionLink(GiteaBundle.message("pull.request.create.cancel")) { onCancel() }
        return HorizontalListPanel(JBUI.scale(12)).apply {
            add(create)
            add(cancel)
        }
    }

    // ── Preview ───────────────────────────────────────────────────────────

    private fun preview(cs: CoroutineScope, vm: GiteaPRCreateViewModel): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            vm.comparison.collectLatest { result ->
                coroutineScope {
                    val content = when {
                        result == null -> centered(GiteaBundle.message("pull.request.create.select.branches"))
                        result.result == null -> CollaborationToolsUIUtil.moveToCenter(LoadingLabel())
                        else -> result.result!!.fold(
                            onSuccess = { comparison ->
                                if (comparison.commits.isEmpty()) centered(GiteaBundle.message("pull.request.create.preview.empty"))
                                else changesPanel(this, vm, comparison)
                            },
                            onFailure = { centered(GiteaBundle.message("pull.request.create.status.error", it.localizedMessage.orEmpty())) },
                        )
                    }
                    wrapper.setContent(content)
                    wrapper.revalidate()
                    wrapper.repaint()
                    awaitCancellation()
                }
            }
        }
        return wrapper
    }

    private fun changesPanel(cs: CoroutineScope, vm: GiteaPRCreateViewModel, comparison: GiteaPRLocalComparison): JComponent {
        val commitsVm = GiteaPRCreateCommitsViewModel(cs, comparison.commits)
        val commits = CodeReviewDetailsCommitsComponentFactory.create(cs, commitsVm) { commit ->
            CommitPresentation(
                titleHtml = HtmlChunk.text(commit.subject).toString(),
                descriptionHtml = HtmlChunk.text(commit.fullMessage.substringAfter('\n', "").trim()).toString(),
                author = commit.author.name,
                committedDate = Date(commit.commitTime),
            )
        }
        val tree = Wrapper()
        cs.launch {
            commitsVm.selectedCommit.collectLatest { commit ->
                coroutineScope {
                    val changes = if (commit == null) comparison.changes
                    else withContext(Dispatchers.IO) { GiteaPRLocalComparison.commitChanges(vm.project, vm.mapping.gitRepository.root, commit) }
                    val listVm = GiteaPRCreateChangeListViewModel(vm.project, changes)
                    val changesTree = CodeReviewChangeListComponentFactory.createIn(
                        this, listVm, CodeReviewProgressTreeModelFromDetails(this, listVm),
                        GiteaBundle.message("pull.request.create.preview.empty"),
                    )
                    withContext(Dispatchers.EDT) {
                        tree.setContent(ScrollPaneFactory.createScrollPane(changesTree, true))
                        tree.revalidate()
                        tree.repaint()
                    }
                    awaitCancellation()
                }
            }
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(Wrapper(commits).apply { border = JBUI.Borders.empty(8, 12) }, BorderLayout.NORTH)
            add(tree, BorderLayout.CENTER)
        }
    }

    private fun centered(text: String): JComponent =
        CollaborationToolsUIUtil.moveToCenter(JBLabel(text).apply { foreground = UIUtil.getContextHelpForeground() })
}
