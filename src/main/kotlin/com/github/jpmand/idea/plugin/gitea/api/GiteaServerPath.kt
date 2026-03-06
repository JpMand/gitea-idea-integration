package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.ServerPath
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.annotations.Tag
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.net.URI

@Tag("Server")
class GiteaServerPath(useHttp: Boolean?, host: String, port: Int?, path: String?) : ServerPath {

  constructor() : this(false, "localhost", -1, null)

  private val myUseHttp: Boolean = useHttp ?: false
  private val myHost: String = host
  private val myPort: Int = port ?: -1
  private val myPath: String? = path

  override fun toURI(): URI = URIBuilder().apply {
    scheme = if (myUseHttp) URLUtil.HTTP_PROTOCOL else URLUtil.HTTPS_PROTOCOL
    host = myHost
    port = myPort
    path = myPath ?: ""
  }.build()

  @NotNull
  fun getSchema(): String = if (myUseHttp) URLUtil.HTTP_PROTOCOL else URLUtil.HTTPS_PROTOCOL

  @NotNull
  fun getHost(): String = myHost

  @NotNull
  fun getPort(): Int = myPort

  @Nullable
  fun getPath(): String? = myPath

  override fun toString(): String = toURI().toString()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteaServerPath) return false

    if (myUseHttp != other.myUseHttp) return false
    if (myPort != other.myPort) return false
    if (myHost != other.myHost) return false
    if (myPath != other.myPath) return false

    return true
  }

  fun equals(other: Any?, ignoreProtocol: Boolean): Boolean {
    if (this === other) return true
    if (other !is GiteaServerPath) return false
    if (!ignoreProtocol && myUseHttp != other.myUseHttp) return false
    if (myPort != other.myPort) return false
    if (myHost != other.myHost) return false
    if (myPath != other.myPath) return false

    return true
  }

  override fun hashCode(): Int {
    var result = myUseHttp.hashCode()
    result = 31 * result + myPort
    result = 31 * result + myHost.hashCode()
    result = 31 * result + (myPath?.hashCode() ?: 0)
    return result
  }
}