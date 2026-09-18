package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * Team represents a team in an organization
 * @param canCreateOrgRepo Whether the team can create repositories in the organization
 * @param description The description of the team
 * @param id The unique identifier of the team
 * @param includesAllRepositories Whether the team has access to all repositories in the organization
 * @param name The name of the team
 * @param organization
 * @param permission
 * @param units Deprecated: This variable should be replaced by UnitsMap and will be dropped in later versions.
 * @param unitsMap
 * @param visibility Team visibility within the organization. \"private\" teams are only listable by members and org owners; \"limited\" teams are listable by any organization member; \"public\" teams are listable by any signed-in user.
 */
data class Team(
    /* Whether the team can create repositories in the organization */
    val canCreateOrgRepo: Boolean? = null,
    /* The description of the team */
    val description: String? = null,
    /* The unique identifier of the team */
    val id: Long? = null,
    /* Whether the team has access to all repositories in the organization */
    val includesAllRepositories: Boolean? = null,
    /* The name of the team */
    val name: String? = null,
    val organization: Organization? = null,
    val permission: Permission? = null,
    /* Deprecated: This variable should be replaced by UnitsMap and will be dropped in later versions. */
    val units: Array<String>? = null,
    val unitsMap: Map<String, String>? = null,
    /* Team visibility within the organization. \"private\" teams are only listable by members and org owners; \"limited\" teams are listable by any organization member; \"public\" teams are listable by any signed-in user. */
    val visibility: Visibility? = null,
) {


    /**
     *
     * Values: NONE,READ,WRITE,ADMIN,OWNER
     */
    enum class Permission(val value: String) {

        NONE("none"),

        READ("read"),

        WRITE("write"),

        ADMIN("admin"),

        OWNER("owner");

    }


    /**
     * Team visibility within the organization. \"private\" teams are only listable by members and org owners; \"limited\" teams are listable by any organization member; \"public\" teams are listable by any signed-in user.
     * Values: PUBLIC,LIMITED,PRIVATE
     */
    enum class Visibility(val value: String) {

        PUBLIC("public"),

        LIMITED("limited"),

        PRIVATE("private");

    }


}

