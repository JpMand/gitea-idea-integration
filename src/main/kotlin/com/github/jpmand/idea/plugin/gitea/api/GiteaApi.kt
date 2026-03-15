package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.HttpApiHelper
import com.intellij.collaboration.api.httpclient.CompoundRequestConfigurer
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.api.httpclient.HttpRequestConfigurer
import com.intellij.collaboration.api.httpclient.RequestTimeoutConfigurer
import com.intellij.collaboration.api.json.JsonHttpApiHelper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpSecurityUtil
import java.net.URI
import java.net.http.HttpRequest

private val LOG: Logger = logger<GiteaApi>()
@Suppress("UnstableApiUsage")
interface GiteaApi : HttpApiHelper {
  val server: GiteaServerPath
  val rest: Rest

  interface Rest : JsonHttpApiHelper, GiteaApi
}
@Suppress("UnstableApiUsage")
internal class GiteaApiImpl(
  override val server: GiteaServerPath,
  httpHelper: HttpApiHelper
) : GiteaApi, HttpApiHelper by httpHelper {
  constructor(server: GiteaServerPath, tokenSupplier: (() -> String)? = null)
          : this(server, tokenSupplier?.let { httpHelper(server, it) } ?: httpHelper())

  override val rest: GiteaApi.Rest =
    RestImpl(
      JsonHttpApiHelper(
        logger<GiteaApi>(),
        this,
        GiteaJsonDeSerializer,
        GiteaJsonDeSerializer
      )
    )

  private inner class RestImpl(helper: JsonHttpApiHelper) : GiteaApi by this, GiteaApi.Rest, JsonHttpApiHelper by helper
}
@Suppress("UnstableApiUsage")
private fun httpHelper(server: GiteaServerPath, tokenSupplier: () -> String): HttpApiHelper {
  val authConfigurer = object : HttpRequestConfigurer {

    override fun configure(builder: HttpRequest.Builder): HttpRequest.Builder {
      val uri = builder.build().uri()
      if (server.isAuthorizedUrl(uri)) {
        val token = tokenSupplier()
        val headerValue = HttpSecurityUtil.createBearerAuthHeaderValue(token)
        return builder.header(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, headerValue)
      } else {
        return builder
      }
    }
  }
  val requestConfigurer = CompoundRequestConfigurer(RequestTimeoutConfigurer(), authConfigurer)
  return HttpApiHelper(
    logger = logger<GiteaApi>(),
    requestConfigurer = requestConfigurer
  )
}

private fun GiteaServerPath.isAuthorizedUrl(targetUri: URI): Boolean {
  val serverUri = toURI()

  if (targetUri.host != serverUri.host) {
    LOG.info("URL $targetUri host does not match the server $serverUri. Authorization will not be granted")
    return false
  }
  if (targetUri.port != serverUri.port) {
    LOG.info("URL $targetUri port does not match the server $serverUri. Authorization will not be granted")
    return false
  }
  if (targetUri.scheme != null && targetUri.scheme != serverUri.scheme) {
    LOG.info("URL $targetUri protocol does not match the server $serverUri. Authorization will not be granted")
    return false
  }
  if (serverUri.scheme == "http") {
    LOG.warn("URL $targetUri use HTTP, not HTTPS, token leak is possible")
  }
  return true
}

@Suppress("UnstableApiUsage")
private fun httpHelper(): HttpApiHelper {
  val requestConfigurer = CompoundRequestConfigurer(RequestTimeoutConfigurer(), GiteaHeaderConfigurer())
  return HttpApiHelper(logger = logger<GiteaApi>(), requestConfigurer = requestConfigurer)
}

private const val PLUGIN_USER_AGENT_NAME = "GiteaIdeaIntegration"

private class GiteaHeaderConfigurer : HttpRequestConfigurer {
  override fun configure(builder: HttpRequest.Builder): HttpRequest.Builder =
    builder.apply {
      header(HttpClientUtil.USER_AGENT_HEADER, HttpClientUtil.getUserAgentValue(PLUGIN_USER_AGENT_NAME))
    }
}
