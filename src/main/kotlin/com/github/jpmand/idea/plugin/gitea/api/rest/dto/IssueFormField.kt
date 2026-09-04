package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * IssueFormField represents a form field
 * @param attributes
 * @param id
 * @param type
 * @param validations
 * @param visible
 */
data class IssueFormField(
    val attributes: Map<String, Any>? = null,
    val id: String? = null,
    val type: Type? = null,
    val validations: Map<String, Any>? = null,
    val visible: Array<Visible>? = null,
) {


    /**
     *
     * Values: MARKDOWN,TEXTAREA,INPUT,DROPDOWN,CHECKBOXES
     */
    enum class Type(val value: String) {

        MARKDOWN("markdown"),

        TEXTAREA("textarea"),

        INPUT("input"),

        DROPDOWN("dropdown"),

        CHECKBOXES("checkboxes");

    }


    /**
     *
     * Values: FORM,CONTENT
     */
    enum class Visible(val value: String) {

        FORM("form"),

        CONTENT("content");

    }


}

