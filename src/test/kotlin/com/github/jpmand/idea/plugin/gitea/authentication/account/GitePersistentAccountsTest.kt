package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.intellij.configurationStore.deserializeAndLoadState
import com.intellij.configurationStore.serialize
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

/**
 * Tests for XML serialization and deserialization of Gitea accounts.
 * Follows the same pattern as GHPersistentAccountsTest in intellij-community.
 */
class GitePersistentAccountsTest {

  companion object {
    @ClassRule
    @JvmField
    val appRule = ApplicationRule()
  }

  @Test
  fun `serialize accounts to xml`() {
    val service = GitePersistentAccounts()

    val id1 = "a1b2c3d4-0000-0000-0000-000000000001"
    val id2 = "a1b2c3d4-0000-0000-0000-000000000002"

    service.accounts = setOf(
      GiteaAccount("alice", GiteaServerPath.from("https://gitea.example.com"), id1),
      GiteaAccount("bob", GiteaServerPath.from("https://gitea.example.com:3000"), id2)
    )

    @Suppress("UsePropertyAccessSyntax")
    val state = service.getState()
    val element = serialize(state)!!
    val xml = JDOMUtil.write(element)

    // Verify both accounts are present
    assertTrue("Expected alice in serialized XML:\n$xml", xml.contains("name=\"alice\""))
    assertTrue("Expected bob in serialized XML:\n$xml", xml.contains("name=\"bob\""))
    assertTrue("Expected id1 in serialized XML:\n$xml", xml.contains("id=\"$id1\""))
    assertTrue("Expected id2 in serialized XML:\n$xml", xml.contains("id=\"$id2\""))
    assertTrue("Expected host in serialized XML:\n$xml", xml.contains("host=\"gitea.example.com\""))
  }

  @Test
  fun `deserialize accounts from xml`() {
    val service = GitePersistentAccounts()

    val id1 = "a1b2c3d4-0000-0000-0000-000000000001"
    val id2 = "a1b2c3d4-0000-0000-0000-000000000002"

    val element = JDOMUtil.load("""
      <component name="GiteaAccounts">
        <account name="alice" id="$id1">
          <server useHttp="false" host="gitea.example.com" port="-1" />
        </account>
        <account name="bob" id="$id2">
          <server useHttp="false" host="gitea.example.com" port="3000" />
        </account>
      </component>
    """.trimIndent())
    deserializeAndLoadState(service, element)

    val accounts = service.accounts.sortedBy { it.name }
    assertEquals(2, accounts.size)

    val alice = accounts[0]
    assertEquals("alice", alice.name)
    assertEquals(id1, alice.id)
    assertEquals("gitea.example.com", alice.server.getHost())
    assertEquals(-1, alice.server.getPort())

    val bob = accounts[1]
    assertEquals("bob", bob.name)
    assertEquals(id2, bob.id)
    assertEquals("gitea.example.com", bob.server.getHost())
    assertEquals(3000, bob.server.getPort())
  }

  @Test
  fun `serialize and deserialize round-trip preserves account data`() {
    val service = GitePersistentAccounts()

    val id = "a1b2c3d4-0000-0000-0000-000000000001"
    val original = GiteaAccount("roundtripuser", GiteaServerPath.from("https://gitea.example.com:3000"), id)
    service.accounts = setOf(original)

    @Suppress("UsePropertyAccessSyntax")
    val element = serialize(service.getState())!!

    val restored = GitePersistentAccounts()
    deserializeAndLoadState(restored, element)

    val restoredAccounts = restored.accounts
    assertEquals(1, restoredAccounts.size)
    val restoredAccount = restoredAccounts.single()
    assertEquals("roundtripuser", restoredAccount.name)
    assertEquals(id, restoredAccount.id)
    assertEquals("gitea.example.com", restoredAccount.server.getHost())
    assertEquals(3000, restoredAccount.server.getPort())
  }

  @Test
  fun `empty accounts serializes to empty array`() {
    val service = GitePersistentAccounts()
    service.accounts = emptySet()

    @Suppress("UsePropertyAccessSyntax")
    val state = service.getState()
    assertEquals(0, state.size)
  }

  @Test
  fun `no state loaded results in empty accounts`() {
    val service = GitePersistentAccounts()
    service.noStateLoaded()
    assertEquals(emptySet<GiteaAccount>(), service.accounts)
  }
}
