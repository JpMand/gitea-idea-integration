package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.ServerPath
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.net.URI
import java.net.URISyntaxException

@Tag("Server")
class GiteaServerPath(useHttp: Boolean?, host: String, port: Int?, path: String?) : ServerPath {

  constructor() : this(false, "localhost", -1, null)

  @field:Attribute("useHttp")
  private val myUseHttp: Boolean = useHttp ?: false

  @field:Attribute("host")
  private val myHost: String = host

  @field:Attribute("port")
  private val myPort: Int = port ?: -1

  @field:Attribute("path")
  private val myPath: String? = path

  /** Context path with surrounding whitespace and any trailing `/` removed; blank becomes null. */
  private val normalizedPath: String?
    get() = myPath?.trim()?.trimEnd('/')?.takeIf(String::isNotEmpty)

  private val defaultPort: Int
    get() = if (myUseHttp) 80 else 443

  /** The port in use: the explicit one, or the scheme's default when none is given. */
  private val effectivePort: Int
    get() = if (myPort > 0) myPort else defaultPort

  /** The explicitly-specified non-default port, or null when the scheme default is in effect. */
  private val explicitPort: Int?
    get() = myPort.takeIf { it > 0 && it != defaultPort }

  override fun toURI(): URI = URIBuilder().apply {
    scheme = if (myUseHttp) URLUtil.HTTP_PROTOCOL else URLUtil.HTTPS_PROTOCOL
    host = myHost
    port = myPort
    path = normalizedPath ?: ""
  }.build()

  @NotNull
  fun getSchema(): String = if (myUseHttp) URLUtil.HTTP_PROTOCOL else URLUtil.HTTPS_PROTOCOL

  @NotNull
  fun getHost(): String = myHost

  @NotNull
  fun getPort(): Int = myPort

  @Nullable
  fun getPath(): String? = normalizedPath

  fun restApiUri(): URI = URIBuilder().apply {
    scheme = if (myUseHttp) URLUtil.HTTP_PROTOCOL else URLUtil.HTTPS_PROTOCOL
    host = myHost
    port = myPort
    path = (normalizedPath ?: "") + API_PREFIX
  }.build()

  companion object{
    val DEFAULT_SERVER = from("https://gitea.com")
    const val API_PREFIX = "/api/v1/"

    @JvmStatic
    fun from(url: String): GiteaServerPath {
      val uri = URI(url)
      val useHttp = when (uri.scheme?.lowercase()) {
        URLUtil.HTTP_PROTOCOL -> true
        URLUtil.HTTPS_PROTOCOL -> false
        else -> throw IllegalArgumentException("Unsupported protocol: ${uri.scheme}")
      }
      val host = uri.host?.lowercase() ?: throw IllegalArgumentException("Missing host: $url")
      val path = uri.path?.trimEnd('/')?.takeIf { it.isNotEmpty() }
      return GiteaServerPath(useHttp, host, uri.port, path)
    }

    /** Like [from], but returns null instead of throwing on a malformed or unsupported URL. */
    @JvmStatic
    fun fromOrNull(url: String): GiteaServerPath? =
      try {
        from(url)
      } catch (_: IllegalArgumentException) {
        null
      } catch (_: URISyntaxException) {
        null
      }
  }

  override fun toString(): String = toURI().toString()

  fun toAccessTokenUrl(): String {
    val instanceUrl = toString().trim('/')
    return "$instanceUrl/user/settings/applications"
  }

  override fun equals(other: Any?): Boolean = equals(other, ignoreProtocol = false)

  /**
   * Compares host (case-insensitive), port and context path. A missing port means the scheme's
   * default port (443/80), and a trailing `/` on the path is ignored.
   *
   * With [ignoreProtocol] the `http`/`https` scheme is not compared: the same host is the same
   * server whichever scheme reached it. Ports are then compared as the ports actually in use (a
   * missing one is its scheme's default), except that two servers both on their own scheme's
   * default port are the same too — `http://host` and `https://host` are one server, while
   * `http://host:3000` only matches another `:3000`.
   */
  fun equals(other: Any?, ignoreProtocol: Boolean): Boolean {
    if (this === other) return true
    if (other !is GiteaServerPath) return false
    if (!myHost.equals(other.myHost, ignoreCase = true)) return false
    if (normalizedPath != other.normalizedPath) return false
    if (!ignoreProtocol) return myUseHttp == other.myUseHttp && explicitPort == other.explicitPort
    val bothOnDefaultPort = explicitPort == null && other.explicitPort == null
    return bothOnDefaultPort || effectivePort == other.effectivePort
  }

  override fun hashCode(): Int {
    var result = myUseHttp.hashCode()
    result = 31 * result + (explicitPort ?: 0)
    result = 31 * result + myHost.lowercase().hashCode()
    result = 31 * result + (normalizedPath?.hashCode() ?: 0)
    return result
  }
}