package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.util.CollectionDelta
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.CheckBoxList
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CompletableDeferred
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Checkbox multi-select popup for requesting/un-requesting PR reviewers — pre-checks
 * [currentlyRequested], and returns the [CollectionDelta] of what changed once the user clicks
 * Apply, or `null` if the popup is dismissed without applying.
 *
 * The platform's own multi-select chooser popups
 * ([com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.showAsyncMultipleChooserPopup]
 * and its incremental-loading sibling) are both `@ApiStatus.Internal`, which this project's
 * Marketplace-compatibility rule forbids referencing — so this is a small, self-contained popup
 * built on the public [CheckBoxList] instead.
 */
suspend fun showReviewersPicker(
    relativePoint: RelativePoint,
    candidates: List<GiteaUser>,
    currentlyRequested: Collection<GiteaUser>,
): CollectionDelta<GiteaUser>? {
    val result = CompletableDeferred<CollectionDelta<GiteaUser>?>()

    val checkBoxList = CheckBoxList<GiteaUser>().apply {
        setItems(candidates) { user -> user.fullName?.let { "${user.login} ($it)" } ?: user.login }
        candidates.forEach { user -> setItemSelected(user, user in currentlyRequested) }
    }

    lateinit var popup: JBPopup
    val applyButton = JButton(GiteaBundle.message("pull.request.action.request.review.apply")).apply {
        addActionListener {
            result.complete(CollectionDelta(currentlyRequested, checkBoxList.getCheckedItems()))
            popup.closeOk(null)
        }
    }

    val panel = JPanel(BorderLayout()).apply {
        preferredSize = Dimension(JBUI.scale(280), JBUI.scale(320))
        add(JBScrollPane(checkBoxList), BorderLayout.CENTER)
        add(
            JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4)
                add(applyButton, BorderLayout.EAST)
            },
            BorderLayout.SOUTH,
        )
    }

    popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(panel, checkBoxList)
        .setTitle(GiteaBundle.message("pull.request.action.request.review"))
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
