package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The persisted PR settings state must have a generated kotlinx.serialization serializer —
 * without the serialization compiler plugin, `State.serializer()` doesn't exist and the platform
 * fails to save/load the component ("Serializer for class 'State' is not found").
 */
class GiteaPullRequestsSettingsStateTest {

  @Test
  fun `state round-trips through its generated serializer`() {
    val state = GiteaPullRequestsSettings.State(
      selectedUrlAndAccountId = "http://localhost:3000/acme/webapp" to "account-1",
      changesGrouping = setOf("directory"),
      editorReviewViewOption = DiscussionsViewOption.ALL,
      viewedPrFiles = mapOf(1 to setOf("src/App.java", "README.md")),
      draftComments = mapOf(
        1 to listOf(GiteaPRDraftComment(localId = 0, path = "src/App.java", newLine = 7, oldLine = null, body = "Draft")),
      ),
    )
    val serializer = GiteaPullRequestsSettings.State.serializer()
    val json = Json.encodeToString(serializer, state)
    assertEquals(state, Json.decodeFromString(serializer, json))
  }
}
