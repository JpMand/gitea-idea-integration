package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * A not-yet-submitted inline review comment, held entirely client-side until the whole batch is
 * submitted as one review (`POST .../pulls/{index}/reviews` — Gitea has no endpoint to add a
 * comment to an already-created review, pending or otherwise, so this must accumulate locally).
 *
 * [localId] is assigned client-side (never a server id) — used only to address a draft for
 * later edit/removal before it's ever sent.
 */
data class GiteaPRDraftComment(
    val localId: Long,
    val path: String,
    /** 1-indexed line number in the head (new) file. Mutually exclusive with [oldLine]. */
    val newLine: Int?,
    /** 1-indexed line number in the base (old) file. Mutually exclusive with [newLine]. */
    val oldLine: Int?,
    val body: String,
)
