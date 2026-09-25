package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PRBranchInfo
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Repository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GiteaPRHeadRefTest {

  private fun pr(headRepoId: Long, baseRepoId: Long = 1L) = GiteaPullRequest.fromDto(
    PullRequest(
      number = 1,
      head = PRBranchInfo(ref = "feature/greeting", repo = Repository(id = headRepoId)),
      base = PRBranchInfo(ref = "main", repo = Repository(id = baseRepoId)),
    ),
  )

  @Test
  fun `same-repo PR is fetched into its branch's remote-tracking ref`() {
    val head = GiteaPRHeadRef.ofBranch(pr(headRepoId = 1L), "origin")!!
    assertEquals("+refs/heads/feature/greeting:refs/remotes/origin/feature/greeting", head.refspec)
    assertEquals("refs/remotes/origin/feature/greeting", head.localRef)
    assertEquals("origin/feature/greeting", head.remoteBranch)
  }

  @Test
  fun `fork PR has no branch ref and uses the pull ref under the remote`() {
    assertNull(GiteaPRHeadRef.ofBranch(pr(headRepoId = 2L), "origin"))
    val head = GiteaPRHeadRef.ofPullRef(pr(headRepoId = 2L), "origin")
    assertEquals("+refs/pull/1/head:refs/remotes/origin/pull/1", head.refspec)
    assertEquals("refs/remotes/origin/pull/1", head.localRef)
    assertNull(head.remoteBranch)
  }
}
