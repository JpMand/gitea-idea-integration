package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * ChangeFileOperation for creating, updating or deleting a file
 * @param content new or updated file content, it must be base64 encoded
 * @param fromPath old path of the file to move
 * @param operation indicates what to do with the file: \"create\" for creating a new file, \"update\" for updating an existing file, \"upload\" for creating or updating a file, \"rename\" for renaming a file, and \"delete\" for deleting an existing file.
 * @param path path to the existing or new file
 * @param sha the blob ID (SHA) for the file that already exists, required for changing existing files
 */
data class ChangeFileOperation(
    /* new or updated file content, it must be base64 encoded */
    val content: String? = null,
    /* old path of the file to move */
    val fromPath: String? = null,
    /* indicates what to do with the file: \"create\" for creating a new file, \"update\" for updating an existing file, \"upload\" for creating or updating a file, \"rename\" for renaming a file, and \"delete\" for deleting an existing file. */
    val operation: Operation,
    /* path to the existing or new file */
    val path: String,
    /* the blob ID (SHA) for the file that already exists, required for changing existing files */
    val sha: String? = null,
) {


    /**
     * indicates what to do with the file: \"create\" for creating a new file, \"update\" for updating an existing file, \"upload\" for creating or updating a file, \"rename\" for renaming a file, and \"delete\" for deleting an existing file.
     * Values: CREATE,UPDATE,UPLOAD,RENAME,DELETE
     */
    enum class Operation(val value: String) {

        @JsonProperty("create") CREATE("create"),

        @JsonProperty("update") UPDATE("update"),

        @JsonProperty("upload") UPLOAD("upload"),

        @JsonProperty("rename") RENAME("rename"),

        @JsonProperty("delete") DELETE("delete");

    }


}

