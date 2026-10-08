package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.json.JsonHttpApiHelper
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// Since 263 the platform's loadJsonValue & co are `context(api: JsonHttpApiHelper) HttpRequest.loadXxx()`
// extensions. These keep the REST layer's `rest.loadJsonValue<T>(request)` shape on top of the
// `load*ByClass` interface members they delegate to.

@Suppress("UnstableApiUsage")
suspend inline fun <reified T : Any> JsonHttpApiHelper.loadJsonValue(request: HttpRequest): HttpResponse<out T> =
  loadJsonValueByClass(request, T::class.java)

@Suppress("UnstableApiUsage")
suspend inline fun <reified T : Any> JsonHttpApiHelper.loadOptionalJsonValue(request: HttpRequest): HttpResponse<out T?> =
  loadOptionalJsonValueByClass(request, T::class.java)

@Suppress("UnstableApiUsage")
suspend inline fun <reified T : Any> JsonHttpApiHelper.loadJsonList(request: HttpRequest): HttpResponse<out List<T>> =
  loadJsonListByClass(request, T::class.java)
