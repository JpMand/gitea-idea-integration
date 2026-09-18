package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPullRequestSortEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GiteaPRListSearchValueTest {

    @Test
    fun `default value has no active filters`() {
        assertEquals(0, GiteaPRListSearchValue.DEFAULT.filterCount)
    }

    @Test
    fun `filterCount counts each non-default facet`() {
        val value = GiteaPRListSearchValue(
            searchQuery = "fix",
            state = GiteaPRListSearchValue.State.CLOSED,
            label = "bug",
            author = "octocat",
            sort = GiteaPRListSearchValue.Sort.OLDEST,
        )
        assertEquals(5, value.filterCount)
    }

    @Test
    fun `open state does not count towards filterCount`() {
        assertEquals(
            0,
            GiteaPRListSearchValue(state = GiteaPRListSearchValue.State.OPEN).filterCount,
        )
        assertEquals(
            1,
            GiteaPRListSearchValue(state = GiteaPRListSearchValue.State.ALL).filterCount,
        )
    }

    @Test
    fun `NEWEST sort maps to no api sort param`() {
        assertNull(GiteaPRListSearchValue.Sort.NEWEST.api)
        assertEquals(GiteaPullRequestSortEnum.MOSTCOMMENT, GiteaPRListSearchValue.Sort.MOST_COMMENTED.api)
    }
}
