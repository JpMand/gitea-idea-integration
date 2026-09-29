package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaReviewVerdict
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.reviewSubmitProblem
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPanelFactory
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.codereview.review.CodeReviewSubmitViewModel
import com.intellij.collaboration.ui.util.bindChildIn
import com.intellij.collaboration.ui.util.bindDisabledIn
import com.intellij.collaboration.ui.util.bindTextIn
import com.intellij.collaboration.ui.util.bindVisibilityIn
import com.intellij.collaboration.ui.util.popup.awaitClose
import com.intellij.icons.AllIcons
import com.intellij.ide.setToolTipText
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.InlineIconButton
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Backs [GiteaSubmitReviewPopup]: a review body plus a verdict, submitted through
 * [GiteaPRDiscussionsViewModels.submit] (which finishes a pending review or sends the local
 * drafts as a new one). [onDone] closes the popup; it runs after a successful submission, a
 * confirmed discard, or a cancel.
 */
@Suppress("UnstableApiUsage")
internal class GiteaSubmitReviewViewModel(
    private val project: Project,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val onDone: () -> Unit,
) : CodeReviewSubmitViewModel {

    val viewerIsAuthor: Boolean get() = discussionsVm.viewerIsAuthor

    /** Comments that go out with the review: local drafts, or those already on a pending review. */
    override val draftCommentsCount: StateFlow<Int> =
        discussionsVm.pendingReview.value?.let { MutableStateFlow(it.commentsCount).asStateFlow() }
            ?: discussionsVm.draftCommentsCount

    /** Shared with every opening of the popup for this PR, so what was typed survives it closing. */
    override val text: MutableStateFlow<String> = discussionsVm.submitReviewText.also {
        if (it.value.isBlank()) it.value = discussionsVm.pendingReview.value?.body.orEmpty()
    }

    override val isBusy: StateFlow<Boolean> get() = discussionsVm.isSubmittingReview

    private val _error = MutableStateFlow<Throwable?>(null)
    override val error: StateFlow<Throwable?> = _error.asStateFlow()

    /** Whether [verdict] can be submitted with the current body and comments. */
    fun canSubmit(verdict: GiteaReviewVerdict): Flow<Boolean> =
        combine(text, draftCommentsCount) { body, count -> reviewSubmitProblem(verdict, body, count) == null }

    fun submit(verdict: GiteaReviewVerdict) {
        _error.value = null
        discussionsVm.submit(verdict, text.value, onSuccess = onDone, onError = { _error.value = it })
    }

    /** Asks for confirmation, then discards the drafts and any pending review. The popup may close
     * while the confirmation has focus; declining keeps what was typed for its next opening. */
    fun discard() {
        if (confirmAndCancelReview(project, discussionsVm)) onDone()
    }

    override fun cancel() = onDone()
}

/**
 * The review-submit popup: a title with the pending comment count, discard and close buttons, a
 * body editor, an error line and Gitea's verdict buttons. Same layout as the platform's popup the
 * GitHub plugin uses, which is internal API and so can't be extended here.
 */
@Suppress("UnstableApiUsage")
internal object GiteaSubmitReviewPopup {
    // 12px gaps minus the buttons' own 3px borders, as in the platform popup.
    private const val ACTIONS_GAP = 6
    private const val TITLE_ACTIONS_GAP = 5

    /** Shows the popup under [parentComponent] and suspends until it closes. */
    suspend fun show(vm: GiteaSubmitReviewViewModel, parentComponent: Component) =
        showPopup(vm) { it.showUnderneathOf(parentComponent) }

    /** Shows the popup centered in [project]'s window and suspends until it closes. */
    suspend fun show(vm: GiteaSubmitReviewViewModel, project: Project) =
        showPopup(vm) { it.showCenteredInCurrentWindow(project) }

    private suspend fun showPopup(vm: GiteaSubmitReviewViewModel, show: (JBPopup) -> Unit) {
        withContext(Dispatchers.EDT) {
            val editor = createEditor(this, vm.text)
            val popup = JBPopupFactory.getInstance()
                // the popup needs a focusable component to focus; it won't look inside a panel
                .createComponentPopupBuilder(createPanel(this, vm, editor), editor)
                .setFocusable(true)
                .setRequestFocus(true)
                .setResizable(true)
                .createPopup()
            show(popup)
            popup.awaitClose()
        }
    }

