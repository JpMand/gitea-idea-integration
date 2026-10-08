package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mappings are plain web URLs here; [preferredCandidate] only needs one per mapping. */
class GiteaPreferredCandidateTest {

  private val server = GiteaServerPath.from("http://localhost:3000")
  private val alice = GiteaAccount("alice", server, id = "alice-id")
  private val bob = GiteaAccount("bob", server, id = "bob-id")
  private val repo = "http://localhost:3000/acme/webapp"
  private val other = "http://localhost:3000/acme/other"

  private val candidates = listOf(
    ContextCandidate(repo, alice, "t1"),
    ContextCandidate(repo, bob, "t2"),
    ContextCandidate(other, bob, "t2"),
  )

  private fun pick(default: GiteaAccount?, selected: Pair<String, String>?) =
    preferredCandidate(candidates, default, selected) { it }

  @Test
  fun `without preferences the first candidate is used`() {
    assertEquals(candidates[0], pick(null, null))
  }

  @Test
  fun `the project's default account wins`() {
    assertEquals(candidates[1], pick(bob, (repo to "alice-id")))
  }

  @Test
  fun `the recorded selection applies when there is no default account`() {
    assertEquals(candidates[2], pick(null, (other to "bob-id")))
  }

  @Test
  fun `a default account without a candidate falls back`() {
    val carol = GiteaAccount("carol", server, id = "carol-id")
    assertEquals(candidates[0], pick(carol, null))
  }

  @Test
  fun `no candidates means no context`() {
    assertNull(preferredCandidate(emptyList<ContextCandidate<String>>(), alice, null) { it })
  }

  @Test
  fun `without a usable account the first repository is read anonymously`() {
    val repos = listOf("https://gitea.example.com/acme/a", "http://localhost:3000/acme/webapp")
    assertEquals(repos[0], anonymousCandidate(repos, emptyList()) { GiteaServerPath.from(it.substringBeforeLast("/acme")) })
  }

  @Test
  fun `a repository on an account's server is read anonymously first`() {
    val repos = listOf("https://gitea.example.com/acme/a", "http://localhost:3000/acme/webapp")
    assertEquals(repos[1], anonymousCandidate(repos, listOf(alice)) { GiteaServerPath.from(it.substringBeforeLast("/acme")) })
  }

  @Test
  fun `no repository means no anonymous context`() {
    assertNull(anonymousCandidate(emptyList<String>(), listOf(alice)) { GiteaServerPath.from(it) })
  }
}
