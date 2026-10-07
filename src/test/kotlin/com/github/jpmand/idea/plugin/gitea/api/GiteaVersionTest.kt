package com.github.jpmand.idea.plugin.gitea.api

import com.github.jpmand.idea.plugin.gitea.CachingGiteaServersManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiteaVersionTest {

  private val manager = CachingGiteaServersManager(CoroutineScope(Dispatchers.Default))
  private val floor = manager.earliestSupportedVersion

  private fun supported(version: String) = manager.isSupported(GiteaVersion.fromString(version), acceptPreReleases = false)
  private fun supportedWithPreReleases(version: String) = manager.isSupported(GiteaVersion.fromString(version), acceptPreReleases = true)

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
    val v = GiteaVersion.fromString("1.27.0-rc0")
    assertEquals(1, v.major)
    assertEquals(27, v.minor)
    assertEquals(0, v.patch)
    assertEquals("rc0", v.metadata)
    // 1.27.0-rc0 is treated as 1.27.0 -> not below the floor
    assertTrue(v >= floor)
    // ...but only releases are supported
    assertFalse(manager.isSupported(v, acceptPreReleases = false))
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
    // ...but Forgejo isn't Gitea
    assertFalse(manager.isSupported(v, acceptPreReleases = false))
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
    // 1.26 lacks the review-comment reply endpoint
    assertTrue(GiteaVersion.fromString("1.26.0") < floor)
    assertTrue(GiteaVersion.fromString("1.26.4") < floor)
    assertTrue(GiteaVersion.fromString("1.27.0") >= floor)
    assertTrue(GiteaVersion.fromString("1.27.3") >= floor)
    assertTrue(GiteaVersion.fromString("2.0.0") >= floor)
  }

  @Test
  fun `release versions from 1_27 and from 28 on are supported`() {
    assertTrue(supported("1.27.0"))
    assertTrue(supported("1.27.3"))
    // Gitea's numbering after 1.27.3
    assertTrue(supported("28.0.0"))
    assertTrue(supported("28.0.1"))
  }

  @Test
  fun `dev builds and release candidates are not supported`() {
    assertFalse(supported("1.27.0+dev-1118-ge629c4fdc2"))
    assertFalse(supported("29.0.0+dev-53-gfc44404843"))
    assertFalse(supported("1.27.0-rc0"))
    assertFalse(supported("v1.27.3"))
    assertFalse(supported("1.27"))
  }

  @Test
  fun `forgejo and old or unknown versions are not supported`() {
    assertFalse(supported("16.0.0-dev-753-6bcc6da0+gitea-1.22.0"))
    assertFalse(supported("16.0.5+gitea-1.22.0"))
    assertFalse(supported("17.0.0-dev-555-733016624c+gitea-1.22.0"))
    // Gitea never released majors 2 to 27
    assertFalse(supported("16.0.5"))
    assertFalse(supported("2.0.0"))
    assertFalse(supported("1.26.4"))
    assertFalse(supported("not-a-version"))
  }

  @Test
  fun `with pre-releases accepted, dev builds and release candidates are supported`() {
    assertTrue(supportedWithPreReleases("1.27.0+dev-1118-ge629c4fdc2"))
    assertTrue(supportedWithPreReleases("29.0.0+dev-53-gfc44404843"))
    assertTrue(supportedWithPreReleases("1.27.0-rc0"))
    assertTrue(supportedWithPreReleases("28.0.0"))
  }

  @Test
  fun `with pre-releases accepted, forgejo and old versions still are not`() {
    assertFalse(supportedWithPreReleases("16.0.0-dev-753-6bcc6da0+gitea-1.22.0"))
    assertFalse(supportedWithPreReleases("16.0.5+gitea-1.22.0"))
    assertFalse(supportedWithPreReleases("16.0.5"))
    assertFalse(supportedWithPreReleases("1.26.0+dev-1-gabc"))
    assertFalse(supportedWithPreReleases("1.26.4"))
    assertFalse(supportedWithPreReleases("not-a-version"))
  }
}
