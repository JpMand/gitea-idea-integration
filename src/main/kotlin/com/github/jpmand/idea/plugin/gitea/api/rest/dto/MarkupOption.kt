package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * MarkupOption markup options
 * @param context URL path for rendering issue, media and file links Expected format: /subpath/{user}/{repo}/src/{branch, commit, tag}/{identifier/path}/{file/dir}
 * @param filePath File path for detecting extension in file mode
 * @param mode Mode to render (markdown, comment, wiki, file)
 * @param text Text markup to render
 * @param wiki Is it a wiki page? (use mode=wiki instead)
 */
data class MarkupOption(
    /* URL path for rendering issue, media and file links Expected format: /subpath/{user}/{repo}/src/{branch, commit, tag}/{identifier/path}/{file/dir} */
    val context: String? = null,
    /* File path for detecting extension in file mode */
    val filePath: String? = null,
    /* Mode to render (markdown, comment, wiki, file) */
    val mode: String? = null,
    /* Text markup to render */
    val text: String? = null,
    /* Is it a wiki page? (use mode=wiki instead) */
    val wiki: Boolean? = null,
)

