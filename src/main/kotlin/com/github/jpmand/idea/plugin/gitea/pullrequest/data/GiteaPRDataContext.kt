package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryCoordinates
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount

/**
 * Holds the resolved context (account + repo coordinates + API) for PR operations. Without an
 * [account] the repository is read anonymously: [api] sends no credentials, and the UI leaves out
 * everything that writes.
 */
data class GiteaPRDataContext(
    val account: GiteaAccount?,
    val repo: GiteaRepositoryCoordinates,
    val api: GiteaApi,
) {
    /** Read-only: no account with a token on [repo]'s server. */
    val isAnonymous: Boolean get() = account == null

    /** Shared by every [GiteaPRRepository] built on this context (tool window, in-editor review),
     * so a change made through one is seen by the others' next load. Not part of equality. */
    internal val sharedLoads = GiteaSharedLoads()

    /** Avatars, shared by the PR list, the Details and Conversation tabs and the review UI, so each
     * is downloaded and decoded once. The most recent 256 are kept (about 37 KB each at 96 px). */
    val avatarImages = GiteaSharedLoads(reuseFor = Long.MAX_VALUE, maxEntries = 256)
}
