package com.github.jpmand.idea.plugin.gitea.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

/**
 * Panel displaying the list of Pull Requests
 */
class GiteaPullRequestsPanel(
  private val project: Project,
  cs: CoroutineScope
) : JPanel(BorderLayout()) {

  private val viewModel = GiteaPullRequestsViewModel(project, cs)
  private val listModel = DefaultListModel<GiteaPullRequest>()
  private val prList = JBList(listModel)
  private val loadingLabel = JBLabel("Loading...", AnimatedIcon.Default.INSTANCE, JLabel.CENTER)
  private val errorLabel = JBLabel("", JLabel.CENTER)
  private val emptyLabel = JBLabel("No pull requests found", JLabel.CENTER)
  private val scope = cs + Dispatchers.Main

  init {
    setupUI()
    observeViewModel()
    viewModel.loadPullRequests()
  }

  private fun setupUI() {
    // Create toolbar with refresh button
    val toolbar = JPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty(5)
      val refreshButton = JButton("Refresh").apply {
        addActionListener { viewModel.loadPullRequests() }
      }
      add(refreshButton, BorderLayout.WEST)
    }

    // Setup PR list
    prList.cellRenderer = PullRequestCellRenderer()
    prList.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val index = prList.locationToIndex(e.point)
          if (index >= 0) {
            val pr = listModel.getElementAt(index)
            BrowserUtil.browse(pr.htmlUrl)
          }
        }
      }
    })

    val scrollPane = JBScrollPane(prList)

    // Layout
    add(toolbar, BorderLayout.NORTH)
    add(scrollPane, BorderLayout.CENTER)

    // Initially hide status labels
    loadingLabel.isVisible = false
    errorLabel.isVisible = false
    emptyLabel.isVisible = false
  }

  private fun observeViewModel() {
    viewModel.state.onEach { state ->
      when (state) {
        is GiteaPullRequestsViewModel.PRState.Empty -> {
          showEmpty()
        }
        is GiteaPullRequestsViewModel.PRState.Loading -> {
          showLoading()
        }
        is GiteaPullRequestsViewModel.PRState.Success -> {
          showPullRequests(state.pullRequests)
        }
        is GiteaPullRequestsViewModel.PRState.Error -> {
          showError(state.message)
        }
      }
    }.launchIn(scope)
  }

  private fun showLoading() {
    listModel.clear()
    prList.isVisible = false
    loadingLabel.isVisible = true
    errorLabel.isVisible = false
    emptyLabel.isVisible = false

    removeStatusLabels()
    add(loadingLabel, BorderLayout.CENTER)
    revalidate()
    repaint()
  }

  private fun showPullRequests(pullRequests: List<GiteaPullRequest>) {
    removeStatusLabels()
    listModel.clear()
    pullRequests.forEach { listModel.addElement(it) }

    if (pullRequests.isEmpty()) {
      showEmpty()
    } else {
      prList.isVisible = true
      loadingLabel.isVisible = false
      errorLabel.isVisible = false
      emptyLabel.isVisible = false
    }
    revalidate()
    repaint()
  }

  private fun showError(message: String) {
    listModel.clear()
    prList.isVisible = false
    loadingLabel.isVisible = false
    errorLabel.text = message
    errorLabel.isVisible = true
    emptyLabel.isVisible = false

    removeStatusLabels()
    add(errorLabel, BorderLayout.CENTER)
    revalidate()
    repaint()
  }

  private fun showEmpty() {
    listModel.clear()
    prList.isVisible = false
    loadingLabel.isVisible = false
    errorLabel.isVisible = false
    emptyLabel.isVisible = true

    removeStatusLabels()
    add(emptyLabel, BorderLayout.CENTER)
    revalidate()
    repaint()
  }

  private fun removeStatusLabels() {
    remove(loadingLabel)
    remove(errorLabel)
    remove(emptyLabel)
  }

  fun dispose() {
    scope.cancel()
  }

  /**
   * Custom cell renderer for Pull Request items
   */
  private class PullRequestCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

      if (value is GiteaPullRequest) {
        val panel = JPanel(VerticalLayout(2)).apply {
          border = JBUI.Borders.empty(5)
          isOpaque = true
          background = if (isSelected) list?.selectionBackground else list?.background
        }

        // Title with ID
        val titleLabel = JBLabel("#${value.number}: ${value.title}").apply {
          font = font.deriveFont(font.style or java.awt.Font.BOLD)
        }

        // State
        val stateLabel = JBLabel("State: ${value.state ?: "unknown"}").apply {
          foreground = java.awt.Color.GRAY
        }

        panel.add(titleLabel)
        panel.add(stateLabel)

        return panel
      }

      return component
    }
  }
}
