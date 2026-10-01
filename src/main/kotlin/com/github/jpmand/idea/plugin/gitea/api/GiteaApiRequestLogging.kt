package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.json.JsonHttpApiHelper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val LOG: Logger = logger<GiteaApi>()

/**
 * Wraps [delegate] to log the body of every request sent through [GiteaApi.rest] at TRACE
 * (Help > Diagnostic Tools > Debug Log Settings > add
 * `#com.github.jpmand.idea.plugin.gitea.api.GiteaApi:trace`). Every REST endpoint function in
 * `api/rest/` builds its request body with [JsonHttpApiHelper.jsonBodyPublisher], so wrapping it
 * here covers the whole REST layer.
 *
 * The platform's own [com.intellij.collaboration.api.httpclient.HttpClientUtil] already logs
 * method/URI and status code at DEBUG and response bodies at TRACE; request bodies are the part
 * it leaves out. Bodies carry comment and review text, so they stay out of DEBUG. The body is
 * re-serialized via [GiteaJsonDeSerializer] purely for the log line; the bytes sent are untouched.
 */
@Suppress("UnstableApiUsage")
internal class GiteaApiRequestLogging(private val delegate: JsonHttpApiHelper) : JsonHttpApiHelper by delegate {

    override fun jsonBodyPublisher(uri: URI, body: Any): HttpRequest.BodyPublisher {
        logRequestBody(uri, body)
        return delegate.jsonBodyPublisher(uri, body)
    }

    override suspend fun <T> loadJsonValueByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out T> =
        delegate.loadJsonValueByClass(request, clazz)

    override suspend fun <T> loadOptionalJsonValueByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out T?> =
        delegate.loadOptionalJsonValueByClass(request, clazz)

    override suspend fun <T> loadJsonListByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out List<T>> =
        delegate.loadJsonListByClass(request, clazz)

    override suspend fun <T> loadOptionalJsonListByClass(request: HttpRequest, clazz: Class<T>): HttpResponse<out List<T>?> =
        delegate.loadOptionalJsonListByClass(request, clazz)

    private fun logRequestBody(uri: URI, body: Any) {
        if (!LOG.isTraceEnabled) return
        LOG.trace("--> $uri request body: ${body.toLoggableJson()}")
    }

    private fun Any.toLoggableJson(): String =
        runCatching { GiteaJsonDeSerializer.toJsonBytes(this).toString(Charsets.UTF_8) }.getOrElse { toString() }
}
