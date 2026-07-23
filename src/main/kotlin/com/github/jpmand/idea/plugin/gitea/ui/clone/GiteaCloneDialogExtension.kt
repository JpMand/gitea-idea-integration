package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.GiteaIcons
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneViewModelImpl
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.async.cancelledWith
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import javax.swing.Icon

class GiteaCloneDialogExtension : VcsCloneDialogExtension {
    override fun getName(): String = SERVICE_DISPLAY_NAME

    override fun getIcon(): Icon = GiteaIcons.Logo

    override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
        val accounts = service<GiteaAccountManager>().accountsState.value
        return if (accounts.isEmpty()) listOf(VcsCloneDialogExtensionStatusLine.greyText(CollaborationToolsBundle.message("clone.dialog.label.no.accounts")))
        else accounts.map { account -> VcsCloneDialogExtensionStatusLine.greyText( "$account") }
    }

    override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent =
        project.service<GiteaCloneDialogExtensionComponentFactory>().create(modalityState)
}

@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
internal class GiteaCloneDialogExtensionComponentFactory(private val project: Project, private val cs: CoroutineScope){
    fun create(modalityState : ModalityState) : GiteaCloneComponent {
        val vmCs = cs.childScope(javaClass.name, modalityState.asContextElement())
        val vm  = GiteaCloneViewModelImpl(project, vmCs + Dispatchers.Default, service<GiteaAccountManager>())

        val componentCs = vmCs.childScope("Gitea clone dialog component")
        val component = GiteaCloneComponent(project, componentCs, vm).also {
            vmCs.cancelledWith { it }
        }
        return component
    }
}