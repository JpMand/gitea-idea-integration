package com.github.jpmand.idea.plugin.gitea.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.loadImage
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

@Suppress("UnstableApiUsage")
class GiteaImageLoader(
    private val api: GiteaApi
) : AsyncImageIconsProvider.AsyncImageLoader<GiteaUser>, AsyncHtmlImageLoader {
    override suspend fun load(key: GiteaUser): Image? =
        key.avatarUrl?.let { avatarUrl ->
            val actualUri = when {
                avatarUrl.startsWith(URLUtil.HTTP_PROTOCOL) -> avatarUrl
                avatarUrl.startsWith("/avatar") -> "https://secure.gravatar.com/avatar/$avatarUrl?d=identicon&s=$LOADED_GRAVATAR_SIZE"
                else -> api.server.restApiUri().resolveRelative(avatarUrl).toString()
            }
            load(null, actualUri)
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