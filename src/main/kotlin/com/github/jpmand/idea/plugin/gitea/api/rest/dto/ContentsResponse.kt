package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * ContentsResponse contains information about a repo's entry's (dir, file, symlink, submodule) metadata and content
 * @param links
 * @param content `content` is populated when `type` is `file`, otherwise null
 * @param downloadUrl DownloadURL is the direct download URL for this file
 * @param encoding `encoding` is populated when `type` is `file`, otherwise null
 * @param gitUrl GitURL is the Git API URL for this blob or tree
 * @param htmlUrl HTMLURL is the web URL for this file or directory
 * @param lastAuthorDate
 * @param lastCommitMessage LastCommitMessage is the message of the last commit that affected this file
 * @param lastCommitSha LastCommitSHA is the SHA of the last commit that affected this file
 * @param lastCommitterDate
 * @param lfsOid LfsOid is the Git LFS object ID if this file is stored in LFS
 * @param lfsSize LfsSize is the file size if this file is stored in LFS
 * @param name Name is the file or directory name
 * @param path Path is the full path to the file or directory
 * @param sha SHA is the Git blob or tree SHA
 * @param size Size is the file size in bytes
 * @param submoduleGitUrl `submodule_git_url` is populated when `type` is `submodule`, otherwise null
 * @param target `target` is populated when `type` is `symlink`, otherwise null
 * @param type `type` will be `file`, `dir`, `symlink`, or `submodule`
 * @param url URL is the API URL for this file or directory
 */
data class ContentsResponse(
    val links: FileLinksResponse? = null,
    /* `content` is populated when `type` is `file`, otherwise null */
    val content: String? = null,
    /* DownloadURL is the direct download URL for this file */
    val downloadUrl: String? = null,
    /* `encoding` is populated when `type` is `file`, otherwise null */
    val encoding: String? = null,
    /* GitURL is the Git API URL for this blob or tree */
    val gitUrl: String? = null,
    /* HTMLURL is the web URL for this file or directory */
    val htmlUrl: String? = null,
    val lastAuthorDate: OffsetDateTime? = null,
    /* LastCommitMessage is the message of the last commit that affected this file */
    val lastCommitMessage: String? = null,
    /* LastCommitSHA is the SHA of the last commit that affected this file */
    val lastCommitSha: String? = null,
    val lastCommitterDate: OffsetDateTime? = null,
    /* LfsOid is the Git LFS object ID if this file is stored in LFS */
    val lfsOid: String? = null,
    /* LfsSize is the file size if this file is stored in LFS */
    val lfsSize: Long? = null,
    /* Name is the file or directory name */
    val name: String? = null,
    /* Path is the full path to the file or directory */
    val path: String? = null,
    /* SHA is the Git blob or tree SHA */
    val sha: String? = null,
    /* Size is the file size in bytes */
    val size: Long? = null,
    /* `submodule_git_url` is populated when `type` is `submodule`, otherwise null */
    val submoduleGitUrl: String? = null,
    /* `target` is populated when `type` is `symlink`, otherwise null */
    val target: String? = null,
    /* `type` will be `file`, `dir`, `symlink`, or `submodule` */
    val type: String? = null,
    /* URL is the API URL for this file or directory */
    val url: String? = null,
)

