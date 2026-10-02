package com.github.jpmand.idea.plugin.gitea.util

import com.intellij.markdown.utils.convertMarkdownToHtml


object GiteaUtil {
  const val SERVICE_NAME = "Gitea"
  const val SERVICE_DISPLAY_NAME = "Gitea"

  //Known issue: \n is considered soft break in CommonMark, this in turn causes html not to render new line for it.
  fun safeConvertMarkdownToHtml(markdown: String): String {
    return convertMarkdownToHtml(markdown)
  }
}