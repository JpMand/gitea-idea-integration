package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.collaboration.api.json.JsonHttpApiHelper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val LOG: Logger = logger<GiteaApi>()

/**
 * Wraps [delegate] to log every request/response body sent through [GiteaApi.rest] at DEBUG
 * (Help > Diagnostic Tools > Debug Log Settings > add
 * `#com.github.jpmand.idea.plugin.gitea.api.GiteaApi`). Every REST endpoint function in `api/rest/`
 * goes through [JsonHttpApiHelper.jsonBodyPublisher] (for a request body) and one of the four
 * `load*JsonValue`/`load*JsonList` calls (for the response), so wrapping those here covers the
 * whole REST layer without touching each of the ~40 individual endpoint functions.
 *
 * The platform's own [com.intellij.collaboration.api.httpclient.HttpClientUtil] already logs
 * method/URI and status code at DEBUG unconditionally, and bodies at TRACE when enabled — this
 * fills exactly the DEBUG-level body gap, re-serializing via [GiteaJsonDeSerializer] purely for
 * the log line. The actual bytes sent/parsed (and [delegate]'s return value) are untouched.
 */
@Suppress("UnstableApiUsage")
internal class GiteaApiRequestLogging(private val delegate: JsonHttpApiHelper) : JsonHttpApiHelper by delegate {

    override fun jsonBodyPublisher(uri: URI, body: Any): HttpRequest.BodyPublisher {
        logRequestBody(uri, body)
        return delegate.jsonBodyPublisher(uri, body)
    }

    override suspend fun <T> loadJsonValueByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out T> =
        logged(request) { delegate.loadJsonValueByClass(request, clazz) }

    override suspend fun <T> loadOptionalJsonValueByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out T?> =
        logged(request) { delegate.loadOptionalJsonValueByClass(request, clazz) }

    override suspend fun <T> loadJsonListByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out List<T>> =
        logged(request) { delegate.loadJsonListByClass(request, clazz) }

    override suspend fun <T> loadOptionalJsonListByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out List<T>?> =
        logged(request) { delegate.loadOptionalJsonListByClass(request, clazz) }

    private suspend inline fun <T> logged(request: HttpRequest, call: suspend () -> HttpResponse<out T>): HttpResponse<out T> {
        try {
            val response = call()
            logResponseBody(request, response.statusCode(), response.body())
            return response
        } catch (e: HttpStatusErrorException) {
            // e.message already reads "HTTP Request <name> failed with status code <code> and
            // response body: <body>" — the same information TRACE-level logs the raw body as,
            // just visible at DEBUG here too.
            LOG.debug("<-- ${request.method()} ${request.uri()} failed: ${e.message}")
            throw e
        }
    }

    private fun logRequestBody(uri: URI, body: Any) {
        if (!LOG.isDebugEnabled) return
        LOG.debug("--> $uri request body: ${body.toLoggableJson()}")
    }

    private fun logResponseBody(request: HttpRequest, statusCode: Int, body: Any?) {
        if (!LOG.isDebugEnabled) return
        val json = body?.toLoggableJson() ?: "<empty>"
        LOG.debug("<-- ${request.method()} ${request.uri()} [$statusCode] response body: $json")
    }

    private fun Any.toLoggableJson(): String =
        runCatching { GiteaJsonDeSerializer.toJsonBytes(this).toString(Charsets.UTF_8) }.getOrElse { toString() }
}
