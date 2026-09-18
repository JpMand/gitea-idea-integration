package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditTeamOption options for editing a team
 * @param canCreateOrgRepo Whether the team can create repositories in the organization
 * @param description The description of the team
 * @param includesAllRepositories Whether the team has access to all repositories in the organization
 * @param name
 * @param permission
 * @param units Deprecated: This variable should be replaced by UnitsMap and will be dropped in later versions.
 * @param unitsMap
 * @param visibility Team visibility within the organization. When omitted, visibility is left unchanged.
 */
data class EditTeamOption(
    /* Whether the team can create repositories in the organization */
    val canCreateOrgRepo: Boolean? = null,
    /* The description of the team */
    val description: String? = null,
    /* Whether the team has access to all repositories in the organization */
    val includesAllRepositories: Boolean? = null,
    val name: String,
    val permission: Permission? = null,
    /* Deprecated: This variable should be replaced by UnitsMap and will be dropped in later versions. */
    val units: Array<String>? = null,
    val unitsMap: Map<String, String>? = null,
    /* Team visibility within the organization. When omitted, visibility is left unchanged. */
    val visibility: Visibility? = null,
) {


    /**
     *
     * Values: READ,WRITE,ADMIN
     */
    enum class Permission(val value: String) {

        READ("read"),

        WRITE("write"),

        ADMIN("admin");

    }


    /**
     * Team visibility within the organization. When omitted, visibility is left unchanged.
     * Values: PUBLIC,LIMITED,PRIVATE
     */
    enum class Visibility(val value: String) {

        PUBLIC("public"),

        LIMITED("limited"),

        PRIVATE("private");

    }


}

