package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneRepositoriesListViewModel
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneViewModel
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.util.swingAction
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.annotations.Nls
import javax.swing.JList

@Suppress("UnstableApiUsage")
internal class GiteaCloneListRenderer internal constructor(
    private val cloneVm: GiteaCloneViewModel,
    private val listVm: GiteaCloneRepositoriesListViewModel
) : ColoredListCellRenderer<GiteaCloneListItem>() {
    override fun customizeCellRenderer(
        list: JList<out GiteaCloneListItem?>,
        value: GiteaCloneListItem,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        clear()
        when (value) {
            is GiteaCloneListItem.Error -> {
                val cloneError = value.error
                val action = swingAction(
                    cloneError.name(), cloneError.performAction()
                )
                append(cloneError.message(), SimpleTextAttributes.ERROR_ATTRIBUTES)
                append(" ")
                append(cloneError.name(), SimpleTextAttributes.LINK_ATTRIBUTES, action)
            }

            is GiteaCloneListItem.Repository -> append(value.presentation())
        }
    }

    private fun GiteaCloneException.message(): @Nls String = when (this) {
        is GiteaCloneException.ConnectionError -> CollaborationToolsBundle.message("error.connection.error")
        is GiteaCloneException.MissingAccessToken -> CollaborationToolsBundle.message("account.token.missing")
        is GiteaCloneException.RevokedToken -> CollaborationToolsBundle.message("http.status.error.refresh.token")
        is GiteaCloneException.Unknown -> message
    }

    private fun GiteaCloneException.name(): @Nls String = when (this) {
        is GiteaCloneException.MissingAccessToken,
        is GiteaCloneException.RevokedToken,
            -> CollaborationToolsBundle.message("login.again.action.text")

        is GiteaCloneException.ConnectionError,
        is GiteaCloneException.Unknown,
            -> CollaborationToolsBundle.message("clone.dialog.error.retry")
    }

    private fun GiteaCloneException.performAction(): (Any) -> Unit {
        when (this) {
            is GiteaCloneException.MissingAccessToken,
            is GiteaCloneException.RevokedToken,
                -> return { cloneVm.switchToLoginPanel(account) }

            is GiteaCloneException.ConnectionError,
            is GiteaCloneException.Unknown,
                -> return { listVm.reload(account) }
        }
    }
}
