package com.github.jpmand.idea.plugin.gitea.util

import com.intellij.markdown.utils.convertMarkdownToHtml
import com.intellij.openapi.diagnostic.Logger


object GiteaUtil {
  @JvmField
  val LOG = Logger.getInstance("gitea")
  const val SERVICE_NAME = "Gitea"
  const val SERVICE_DISPLAY_NAME = "Gitea"


  fun safeConvertMarkdownToHtml(markdown: String): String {
    //TODO: Fix newline issue
    // for some reason sometimes \n is kept as is instead of being converted into <p> paragraphs. (e.g "test\nwith\nnewlines" becomes <p>test\nwith\nnewlines</p> instead of <p>test</p><p>with</p><p>newlines</p>)
    val unixNormalized = markdown.replace("\r\n", "\n")
    return convertMarkdownToHtml(unixNormalized)
  }
}