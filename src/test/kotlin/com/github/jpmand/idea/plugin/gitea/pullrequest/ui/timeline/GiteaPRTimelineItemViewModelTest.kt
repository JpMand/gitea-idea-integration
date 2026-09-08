package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class GiteaPRTimelineItemViewModelTest {

    private fun date(min: Int) = Date(1_700_000_000_000L + min * 60_000L)
    private fun comment(min: Int) = GiteaTimelineItem.Comment(min.toLong(), null, date(min), "c$min", null)
    private fun commit(min: Int) = GiteaTimelineItem.Commit(min.toLong(), null, date(min), "sha$min", "sha$min", "msg$min", null)
    private fun review(min: Int) =
        GiteaTimelineItem.Review(min.toLong(), null, date(min), GiteaReviewState.COMMENT, null, null, emptyList())

    @Test
    fun `consecutive commits fold into one Commits block`() {
        val items = listOf(comment(0), commit(1), commit(2), commit(3), comment(4)).toItemViewModels()

        assertEquals(3, items.size)
        assertEquals(GiteaPRTimelineItemViewModel.Comment::class, items[0]::class)
        val commits = items[1] as GiteaPRTimelineItemViewModel.Commits
        assertEquals(3, commits.commits.size)
        assertEquals(GiteaPRTimelineItemViewModel.Comment::class, items[2]::class)
    }

    @Test
    fun `non-adjacent commit runs stay separate`() {
        val items = listOf(commit(0), review(1), commit(2), commit(3)).toItemViewModels()

        assertEquals(3, items.size)
        assertEquals(1, (items[0] as GiteaPRTimelineItemViewModel.Commits).commits.size)
        assertEquals(GiteaPRTimelineItemViewModel.Review::class, items[1]::class)
        assertEquals(2, (items[2] as GiteaPRTimelineItemViewModel.Commits).commits.size)
    }

    @Test
    fun `trailing commits are flushed`() {
        val items = listOf(comment(0), commit(1), commit(2)).toItemViewModels()
        assertEquals(2, items.size)
        assertEquals(2, (items[1] as GiteaPRTimelineItemViewModel.Commits).commits.size)
    }
}
