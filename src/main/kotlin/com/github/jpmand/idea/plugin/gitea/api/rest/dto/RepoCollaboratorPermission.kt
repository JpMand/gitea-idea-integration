package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * RepoCollaboratorPermission to get repository permission for a collaborator
 * @param permission Permission level of the collaborator
 * @param roleName RoleName is the name of the permission role
 * @param user
 */
data class RepoCollaboratorPermission(
    /* Permission level of the collaborator */
    val permission: Permission? = null,
    /* RoleName is the name of the permission role */
    val roleName: String? = null,
    val user: User? = null,
) {


    /**
     * Permission level of the collaborator
     * Values: NONE,READ,WRITE,ADMIN,OWNER
     */
    enum class Permission(val value: String) {

        NONE("none"),

        READ("read"),

        WRITE("write"),

        ADMIN("admin"),

        OWNER("owner");

    }


}

