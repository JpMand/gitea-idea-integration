package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shares the result of identical loads. Opening a pull request starts several view models at once
 * (details, status, review threads, Conversation tab), and each loads the PR, its reviews, commits
 * and so on: a load already running is joined, and one that finished less than [reuseFor] ago is
 * reused. Failures are not kept.
 *
 * Anything that changes data on the server must [clear] it, so the reload that follows sees the
 * change; so must an explicit refresh.
 *
 * At most [maxEntries] results are kept: beyond that, the oldest finished ones are dropped (a load
 * still running never is), which bounds a cache whose results don't expire.
 */
class GiteaSharedLoads(
    private val reuseFor: Long = TimeUnit.SECONDS.toNanos(3),
    private val maxEntries: Int = Int.MAX_VALUE,
    private val clock: () -> Long = System::nanoTime,
) {
    private class Entry {
        val result = CompletableDeferred<Any?>()
        @Volatile
        var completedAt: Long = 0
    }

    private val entries = ConcurrentHashMap<Any, Entry>()

    suspend fun <T> load(key: Any, load: suspend () -> T): T {
        while (true) {
            val now = clock()
            if (reuseFor != Long.MAX_VALUE) entries.values.removeIf { it.isExpired(now) }
            val mine = Entry()
            val entry = entries.compute(key) { _, old -> if (old != null && !old.isExpired(now)) old else mine }!!
            if (entry === mine) {
                try {
                    val value = load()
                    mine.completedAt = clock()
                    mine.result.complete(value)
                    trimToSize()
                    return value
                } catch (e: Throwable) {
                    entries.remove(key, mine)
                    mine.result.completeExceptionally(e)
                    throw e
                }
            }
            try {
                @Suppress("UNCHECKED_CAST")
                return entry.result.await() as T
            } catch (e: CancellationException) {
                // The load we joined was cancelled with its caller; unless this caller is cancelled
                // too, run the load again.
                currentCoroutineContext().ensureActive()
            }
        }
    }

    fun clear() = entries.clear()

    private fun trimToSize() {
        val excess = entries.size - maxEntries
        if (excess <= 0) return
        entries.entries
            .filter { it.value.result.isCompleted }
            .sortedBy { it.value.completedAt }
            .take(excess)
            .forEach { entries.remove(it.key, it.value) }
    }

    private fun Entry.isExpired(now: Long): Boolean = result.isCompleted && now - completedAt >= reuseFor
}
