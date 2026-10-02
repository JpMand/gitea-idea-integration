package com.github.jpmand.idea.plugin.gitea.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.loadImage
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaSharedLoads
import com.intellij.collaboration.ui.html.AsyncHtmlImageLoader
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.util.resolveRelative
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.IconUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.ui.ImageUtil
import icons.CollaborationToolsIcons
import java.awt.Image
import java.net.URI
import java.net.URL
import javax.swing.Icon
import kotlin.coroutines.cancellation.CancellationException

private val LOG = logger<GiteaImageLoader>()

private const val LOADED_GRAVATAR_SIZE: Int = 80

/** Avatars are shown at 40 px at most; this keeps them sharp up to 2.4x scaling. Gitea serves them at
 * 512 px, about 1 MB each once decoded. */
private const val AVATAR_SIZE: Int = 96

@Suppress("UnstableApiUsage")
class GiteaImageLoader(
    private val api: GiteaApi,
    /** Where avatars are kept, see [com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext.avatarImages]. */
    private val avatars: GiteaSharedLoads? = null,
) : AsyncImageIconsProvider.AsyncImageLoader<GiteaUser>, AsyncHtmlImageLoader {
    override suspend fun load(key: GiteaUser): Image? =
        key.avatarUrl?.let { avatarUrl ->
            val actualUri = when {
                avatarUrl.startsWith(URLUtil.HTTP_PROTOCOL) -> avatarUrl
                avatarUrl.startsWith("/avatar") -> "https://secure.gravatar.com/avatar/$avatarUrl?d=identicon&s=$LOADED_GRAVATAR_SIZE"
                else -> api.server.restApiUri().resolveRelative(avatarUrl).toString()
            }
            avatars?.load(actualUri) { loadAvatar(actualUri) } ?: loadAvatar(actualUri)
        }

    private suspend fun loadAvatar(uri: String): Image {
        val image = load(null, uri)
        val width = image.getWidth(null)
        return if (width > AVATAR_SIZE) ImageUtil.scaleImage(image, AVATAR_SIZE, AVATAR_SIZE * image.getHeight(null) / width) else image
    }

    override suspend fun load(baseUrl: URL?, src: String): Image =
        try {
            val uri = URI.create(src)
            api.rest.loadImage(uri.toString())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            LOG.warn("Failed to load the image from src $src", e)
            throw e
        }

    override fun createBaseIcon(key: GiteaUser?, iconSize: Int): Icon =
        IconUtil.resizeSquared(CollaborationToolsIcons.Review.DefaultAvatar, iconSize)

    override suspend fun postProcess(image: Image): Image =
        ImageUtil.createCircleImage(ImageUtil.toBufferedImage(image))
}