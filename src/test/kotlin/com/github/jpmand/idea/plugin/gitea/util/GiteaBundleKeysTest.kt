package com.github.jpmand.idea.plugin.gitea.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * Every literal key passed to [GiteaBundle] exists in `GiteaBundle.properties` — a missing one
 * only shows up at runtime, as `!key!` text in the UI.
 */
class GiteaBundleKeysTest {

  private val keyUse = Regex("""GiteaBundle\.message(?:Pointer)?\(\s*"([^"]+)"""")

  @Test
  fun `every key used in the sources exists`() {
    val bundle = Properties().apply {
      File("src/main/resources/messages/GiteaBundle.properties").reader().use { load(it) }
    }
    val missing = File("src/main/kotlin").walk()
      .filter { it.extension == "kt" }
      .flatMap { file -> keyUse.findAll(file.readText()).map { "${it.groupValues[1]} (${file.name})" to it.groupValues[1] } }
      .filterNot { (_, key) -> bundle.containsKey(key) }
      .map { it.first }
      .toSortedSet()
    assertEquals(emptySet<String>(), missing)
  }
}
