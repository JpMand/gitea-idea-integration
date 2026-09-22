package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty


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

        @JsonProperty("read") READ("read"),

        @JsonProperty("write") WRITE("write"),

        @JsonProperty("admin") ADMIN("admin");

    }


}

