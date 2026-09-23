package com.github.jpmand.idea.plugin.gitea.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GiteaPagingTest {

  @Test
  fun `stops on the first short page`() = runBlocking {
    val pages = listOf(List(50) { it }, List(50) { it }, List(7) { it })
    val calls = mutableListOf<Int>()
    val all = loadAllGiteaPages(pageSize = 50) { page -> calls += page; pages[page - 1] }
    assertEquals(listOf(1, 2, 3), calls)
    assertEquals(107, all.size)
  }

  @Test
  fun `single full page then empty page`() = runBlocking {
    val all = loadAllGiteaPages(pageSize = 3) { page -> if (page == 1) listOf("a", "b", "c") else emptyList() }
    assertEquals(listOf("a", "b", "c"), all)
  }

  @Test
  fun `empty first page`() = runBlocking {
    val all = loadAllGiteaPages(pageSize = 10) { emptyList<Int>() }
    assertEquals(emptyList<Int>(), all)
  }

  @Test
  fun `respects maxPages`() = runBlocking {
    var calls = 0
    val all = loadAllGiteaPages(pageSize = 1, maxPages = 5) { calls++; listOf(0) }
    assertEquals(5, calls)
    assertEquals(5, all.size)
  }
}
