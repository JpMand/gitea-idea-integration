package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * AnnotatedTag represents an annotated tag
 * @param message The message associated with the annotated tag
 * @param &#x60;object&#x60; 
 * @param sha The SHA hash of the annotated tag
 * @param tag The name of the annotated tag
 * @param tagger
 * @param url The URL to access the annotated tag
 * @param verification
 */
data class AnnotatedTag(
    /* The message associated with the annotated tag */
    val message: String? = null,
    val `object`: AnnotatedTagObject? = null,
    /* The SHA hash of the annotated tag */
    val sha: String? = null,
    /* The name of the annotated tag */
    val tag: String? = null,
    val tagger: CommitUser? = null,
    /* The URL to access the annotated tag */
    val url: String? = null,
    val verification: PayloadCommitVerification? = null,
)

