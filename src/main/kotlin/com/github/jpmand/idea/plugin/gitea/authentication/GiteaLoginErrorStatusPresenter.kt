package com.github.jpmand.idea.plugin.gitea.authentication

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaTokenLoginPanelModel
import com.intellij.collaboration.auth.ui.login.LoginException
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.ExceptionUtil
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.util.swingAction
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Nls
import java.net.ConnectException
import javax.swing.Action
@Suppress("UnstableApiUsage")
internal class GiteaLoginErrorStatusPresenter(
  private val cs: CoroutineScope,
  private val model: GiteaTokenLoginPanelModel
) : ErrorStatusPresenter.HTML<Throwable> {

  override fun getErrorTitle(error: Throwable): @Nls String = ""

  override fun getHTMLBody(error: Throwable): @NlsSafe String {
    val builder = HtmlBuilder()
    when (error) {
      is ConnectException -> builder.append(CollaborationToolsBundle.message("clone.dialog.login.error.server"))
      is LoginException.UnsupportedServerVersion -> builder.customizeUnsupportedVersionError(error)
      is LoginException.InvalidTokenOrUnsupportedServerVersion -> builder.customizeInvalidTokenOrUnsupportedServerVersionError(error)
      is LoginException.AccountAlreadyExists -> builder.append(CollaborationToolsBundle.message("login.dialog.error.account.already.exists", error.username))
      is LoginException.AccountUsernameMismatch -> builder.append(CollaborationToolsBundle.message("login.dialog.error.account.username.mismatch", error.requiredUsername, error.username))
      else -> builder.append(ExceptionUtil.getPresentableMessage(error))
    }

    return builder.wrapWithHtmlBody().toString()
  }

  override fun getErrorAction(error: Throwable): Action? = when(error) {
    is LoginException.UnsupportedServerVersion,
    is LoginException.InvalidTokenOrUnsupportedServerVersion -> swingAction(CollaborationToolsBundle.message("login.via.git")) {
      cs.launch {
        model.tryGitAuthorization()
      }
    }
    else -> null
  }

  private fun HtmlBuilder.customizeUnsupportedVersionError(
    error: LoginException.UnsupportedServerVersion
  ): HtmlBuilder {
    val text = GiteaBundle.message("server.version.unsupported")
    val link = HtmlChunk.link(ErrorStatusPresenter.ERROR_ACTION_HREF, CollaborationToolsBundle.message("login.via.git"))

    return this
      .append(text).nbsp()
      .append(link)
  }

  private fun HtmlBuilder.customizeInvalidTokenOrUnsupportedServerVersionError(
    error: LoginException.InvalidTokenOrUnsupportedServerVersion
  ): HtmlBuilder {
    val text = GiteaBundle.message("invalid.token.or.server.version.unsupported")
    val linkAction = HtmlChunk.link(ErrorStatusPresenter.ERROR_ACTION_HREF, CollaborationToolsBundle.message("login.via.git"))
    val linkAdditionalText = GiteaBundle.message("invalid.token.or.server.version.unsupported.additional.text")

    val linkBuilder = HtmlBuilder()
      .append(linkAction).nbsp()
      .append(linkAdditionalText)

    return this
      .append(text)
      .append(HtmlChunk.p().attr("align", "left").child(linkBuilder.toFragment()))
  }
}
