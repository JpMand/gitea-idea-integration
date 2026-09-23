package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.util.ui.cloneDialog.VcsCloneWithExtensionAction
import kotlin.jvm.java

class GiteaCloneAction: VcsCloneWithExtensionAction() {
    override fun getExtension(): Class<out VcsCloneDialogExtension> = GiteaCloneDialogExtension::class.java
}