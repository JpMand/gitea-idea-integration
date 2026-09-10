package com.github.jpmand.idea.plugin.gitea.api

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.api.HttpStatusErrorException
import org.jetbrains.annotations.Nls

/**
 * A Gitea REST failure classified into an actionable category with a user-facing [message].
 *
 * Only HTTP status failures ([HttpStatusErrorException]) are wrapped in this type; network
 * errors, cancellation and everything else pass through untouched. Wrap a call site with
 * [giteaApiCall] to get this translation.
 */
@Suppress("UnstableApiUsage")
sealed class GiteaHttpError(@Nls message: String, cause: Throwable) : RuntimeException(message, cause) {

  /** 401 — the token is invalid or expired. */
  class Unauthorized(cause: Throwable) :
    GiteaHttpError(GiteaBundle.message("error.http.unauthorized"), cause)

  /** 403 — the token authenticates but lacks permission; [missingScope] is named when Gitea reports it. */
  class Forbidden(val missingScope: String?, cause: Throwable) : GiteaHttpError(
    missingScope
      ?.let { GiteaBundle.message("error.http.forbidden.scope", it) }
      ?: GiteaBundle.message("error.http.forbidden"),
    cause,
  )

  /** 404 — repository/PR missing or invisible to this token. */
  class NotFound(cause: Throwable) :
    GiteaHttpError(GiteaBundle.message("error.http.not.found"), cause)

  /** 5xx — server-side, generally retryable. */
  class ServerError(val statusCode: Int, cause: Throwable) :
    GiteaHttpError(GiteaBundle.message("error.http.server", statusCode.toString()), cause)

  /** Any other status — carries Gitea's own message when the response body had one. */
  class Other(@Nls message: String, cause: Throwable) : GiteaHttpError(message, cause)

  companion object {
    private val SCOPE_RE = Regex("""scope\(s\):\s*\[([^]]+)]""")
    private val MESSAGE_RE = Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""")

    fun from(e: HttpStatusErrorException): GiteaHttpError {
      val body = e.body.orEmpty()
      return when (e.statusCode) {
        401 -> Unauthorized(e)
        403 -> Forbidden(SCOPE_RE.find(body)?.groupValues?.get(1)?.trim(), e)
        404 -> NotFound(e)
        in 500..599 -> ServerError(e.statusCode, e)
        else -> Other(serverMessage(body) ?: e.localizedMessage ?: "HTTP ${e.statusCode}", e)
      }
    }

    /** Best-effort extraction of Gitea's `{"message": "..."}` from an error response body. */
    private fun serverMessage(body: String): String? =
      MESSAGE_RE.find(body)?.groupValues?.get(1)?.replace("\\\"", "\"")?.takeIf { it.isNotBlank() }
  }
}

/**
 * Runs [block], converting an [HttpStatusErrorException] into a [GiteaHttpError] (a friendly,
 * actionable message). Every other throwable — including [kotlinx.coroutines.CancellationException]
 * — propagates unchanged.
 */
@Suppress("UnstableApiUsage")
suspend fun <T> giteaApiCall(block: suspend () -> T): T =
  try {
    block()
  } catch (e: HttpStatusErrorException) {
    throw GiteaHttpError.from(e)
  }
