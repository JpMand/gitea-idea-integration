package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.util.CollectionDelta
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.CheckBoxList
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CompletableDeferred
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Checkbox multi-select popup for requesting/un-requesting PR reviewers — pre-checks
 * [currentlyRequested], and returns the [CollectionDelta] of what changed once the user clicks
 * Apply, or `null` if the popup is dismissed without applying.
 */
suspend fun showReviewersPicker(
    relativePoint: RelativePoint,
    candidates: List<GiteaUser>,
    currentlyRequested: Collection<GiteaUser>,
): CollectionDelta<GiteaUser>? = showCheckBoxPicker(
    relativePoint,
    GiteaBundle.message("pull.request.action.request.review"),
    GiteaBundle.message("pull.request.action.request.review.apply"),
    candidates,
    currentlyRequested,
    text = { user -> user.fullName?.let { "${user.login} ($it)" } ?: user.login },
    key = { it.login },
)

/**
 * Checkbox multi-select popup: pre-checks the [candidates] whose [key] is among [selected]'s, and
 * returns the [CollectionDelta] of what changed once the user clicks Apply, or `null` if the popup
 * is dismissed without applying.
 *
 * The platform's own multi-select chooser popups
 * ([com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.showAsyncMultipleChooserPopup]
 * and its incremental-loading sibling) are both `@ApiStatus.Internal`, which this project's
 * Marketplace-compatibility rule forbids referencing — so this is a small, self-contained popup
 * built on the public [CheckBoxList] instead.
 */
suspend fun <T : Any> showCheckBoxPicker(
    relativePoint: RelativePoint,
    @NlsContexts.PopupTitle title: String,
    @NlsContexts.Button applyText: String,
    candidates: List<T>,
    selected: Collection<T>,
    text: (T) -> String,
    key: (T) -> Any,
): CollectionDelta<T>? {
    val result = CompletableDeferred<CollectionDelta<T>?>()
    val selectedKeys = selected.mapTo(HashSet(), key)
    val selectedCandidates = candidates.filter { key(it) in selectedKeys }

    val checkBoxList = CheckBoxList<T>().apply {
        setItems(candidates) { text(it) }
        selectedCandidates.forEach { setItemSelected(it, true) }
    }

    lateinit var popup: JBPopup
    val applyButton = JButton(applyText).apply {
        with(CollaborationToolsUIUtil) { isDefault = true }
        addActionListener {
            result.complete(CollectionDelta(selectedCandidates, checkBoxList.getCheckedItems()))
            popup.closeOk(null)
        }
    }

    val panel = JPanel(BorderLayout()).apply {
        preferredSize = JBDimension(280, 320)
        add(ScrollPaneFactory.createScrollPane(checkBoxList, true), BorderLayout.CENTER)
        add(
            JPanel(BorderLayout()).apply {
                // A separator line above the footer, as popups with a bottom button bar have.
                border = JBUI.Borders.compound(JBUI.Borders.customLineTop(JBUI.CurrentTheme.Popup.separatorColor()), JBUI.Borders.empty(6, 8))
                add(applyButton, BorderLayout.EAST)
            },
            BorderLayout.SOUTH,
        )
    }

    popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(panel, checkBoxList)
        .setTitle(title)
        .setRequestFocus(true)
        .setResizable(true)
        .setMovable(true)
        .setCancelOnClickOutside(true)
        .createPopup()
    popup.addListener(object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            if (!result.isCompleted) result.complete(null)
        }
    })
    popup.show(relativePoint)

    return result.await()
}
