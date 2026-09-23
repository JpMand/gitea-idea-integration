package com.github.jpmand.idea.plugin.gitea.api.json

import com.github.jpmand.idea.plugin.gitea.api.GiteaJsonDeSerializer
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CommitStatus
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

/**
 * Gitea is self-hosted and versioned, so a server may return an enum value this DTO set does not
 * know. Such a value must not fail the whole response: it deserialises to null, or to the
 * `@JsonEnumDefaultValue` where one is declared.
 */
class GiteaJsonEnumToleranceTest {

  private fun <T> deserialize(json: String, clazz: Class<T>): T? =
    GiteaJsonDeSerializer.fromJson(StringReader(json), clazz)

  @Test
  fun `unknown pull request state deserialises to null`() {
    val json = """{ "number": 1, "state": "some_future_state" }"""
    val pr = deserialize(json, PullRequest::class.java)
    assertNotNull(pr)
    assertNull(pr!!.state)
    assertEquals(1L, pr.number)
  }

  @Test
  fun `known lowercase pull request state still maps`() {
    val pr = deserialize("""{ "number": 2, "state": "closed" }""", PullRequest::class.java)
    assertEquals(PullRequest.State.CLOSED, pr!!.state)
  }

  @Test
  fun `unknown commit status maps to null`() {
    val status = deserialize("""{ "id": 1, "status": "cancelled" }""", CommitStatus::class.java)
    assertNotNull(status)
    assertNull(status!!.status)
  }

  @Test
  fun `unknown review state falls back to the declared default value`() {
    // PullReview.State annotates PENDING with @JsonEnumDefaultValue.
    val review = deserialize("""{ "id": 1, "state": "SUPERSEDED" }""", PullReview::class.java)
    assertNotNull(review)
    assertEquals(PullReview.State.PENDING, review!!.state)
  }
}
