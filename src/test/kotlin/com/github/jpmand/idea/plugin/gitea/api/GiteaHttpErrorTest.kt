package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

/** Uses [ApplicationRule] because the messages resolve through the plugin's resource bundle. */
@Suppress("UnstableApiUsage")
class GiteaHttpErrorTest {

  companion object {
    @ClassRule
    @JvmField
    val appRule = ApplicationRule()
  }

  private fun err(status: Int, body: String = "") =
    GiteaHttpError.from(HttpStatusErrorException("GET /x", status, body))

  @Test
  fun `401 maps to Unauthorized`() {
    assertTrue(err(401) is GiteaHttpError.Unauthorized)
  }

  @Test
  fun `403 without scope info`() {
    val e = err(403, """{"message":"Forbidden"}""")
    assertTrue(e is GiteaHttpError.Forbidden)
    assertNull((e as GiteaHttpError.Forbidden).missingScope)
  }

  @Test
  fun `403 names the missing scope from the body`() {
    val body = """{"message":"token does not have at least one of required scope(s): [write:repository]"}"""
    val e = err(403, body) as GiteaHttpError.Forbidden
    assertEquals("write:repository", e.missingScope)
    assertTrue(e.message!!.contains("write:repository"))
  }

  @Test
  fun `404 maps to NotFound`() {
    assertTrue(err(404) is GiteaHttpError.NotFound)
  }

  @Test
  fun `500 maps to ServerError`() {
    val e = err(503) as GiteaHttpError.ServerError
    assertEquals(503, e.statusCode)
  }

  @Test
  fun `other status carries the server message`() {
    val e = err(422, """{"message":"Validation failed"}""")
    assertTrue(e is GiteaHttpError.Other)
    assertEquals("Validation failed", e.message)
  }

  @Test
  fun `giteaApiCall wraps HttpStatusErrorException and passes other throwables through`() {
    val wrapped = runCatchingBlocking { giteaApiCall<Unit> { throw HttpStatusErrorException("GET /x", 404, "") } }
    assertTrue(wrapped is GiteaHttpError.NotFound)

    val passthrough = runCatchingBlocking { giteaApiCall<Unit> { throw IllegalStateException("boom") } }
    assertTrue(passthrough is IllegalStateException)
  }

  private fun runCatchingBlocking(block: suspend () -> Unit): Throwable? =
    try {
      kotlinx.coroutines.runBlocking { block() }
      null
    } catch (t: Throwable) {
      t
    }
}
