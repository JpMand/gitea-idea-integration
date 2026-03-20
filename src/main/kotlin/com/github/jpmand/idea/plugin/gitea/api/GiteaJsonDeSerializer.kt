package com.github.jpmand.idea.plugin.gitea.api

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.intellij.collaboration.api.json.JsonDataDeserializer
import com.intellij.collaboration.api.json.JsonDataSerializer
import java.io.Reader
import java.util.TimeZone
@Suppress("UnstableApiUsage")
object GiteaJsonDeSerializer : JsonDataSerializer, JsonDataDeserializer {
  private val mapper: ObjectMapper = giteaJacksonMapper()
    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    .setDateFormat(StdDateFormat.instance)
    .setTimeZone(TimeZone.getDefault())

  internal fun giteaJacksonMapper(): ObjectMapper =
    jacksonMapperBuilder()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .serializationInclusion(JsonInclude.Include.NON_NULL)
      .visibility(VisibilityChecker.Std(JsonAutoDetect.Visibility.NONE,
        JsonAutoDetect.Visibility.NONE,
        JsonAutoDetect.Visibility.NONE,
        JsonAutoDetect.Visibility.NONE,
        JsonAutoDetect.Visibility.ANY))
      .build()

  override fun toJsonBytes(content: Any): ByteArray = mapper.writeValueAsBytes(content)

  // this is required to handle empty reader/stream without an exception
  override fun <T> fromJson(bodyReader: Reader, clazz: Class<T>): T? =
    mapper.createParser(bodyReader)
      .readValueAsTree<JsonNode>()
      ?.let { mapper.treeToValue(it, clazz) }

  override fun <T> fromJson(bodyReader: Reader, clazz: Class<T>, vararg classArgs: Class<*>): T? {
    val type = mapper.typeFactory.constructParametricType(clazz, *classArgs)
    return mapper.createParser(bodyReader)
      .readValueAsTree<JsonNode>()
      ?.let { mapper.treeToValue(it, type) }
  }
}