    private fun createPanel(cs: CoroutineScope, vm: GiteaSubmitReviewViewModel, editor: EditorTextField): JComponent {
        val titleLabel = JLabel(CollaborationToolsBundle.message("review.submit.review.title")).apply {
            font = JBFont.label().asBold()
        }
        val titlePanel = JPanel(HorizontalLayout(TITLE_ACTIONS_GAP)).apply {
            isOpaque = false
            add(titleLabel, HorizontalLayout.LEFT)
            bindChildIn(cs, vm.draftCommentsCount, HorizontalLayout.LEFT, 1) {
                if (it <= 0) null else JLabel(CollaborationToolsBundle.message("review.pending.comments.count", it))
            }
            add(createTitleActions(cs, vm), HorizontalLayout.RIGHT)
        }
        val errorPanel = ErrorStatusPanelFactory.create(cs, vm.error, errorPresenter, ErrorStatusPanelFactory.Alignment.LEFT)

        return JPanel(MigLayout(LC().insets("12").fill().flowY().noGrid().hideMode(3))).apply {
            background = JBUI.CurrentTheme.Popup.BACKGROUND
            preferredSize = JBDimension(500, 200)

            add(titlePanel, CC().growX())
            add(editor, CC().growX().growY())
            add(errorPanel, CC().growY().growPrioY(0))
            add(createActions(cs, vm), CC())
        }
    }

    private fun createEditor(cs: CoroutineScope, text: MutableStateFlow<String>): EditorTextField =
        EditorTextField(text.value, null, FileTypes.PLAIN_TEXT).apply {
            setOneLineMode(false)
            setPlaceholder(CollaborationToolsBundle.message("review.comment.placeholder"))
            addSettingsProvider {
                it.settings.isUseSoftWraps = true
                it.setVerticalScrollbarVisible(true)
                it.scrollPane.viewportBorder = JBUI.Borders.emptyLeft(4)
                it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
            }
            document.bindTextIn(cs, text)
        }

    private fun createActions(cs: CoroutineScope, vm: GiteaSubmitReviewViewModel): JComponent {
        fun verdictButton(bundleKey: String, verdict: GiteaReviewVerdict): JButton =
            JButton(GiteaBundle.message(bundleKey)).apply {
                isOpaque = false
                bindDisabledIn(cs, combine(vm.isBusy, vm.canSubmit(verdict)) { busy, ok -> busy || !ok })
                addActionListener { vm.submit(verdict) }
            }
        return HorizontalListPanel(ACTIONS_GAP).apply {
            // Gitea rejects approving or requesting changes on your own PR.
            if (!vm.viewerIsAuthor) {
                add(verdictButton("pull.request.action.approve", GiteaReviewVerdict.APPROVE))
                add(verdictButton("pull.request.action.request.changes", GiteaReviewVerdict.REQUEST_CHANGES).apply {
                    setToolTipText(HtmlChunk.text(GiteaBundle.message("pull.request.review.submit.error.request.changes.body")))
                })
            }
            add(verdictButton("pull.request.action.comment", GiteaReviewVerdict.COMMENT))
        }
    }

    private fun createTitleActions(cs: CoroutineScope, vm: GiteaSubmitReviewViewModel): JComponent {
        val discard = InlineIconButton(
            icon = CollaborationToolsIcons.Delete,
            hoveredIcon = CollaborationToolsIcons.DeleteHovered,
            tooltip = GiteaBundle.message("pull.request.action.cancel.review"),
        ).apply {
            border = JBUI.Borders.empty(5)
            bindDisabledIn(cs, vm.isBusy)
            bindVisibilityIn(cs, vm.draftCommentsCount.map { it > 0 })
            actionListener = ActionListener { vm.discard() }
        }
        val close = InlineIconButton(
            icon = AllIcons.Actions.Close,
            hoveredIcon = AllIcons.Actions.CloseHovered,
        ).apply {
            border = JBUI.Borders.empty(5)
            actionListener = ActionListener { vm.cancel() }
        }
        return HorizontalListPanel(TITLE_ACTIONS_GAP).apply {
            add(discard)
            add(close)
        }
    }

    private val errorPresenter: ErrorStatusPresenter<Throwable> by lazy {
        ErrorStatusPresenter.simpleHTML(
            CollaborationToolsBundle.message("review.submit.failed"),
            descriptionProvider = { it.localizedMessage },
        )
    }
}
