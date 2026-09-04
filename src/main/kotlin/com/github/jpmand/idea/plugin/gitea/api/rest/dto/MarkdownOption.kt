package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * MarkdownOption markdown options
 * @param context URL path for rendering issue, media and file links Expected format: /subpath/{user}/{repo}/src/{branch, commit, tag}/{identifier/path}/{file/dir}
 * @param mode Mode to render (markdown, comment, wiki, file)
 * @param text Text markdown to render
 * @param wiki Is it a wiki page? (use mode=wiki instead)
 */
data class MarkdownOption(
    /* URL path for rendering issue, media and file links Expected format: /subpath/{user}/{repo}/src/{branch, commit, tag}/{identifier/path}/{file/dir} */
    val context: String? = null,
    /* Mode to render (markdown, comment, wiki, file) */
    val mode: String? = null,
    /* Text markdown to render */
    val text: String? = null,
    /* Is it a wiki page? (use mode=wiki instead) */
    val wiki: Boolean? = null,
)

