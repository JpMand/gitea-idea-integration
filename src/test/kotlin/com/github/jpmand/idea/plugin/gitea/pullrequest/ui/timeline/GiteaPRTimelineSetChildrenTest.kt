package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.awt.Component
import java.awt.Container
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.random.Random

class GiteaPRTimelineSetChildrenTest {

    private fun labels(n: Int) = List(n) { JLabel("$it") }

    private fun Container.children(): List<Component> = components.toList()

    @Test
    fun `fills an empty container`() {
        val panel = JPanel()
        val items = labels(3)
        setChildrenInPlace(panel, items)
        assertEquals(items, panel.children())
    }

    @Test
    fun `reorders, drops and adds children`() {
        val panel = JPanel()
        val (a, b, c, d) = labels(4)
        setChildrenInPlace(panel, listOf(a, b, c))

        setChildrenInPlace(panel, listOf(c, d, a))
        assertEquals(listOf(c, d, a), panel.children())

        setChildrenInPlace(panel, listOf(a))
        assertEquals(listOf(a), panel.children())
    }

    @Test
    fun `a child that stays in place is not re-added`() {
        val panel = JPanel()
        val (a, b, c) = labels(3)
        setChildrenInPlace(panel, listOf(a, b))
        var removed = 0
        panel.addContainerListener(object : java.awt.event.ContainerAdapter() {
            override fun componentRemoved(e: java.awt.event.ContainerEvent) {
                removed++
            }
        })

        setChildrenInPlace(panel, listOf(a, b, c))
        assertEquals(0, removed)
        assertSame(a, panel.getComponent(0))
    }

    @Test
    fun `any sequence of updates ends with the requested children`() {
        val pool = labels(8)
        val panel = JPanel()
        val random = Random(42)
        repeat(500) {
            val next = pool.shuffled(random).take(random.nextInt(pool.size + 1))
            setChildrenInPlace(panel, next)
            assertEquals(next, panel.children())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a repeated component is rejected`() {
        val (a, b) = labels(2)
        setChildrenInPlace(JPanel(), listOf(a, b, a))
    }
}
