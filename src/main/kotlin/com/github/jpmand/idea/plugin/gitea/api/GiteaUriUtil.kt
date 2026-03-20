package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.util.withQuery
import java.net.URI
import java.net.URLEncoder

class GiteaUriUtil {

  class QueryBuilder {
    private val params = mutableListOf<Pair<String, String>>()

    fun addParam(key: String, value: String?): QueryBuilder {
      if (null == value) return this
      params.add(key to value)
      return this
    }

    fun addParam(key: String, value: Int?): QueryBuilder {
      if (null == value) return this
      addParam(key, value.toString())
      return this
    }

    fun addParam(key: String, value: Boolean?): QueryBuilder {
      if (null == value) return this
      addParam(key, value.toString())
      return this
    }

    infix fun build(uri: URI): URI {
      return uri.withQuery(params)
    }
  }
}

internal fun URI.withQuery(params: Collection<Pair<String, String>>): URI {
  val queryString = params.joinToString("&") { (key, value) ->
    "$key=${URLEncoder.encode(value, Charsets.UTF_8)}"
  }
  return withQuery(queryString)
}