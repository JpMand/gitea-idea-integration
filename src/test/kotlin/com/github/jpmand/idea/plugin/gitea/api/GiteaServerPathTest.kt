package com.github.jpmand.idea.plugin.gitea.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiteaServerPathTest {

  @Test
  fun `from https url sets correct fields`() {
    val path = GiteaServerPath.from("https://gitea.example.com")
    assertEquals("https", path.getSchema())
    assertEquals("gitea.example.com", path.getHost())
    assertEquals(-1, path.getPort())
    assertNull(path.getPath())
  }

  @Test
  fun `from http url sets http schema`() {
    val path = GiteaServerPath.from("http://gitea.example.com")
    assertEquals("http", path.getSchema())
    assertEquals("gitea.example.com", path.getHost())
  }

  @Test
  fun `from url with port stores port`() {
    val path = GiteaServerPath.from("https://gitea.example.com:3000")
    assertEquals(3000, path.getPort())
    assertEquals("gitea.example.com", path.getHost())
    assertEquals("https", path.getSchema())
  }

  @Test
  fun `from url with path stores path`() {
    val path = GiteaServerPath.from("https://gitea.example.com/gitea")
    assertEquals("/gitea", path.getPath())
    assertEquals("gitea.example.com", path.getHost())
  }

  @Test
  fun `from url with port and path stores both`() {
    val path = GiteaServerPath.from("https://gitea.example.com:3000/gitea")
    assertEquals(3000, path.getPort())
    assertEquals("/gitea", path.getPath())
  }

  @Test
  fun `from url with no path stores null path`() {
    val path = GiteaServerPath.from("https://gitea.example.com")
    assertNull(path.getPath())
  }

  @Test
  fun `restApiUri for simple server`() {
    val path = GiteaServerPath.from("https://gitea.example.com")
    assertEquals("https://gitea.example.com/api/v1/", path.restApiUri().toString())
  }

  @Test
  fun `restApiUri for server with port`() {
    val path = GiteaServerPath.from("https://gitea.example.com:3000")
    assertEquals("https://gitea.example.com:3000/api/v1/", path.restApiUri().toString())
  }

  @Test
  fun `restApiUri for server with path`() {
    val path = GiteaServerPath.from("https://gitea.example.com/gitea")
    assertEquals("https://gitea.example.com/gitea/api/v1/", path.restApiUri().toString())
  }

  @Test
  fun `restApiUri for http server with port`() {
    val path = GiteaServerPath.from("http://gitea.example.com:3000")
    assertEquals("http://gitea.example.com:3000/api/v1/", path.restApiUri().toString())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `from invalid protocol throws IllegalArgumentException`() {
    GiteaServerPath.from("ftp://gitea.example.com")
  }

  @Test
  fun `equals same server paths are equal`() {
    val p1 = GiteaServerPath.from("https://gitea.example.com")
    val p2 = GiteaServerPath.from("https://gitea.example.com")
    assertEquals(p1, p2)
  }

  @Test
  fun `equals different protocol not equal by default`() {
    val p1 = GiteaServerPath.from("https://gitea.example.com")
    val p2 = GiteaServerPath.from("http://gitea.example.com")
    assertFalse(p1 == p2)
  }

  @Test
  fun `equals ignoreProtocol treats http and https as equal`() {
    val p1 = GiteaServerPath.from("https://gitea.example.com")
    val p2 = GiteaServerPath.from("http://gitea.example.com")
    assertTrue(p1.equals(p2, ignoreProtocol = true))
  }

  @Test
  fun `equals different hosts not equal`() {
    val p1 = GiteaServerPath.from("https://gitea.example.com")
    val p2 = GiteaServerPath.from("https://gitea.other.com")
    assertFalse(p1 == p2)
  }

  @Test
  fun `equals different ports not equal`() {
    val p1 = GiteaServerPath.from("https://gitea.example.com:3000")
    val p2 = GiteaServerPath.from("https://gitea.example.com:3001")
    assertFalse(p1 == p2)
  }

  @Test
  fun `toString produces valid URI string`() {
    val path = GiteaServerPath.from("https://gitea.example.com")
    assertTrue(path.toString().startsWith("https://gitea.example.com"))
  }

  @Test
  fun `toString includes port when specified`() {
    val path = GiteaServerPath.from("https://gitea.example.com:3000")
    assertTrue(path.toString().contains(":3000"))
  }

  @Test
  fun `toAccessTokenUrl returns correct url`() {
    val path = GiteaServerPath.from("https://gitea.example.com")
    assertTrue(path.toAccessTokenUrl().endsWith("/user/settings/applications"))
    assertTrue(path.toAccessTokenUrl().startsWith("https://gitea.example.com"))
  }

  @Test
  fun `hashCode same for equal paths`() {
    val p1 = GiteaServerPath.from("https://gitea.example.com:3000")
    val p2 = GiteaServerPath.from("https://gitea.example.com:3000")
    assertEquals(p1.hashCode(), p2.hashCode())
  }
}
