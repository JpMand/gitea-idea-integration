package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CommitAffectedFiles store information about files affected by the commit
 * @param filename Filename is the path of the affected file
 * @param status Status indicates how the file was affected (added, modified, deleted)
 */
data class CommitAffectedFiles(
    /* Filename is the path of the affected file */
    val filename: String? = null,
    /* Status indicates how the file was affected (added, modified, deleted) */
    val status: String? = null,
)

