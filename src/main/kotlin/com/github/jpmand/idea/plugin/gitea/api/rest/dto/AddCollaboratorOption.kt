package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * AddCollaboratorOption options when adding a user as a collaborator of a repository
 * @param permission Permission level to grant the collaborator
 */
data class AddCollaboratorOption(
    /* Permission level to grant the collaborator */
    val permission: Permission? = null,
) {


    /**
     * Permission level to grant the collaborator
     * Values: READ,WRITE,ADMIN
     */
    enum class Permission(val value: String) {

        READ("read"),

        WRITE("write"),

        ADMIN("admin");

    }


}

