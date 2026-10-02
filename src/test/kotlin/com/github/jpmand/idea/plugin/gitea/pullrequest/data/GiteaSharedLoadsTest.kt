package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GiteaSharedLoadsTest {

    private var now = 0L
    private val loads = GiteaSharedLoads(reuseFor = 100, clock = { now })
    private val calls = AtomicInteger()

    private suspend fun load(key: Any = "k", value: String = "v"): String =
        loads.load(key) { calls.incrementAndGet(); value }

    @Test
    fun `concurrent loads share one call`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val results = List(3) {
            async { loads.load("k") { calls.incrementAndGet(); gate.await(); "v" } }
        }
        yield()
        gate.complete(Unit)
        assertEquals(listOf("v", "v", "v"), results.awaitAll())
        assertEquals(1, calls.get())
    }

    @Test
    fun `a recent result is reused, an old one is not`() = runBlocking {
        load()
        now = 99
        load()
        assertEquals(1, calls.get())
        now = 100
        load()
        assertEquals(2, calls.get())
    }

    @Test
    fun `different keys load separately`() = runBlocking {
        assertEquals("a", load("a", "a"))
        assertEquals("b", load("b", "b"))
        assertEquals(2, calls.get())
    }

    @Test
    fun `clear makes the next load call again`() = runBlocking {
        load()
        loads.clear()
        load()
        assertEquals(2, calls.get())
    }

    @Test
    fun `a failure is not kept`() = runBlocking {
        try {
            loads.load("k") { calls.incrementAndGet(); throw IllegalStateException("boom") }
            fail()
        } catch (_: IllegalStateException) {
        }
        assertEquals("v", load())
        assertEquals(2, calls.get())
    }

    @Test
    fun `a cancelled load is run again by the caller that joined it`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val first = launch { loads.load("k") { calls.incrementAndGet(); started.complete(Unit); CompletableDeferred<String>().await() } }
        started.await()
        val second = async { load() }
        yield()
        first.cancelAndJoin()
        assertEquals("v", second.await())
        assertEquals(2, calls.get())
    }

    @Test
    fun `beyond the capacity the oldest finished results are dropped`() = runBlocking {
        val small = GiteaSharedLoads(reuseFor = Long.MAX_VALUE, maxEntries = 2, clock = { now })
        suspend fun get(key: String) = small.load(key) { calls.incrementAndGet(); key }
        get("a"); now++
        get("b"); now++
        get("c") // drops "a"
        assertEquals(3, calls.get())
        get("b"); get("c")
        assertEquals(3, calls.get())
        get("a")
        assertEquals(4, calls.get())
    }

    @Test
    fun `a load still running is never dropped`() = runBlocking {
        val small = GiteaSharedLoads(reuseFor = Long.MAX_VALUE, maxEntries = 1, clock = { now })
        val gate = CompletableDeferred<Unit>()
        val running = async { small.load("slow") { calls.incrementAndGet(); gate.await(); "slow" } }
        yield()
        small.load("quick") { calls.incrementAndGet(); "quick" } // over capacity, but "slow" is still running
        val joined = async { small.load("slow") { calls.incrementAndGet(); "again" } }
        yield()
        gate.complete(Unit)
        assertEquals("slow", running.await())
        assertEquals("slow", joined.await())
        assertEquals(2, calls.get())
    }
}
