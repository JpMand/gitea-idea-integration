package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import org.junit.Assert.assertEquals
import org.junit.Test

class GiteaPRDiffParsingTest {

  // Shaped like Gitea 1.27's `compare/{base}...{head}?output=diff` reply.
  private val diff = """
    diff --git a/docs/notes.md b/docs/notes.md
    deleted file mode 100644
    index 1f0e0f5..0000000
    --- a/docs/notes.md
    +++ /dev/null
    @@ -1,3 +0,0 @@
    -# Notes
    -
    -Scratch notes; to be removed.
    diff --git a/src/App.java b/src/App.java
    index 3c4a0e1..9b7f2d2 100644
    --- a/src/App.java
    +++ b/src/App.java
    @@ -4,4 +4,5 @@ public class App {
         public static void main(String[] args) {
    -        run();
    +        --- a/not/a/header
    +        +++ b/not/a/header
    +        run(args);
         }
    diff --git a/src/Greeting.java b/src/Greeting.java
    new file mode 100644
    index 0000000..b2f3cd0
    --- /dev/null
    +++ b/src/Greeting.java
    @@ -0,0 +1,2 @@
    +class Greeting {
    +}
    diff --git a/old name.txt b/new name.txt
    similarity index 100%
    rename from old name.txt
    rename to new name.txt
    diff --git a/logo.png b/logo.png
    index 1111111..2222222 100644
    Binary files a/logo.png and b/logo.png differ
  """.trimIndent()

  @Test
  fun `reads each file's name, status and line counts`() {
    val files = parseDiffChangedFiles(diff)
    assertEquals(
      listOf(
        GiteaPRChangedFile("docs/notes.md", null, GiteaPRFileStatusEnum.DELETED, 0, 3, 3),
        GiteaPRChangedFile("src/App.java", null, GiteaPRFileStatusEnum.MODIFIED, 3, 1, 4),
        GiteaPRChangedFile("src/Greeting.java", null, GiteaPRFileStatusEnum.ADDED, 2, 0, 2),
        GiteaPRChangedFile("new name.txt", "old name.txt", GiteaPRFileStatusEnum.RENAMED, 0, 0, 0),
        GiteaPRChangedFile("logo.png", null, GiteaPRFileStatusEnum.MODIFIED, 0, 0, 0),
      ),
      files,
    )
  }

  @Test
  fun `reads quoted names`() {
    val quoted = """
      diff --git "a/caf\303\251 \"menu\".txt" "b/caf\303\251 \"menu\".txt"
      new file mode 100644
      --- /dev/null
      +++ "b/caf\303\251 \"menu\".txt"
      @@ -0,0 +1 @@
      +x
    """.trimIndent()
    assertEquals(listOf("café \"menu\".txt"), parseDiffChangedFiles(quoted).map { it.filename })
  }

  @Test
  fun `a name containing the b-slash separator survives a mode-only change`() {
    val modeOnly = """
      diff --git a/x b/y.sh b/x b/y.sh
      old mode 100644
      new mode 100755
    """.trimIndent()
    assertEquals(listOf("x b/y.sh"), parseDiffChangedFiles(modeOnly).map { it.filename })
  }

  @Test
  fun `an empty diff has no files`() {
    assertEquals(emptyList<GiteaPRChangedFile>(), parseDiffChangedFiles(""))
  }
}
