package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneLoginViewModel
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneRepositoriesViewModel
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneViewModel
import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.async.nestedDisposable
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.util.bindContentIn
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.components.panels.Wrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.yield
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
internal class GiteaCloneComponent(
    private val project: Project,
    parentCs: CoroutineScope,
    private val vm: GiteaCloneViewModel,
) : VcsCloneDialogExtensionComponent() {
    private val cs: CoroutineScope = parentCs.childScope(javaClass.name, Dispatchers.Main)

    private val wrapper: Wrapper = Wrapper().apply {
        bindContentIn(cs, vm.panelVm) { panelVm ->
            val innerCs = this
            when (panelVm) {
                is GiteaCloneLoginViewModel -> GiteaCloneLoginComponentFactory.create(
                    innerCs,
                    panelVm,
                    this@GiteaCloneComponent.vm
                )

                is GiteaCloneRepositoriesViewModel -> GiteaCloneRepositoriesComponentFactory.create(
                    project,
                    innerCs,
                    panelVm,
                    this@GiteaCloneComponent.vm
                )
                    .also { panel ->
                        panel.registerValidators(innerCs.nestedDisposable())
                        innerCs.launchNow {
                            panelVm.selectedUrl.collectLatest { selectedUrl ->
                                val isUrlSelected = selectedUrl != null
                                dialogStateListener.onOkActionEnabled(isUrlSelected)
                            }
                        }

                        innerCs.launchNow {
                            panelVm.listVm.allItems.collectLatest {
                                dialogStateListener.onListItemChanged()
                            }
                        }

                        innerCs.launchNow {
                            yield()
                            CollaborationToolsUIUtil.focusPanel(panel)
                        }
                    }
            }
        }
    }

    override fun getView(): JComponent = wrapper

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        this.vm.doClone(checkoutListener)
    }

    override fun doValidateAll(): List<ValidationInfo> =
        (wrapper.targetComponent as? DialogPanel)?.validationsOnApply?.values?.flatten()?.mapNotNull {
            it.validate()
        } ?: emptyList()

    override fun onComponentSelected() {
        dialogStateListener.onOkActionNameChanged(DvcsBundle.message("clone.button"))
        CollaborationToolsUIUtil.focusPanel(wrapper.targetComponent)
    }
}
