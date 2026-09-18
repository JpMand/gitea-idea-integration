package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * NotificationSubject contains the notification subject (Issue/Pull/Commit)
 * @param htmlUrl HTMLURL is the web URL for the notification subject
 * @param latestCommentHtmlUrl LatestCommentHTMLURL is the web URL for the latest comment
 * @param latestCommentUrl LatestCommentURL is the API URL for the latest comment
 * @param state State indicates the current state of the notification subject
 * @param title Title is the title of the notification subject
 * @param type Type indicates the type of the notification subject
 * @param url URL is the API URL for the notification subject
 */
data class NotificationSubject(
    /* HTMLURL is the web URL for the notification subject */
    val htmlUrl: String? = null,
    /* LatestCommentHTMLURL is the web URL for the latest comment */
    val latestCommentHtmlUrl: String? = null,
    /* LatestCommentURL is the API URL for the latest comment */
    val latestCommentUrl: String? = null,
    /* State indicates the current state of the notification subject */
    val state: State? = null,
    /* Title is the title of the notification subject */
    val title: String? = null,
    /* Type indicates the type of the notification subject */
    val type: Type? = null,
    /* URL is the API URL for the notification subject */
    val url: String? = null,
) {


    /**
     * State indicates the current state of the notification subject
     * Values: OPEN,CLOSED,MERGED
     */
    enum class State(val value: String) {

        OPEN("open"),

        CLOSED("closed"),

        MERGED("merged");

    }


    /**
     * Type indicates the type of the notification subject
     * Values: ISSUE,PULL,COMMIT,REPOSITORY
     */
    enum class Type(val value: String) {

        ISSUE("Issue"),

        PULL("Pull"),

        COMMIT("Commit"),

        REPOSITORY("Repository");

    }


}

