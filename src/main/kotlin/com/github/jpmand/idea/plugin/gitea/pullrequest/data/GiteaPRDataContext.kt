package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryCoordinates
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount

/** Holds the resolved context (account + repo coordinates + authenticated API) for PR operations. */
data class GiteaPRDataContext(
    val account: GiteaAccount,
    val repo: GiteaRepositoryCoordinates,
    val api: GiteaApi,
) {
    /** Shared by every [GiteaPRRepository] built on this context (tool window, in-editor review),
     * so a change made through one is seen by the others' next load. Not part of equality. */
    internal val sharedLoads = GiteaSharedLoads()

    /** Avatars, kept for the context's lifetime and shared by the PR list, the Details and
     * Conversation tabs and the review UI, so each is downloaded and decoded once. */
    val avatarImages = GiteaSharedLoads(reuseFor = Long.MAX_VALUE)
}
