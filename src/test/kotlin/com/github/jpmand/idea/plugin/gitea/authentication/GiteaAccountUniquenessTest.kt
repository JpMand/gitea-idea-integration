package com.github.jpmand.idea.plugin.gitea.authentication

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

/**
 * The "account already exists" check must use one criterion everywhere: host + effective port +
 * context path, protocol-insensitive. Previously [GiteLoginUtil.isAccountUnique] compared full
 * URIs while `GiteaAccountManager.isAccountUnique` compared protocol-sensitively.
 */
class GiteaAccountUniquenessTest {

  companion object {
    @ClassRule
    @JvmField
    val appRule = ApplicationRule()
  }

  private fun account(url: String, name: String) =
    GiteaAccount(name = name, server = GiteaServerPath.from(url))

  @Test
  fun `same host different protocol is treated as a duplicate`() {
    val existing = listOf(account("https://gitea.example.com", "alice"))
    assertFalse(GiteLoginUtil.isAccountUnique(existing, GiteaServerPath.from("http://gitea.example.com"), "alice"))
  }

  @Test
  fun `trailing slash on the server path does not create a second account`() {
    val existing = listOf(account("https://example.com/gitea", "alice"))
    assertFalse(GiteLoginUtil.isAccountUnique(existing, GiteaServerPath.from("https://example.com/gitea/"), "alice"))
  }

  @Test
  fun `different username on the same server is unique`() {
    val existing = listOf(account("https://gitea.example.com", "alice"))
    assertTrue(GiteLoginUtil.isAccountUnique(existing, GiteaServerPath.from("https://gitea.example.com"), "bob"))
  }

  @Test
  fun `different host is unique`() {
    val existing = listOf(account("https://gitea.example.com", "alice"))
    assertTrue(GiteLoginUtil.isAccountUnique(existing, GiteaServerPath.from("https://gitea.other.com"), "alice"))
  }
}
