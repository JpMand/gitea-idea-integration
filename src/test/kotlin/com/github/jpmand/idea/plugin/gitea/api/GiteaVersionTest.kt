package com.github.jpmand.idea.plugin.gitea.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiteaVersionTest {

  private val floor = GiteaVersion(1, 26, 0)

  @Test
  fun `parses plain release`() {
    val v = GiteaVersion.fromString("1.26.4")
    assertEquals(1, v.major)
    assertEquals(26, v.minor)
    assertEquals(4, v.patch)
    assertNull(v.metadata)
  }

  @Test
  fun `parses dev build with plus metadata`() {
    val v = GiteaVersion.fromString("1.27.0+dev-651-gcb08549242")
    assertEquals(1, v.major)
    assertEquals(27, v.minor)
    assertEquals(0, v.patch)
    assertEquals("dev-651-gcb08549242", v.metadata)
    // metadata is ignored for comparison
    assertEquals(0, v.compareTo(GiteaVersion(1, 27, 0)))
  }

  @Test
  fun `parses release candidate with hyphen`() {
    val v = GiteaVersion.fromString("1.26.0-rc0")
    assertEquals(1, v.major)
    assertEquals(26, v.minor)
    assertEquals(0, v.patch)
    assertEquals("rc0", v.metadata)
    // 1.26.0-rc0 is treated as 1.26.0 -> not below the floor
    assertTrue(v >= floor)
  }

  @Test
  fun `parses leading v`() {
    val v = GiteaVersion.fromString("v1.21.11")
    assertEquals(1, v.major)
    assertEquals(21, v.minor)
    assertEquals(11, v.patch)
  }

  @Test
  fun `parses forgejo style version`() {
    val v = GiteaVersion.fromString("11.0.1+gitea-1.22.0")
    assertEquals(11, v.major)
    assertEquals(0, v.minor)
    assertEquals(1, v.patch)
    assertTrue(v >= floor)
  }

  @Test
  fun `parses bare major`() {
    val v = GiteaVersion.fromString("1")
    assertEquals(1, v.major)
    assertNull(v.minor)
    assertNull(v.patch)
  }

  @Test
  fun `empty string does not throw and is below the floor`() {
    val v = GiteaVersion.fromString("")
    assertEquals(0, v.major)
    assertTrue(v < floor)
  }

  @Test
  fun `unparseable string does not throw and is below the floor`() {
    val v = GiteaVersion.fromString("not-a-version")
    assertEquals(0, v.major)
    assertTrue(v < floor)
  }

  @Test
  fun `error page with digits is not mistaken for a version`() {
    // Digits appearing after leading text (e.g. an HTTP error page from a proxy, not a real
    // Gitea /version response) must not be parsed out as the major version.
    val v = GiteaVersion.fromString("Bad Gateway 502")
    assertEquals(0, v.major)
    assertTrue(v < floor)
    assertNull(GiteaVersion.fromStringOrNull("Bad Gateway 502"))
  }

  @Test
  fun `fromStringOrNull returns null for unparseable input`() {
    assertNull(GiteaVersion.fromStringOrNull(""))
    assertNull(GiteaVersion.fromStringOrNull("nginx"))
  }

  @Test
  fun `floor comparison across candidate versions`() {
    assertTrue(GiteaVersion.fromString("1.24.0") < floor)
    assertTrue(GiteaVersion.fromString("1.25.5") < floor)
    assertTrue(GiteaVersion.fromString("1.26.0") >= floor)
    assertTrue(GiteaVersion.fromString("1.26.4") >= floor)
    assertTrue(GiteaVersion.fromString("1.27.3") >= floor)
    assertTrue(GiteaVersion.fromString("2.0.0") >= floor)
  }
}
