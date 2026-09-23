package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPanelFactory
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.util.swingAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * A banner that is collapsed while [errorFlow] holds null and, when it holds a throwable, shows
 * [title] plus that throwable's message (see [com.github.jpmand.idea.plugin.gitea.api.GiteaHttpError]
 * for the friendly messages) and a Retry link.
 */
@Suppress("UnstableApiUsage")
internal fun giteaReviewErrorPanel(
  cs: CoroutineScope,
  errorFlow: Flow<Throwable?>,
  @Nls title: String,
  onRetry: () -> Unit,
): JComponent =
  ErrorStatusPanelFactory.create(
    cs,
    errorFlow,
    ErrorStatusPresenter.simple(
      title,
      descriptionProvider = { it.message },
      actionProvider = { swingAction(GiteaBundle.message("pull.request.error.retry")) { onRetry() } },
    ),
  )
