package com.github.jpmand.idea.plugin.gitea.api

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

/**
 * Tests for JSON deserialization of Gitea API responses.
 * Verifies that snake_case JSON field names are correctly mapped to their Kotlin counterparts
 * via @JsonProperty annotations, consistent with the Gitea REST API format documented at
 * https://gitea.com/api/swagger (GET /user endpoint).
 */
class GiteaJsonDeSerializerTest {

  private fun <T> deserialize(json: String, clazz: Class<T>): T? =
    GiteaJsonDeSerializer.fromJson(StringReader(json), clazz)

  @Test
  fun `deserializes basic user fields`() {
    val json = """
      {
        "id": 42,
        "login": "jdoe",
        "email": "jdoe@example.com"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertEquals(42, user!!.id)
    assertEquals("jdoe", user.login)
    assertEquals("jdoe@example.com", user.email)
  }

  @Test
  fun `deserializes snake_case avatar_url to avatarUrl`() {
    val json = """
      {
        "id": 1,
        "login": "user",
        "avatar_url": "https://gitea.example.com/user/avatar/user/-1"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertEquals("https://gitea.example.com/user/avatar/user/-1", user!!.avatarUrl)
  }

  @Test
  fun `deserializes snake_case full_name to fullName`() {
    val json = """
      {
        "id": 1,
        "login": "user",
        "full_name": "John Doe"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertEquals("John Doe", user!!.fullName)
  }

  @Test
  fun `deserializes snake_case html_url to htmlUrl`() {
    val json = """
      {
        "id": 1,
        "login": "user",
        "html_url": "https://gitea.example.com/user"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertEquals("https://gitea.example.com/user", user!!.htmlUrl)
  }

  @Test
  fun `deserializes last_login date field in utc`() {
    val json = """
      {
        "id": 1,
        "login": "user",
        "last_login": "2026-03-12T21:52:10Z"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertNotNull(user!!.lastLogin)
    println("Deserialized lastLogin: ${user.lastLogin}")
    assertEquals(user.lastLogin?.date,12)
    assertEquals(user.lastLogin?.minutes,52)
  }

  @Test
  fun `deserializes last_login date field with offset`() {
    val json = """
      {
        "id": 1,
        "login": "user",
        "last_login": "2026-03-12T21:52:10+01:00"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertNotNull(user!!.lastLogin)
    println("Deserialized lastLogin: ${user.lastLogin}")
    assertEquals(user.lastLogin?.date,12)
    assertEquals(user.lastLogin?.minutes,52)
  }

  @Test
  fun `deserializes full user response matching gitea api format`() {
    // Matches the Gitea API /api/v1/user response format
    val json = """
      {
        "id": 123,
        "login": "testuser",
        "email": "testuser@gitea.example.com",
        "full_name": "Test User",
        "avatar_url": "https://gitea.example.com/user/avatar/testuser/-1",
        "html_url": "https://gitea.example.com/testuser",
        "last_login": "2026-03-12T21:52:10Z",
        "is_admin": false,
        "restricted": false,
        "active": true
      }
    """.trimIndent()

    val dto = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(dto)
    assertEquals(123, dto!!.id)
    assertEquals("testuser", dto.login)
    assertEquals("testuser@gitea.example.com", dto.email)
    assertEquals("Test User", dto.fullName)
    assertEquals("https://gitea.example.com/user/avatar/testuser/-1", dto.avatarUrl)
    assertEquals("https://gitea.example.com/testuser", dto.htmlUrl)
    assertNotNull(dto.lastLogin)
  }

  @Test
  fun `missing optional fields deserialize as null`() {
    val json = """
      {
        "id": 1,
        "login": "minimal"
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertEquals(1, user!!.id)
    assertEquals("minimal", user.login)
    assertNull(user.email)
    assertNull(user.fullName)
    assertNull(user.avatarUrl)
    assertNull(user.htmlUrl)
    assertNull(user.lastLogin)
  }

  @Test
  fun `toUser converts DTO to GiteaUser correctly`() {
    val json = """
      {
        "id": 7,
        "login": "converter",
        "email": "converter@example.com",
        "full_name": "Converter User",
        "avatar_url": "https://gitea.example.com/avatar",
        "html_url": "https://gitea.example.com/converter"
      }
    """.trimIndent()

    val dto = deserialize(json, GiteaUserDTO::class.java)!!
    val user = dto.toUser()

    assertEquals(7, user.id)
    assertEquals("converter", user.login)
    assertEquals("converter@example.com", user.email)
    assertEquals("Converter User", user.fullName)
    assertEquals("https://gitea.example.com/avatar", user.avatarUrl)
    assertEquals("https://gitea.example.com/converter", user.htmlUrl)
    assertEquals("converter", user.name) // name property delegates to login
  }

  @Test
  fun `unknown fields are ignored`() {
    // FAIL_ON_UNKNOWN_PROPERTIES is false — unknown fields must be silently ignored
    val json = """
      {
        "id": 1,
        "login": "user",
        "unknown_future_field": "some_value",
        "another_unknown": 42
      }
    """.trimIndent()

    val user = deserialize(json, GiteaUserDTO::class.java)
    assertNotNull(user)
    assertEquals(1, user!!.id)
    assertEquals("user", user.login)
  }
}
