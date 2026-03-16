package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

class GiteaChooseAccountDialog
@JvmOverloads
constructor(
  project: Project?,
  parentComponent: Component?,
  accounts: Collection<GiteaAccount>,
  @Nls(capitalization = Nls.Capitalization.Sentence)
  descriptionText: String?,
  showHosts: Boolean,
  allowDefault: Boolean,
  @Nls(capitalization = Nls.Capitalization.Title)
  title: String = GiteaBundle.message("account.choose.title"),
  @Nls(capitalization = Nls.Capitalization.Title)
  okText: String = GiteaBundle.message("account.choose.button")
) : DialogWrapper(project, parentComponent, false, IdeModalityType.IDE) {

  private val myDescription: JTextArea? = descriptionText?.let {
    JTextArea().apply {
      minimumSize = Dimension(0, 0)
      text = it
      lineWrap = true
      wrapStyleWord = true
      isEditable = false
      isFocusable = false
      isOpaque = false
      border = null
      margin = JBInsets(0, 0, 0, 0)
    }
  }

  private val myAccountsList: JBList<GiteaAccount> = JBList(accounts).apply {
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    cellRenderer = object : ColoredListCellRenderer<GiteaAccount>() {
      override fun customizeCellRenderer(
        list: JList<out GiteaAccount>,
        value: GiteaAccount,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
      ) {
        append(value.name)
        if (showHosts) {
          append(" ${value.server}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
        border = JBUI.Borders.empty(0, UIUtil.DEFAULT_HGAP)
      }
    }
  }

  private val mySetDefaultCheckBox: JBCheckBox? = if (allowDefault) JBCheckBox(GiteaBundle.message("account.choose.as.default")) else null

  val myAccount: GiteaAccount get() = myAccountsList.selectedValue

  val mySetDefault: Boolean get() = mySetDefaultCheckBox?.isSelected ?: false

  init {
    this.title = title
    setOKButtonText(okText)
    init()
    myAccountsList.selectedIndex = 0
  }

  override fun getDimensionServiceKey(): @NonNls String? = "Gitea.Dialog.Accounts.Choose"

  override fun doValidate(): ValidationInfo? {
    return if (myAccountsList.selectedValue == null) ValidationInfo(GiteaBundle.message("account.choose.not.selected")) else null
  }

  override fun createCenterPanel(): JComponent {
    return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP).apply {
      myDescription?.run { ::addToTop }
    }.addToCenter(JBScrollPane(myAccountsList).apply {
      preferredSize = Dimension(150, 20 * (myAccountsList.itemsCount.plus(1)))
    })
      .apply {
        mySetDefaultCheckBox?.run { ::addToBottom }
      }
  }

  override fun getPreferredFocusedComponent() = myAccountsList
}