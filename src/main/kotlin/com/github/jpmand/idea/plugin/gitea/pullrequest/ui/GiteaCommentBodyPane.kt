package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JEditorPane

/**
 * A comment body as the Conversation tab and the diff/editor inlays both show it: the text as-is
 * (escaped, line breaks kept) right away, replaced by its rendered markdown once [render] returns.
 * An empty body shows a grey, italic "no description" note instead.
 */
internal fun commentBodyPane(
    cs: CoroutineScope,
    body: String?,
    render: suspend (String) -> String? = { GiteaUtil.safeConvertMarkdownToHtml(it) },
): JEditorPane {
    if (body.isNullOrBlank()) {
        return SimpleHtmlPane(HtmlChunk.text(GiteaBundle.message("pull.request.timeline.no.body")).italic().toString()).apply {
            foreground = UIUtil.getContextHelpForeground()
        }
    }
    val pane = SimpleHtmlPane(plainTextHtml(body))
    cs.launch {
        val html = render(body) ?: return@launch
        withContext(Dispatchers.EDT) {
            pane.text = html
            pane.contentType = "text/html"
        }
    }
    return pane
}

/** [text] as HTML: escaped, with its line breaks kept. */
internal fun plainTextHtml(text: String): String =
    HtmlBuilder().appendWithSeparators(HtmlChunk.br(), text.lines().map { HtmlChunk.text(it) }).toString()
