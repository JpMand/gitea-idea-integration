package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.mention

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement

/**
 * Set on a comment editor's [com.intellij.openapi.editor.Editor] user data to both provide the
 * candidate list and scope [GiteaMentionCompletionContributor] to that editor — see
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory].
 * Candidates are repo collaborators (`GiteaPRRepository.loadPossibleAuthors()`) plus the current
 * PR's own author and requested reviewers (`GiteaPullRequest.mentionCandidates()`) — someone
 * clearly relevant to *this* PR specifically isn't necessarily an explicit repo collaborator.
 * Loaded once per PR session and filtered client-side here — no live per-keystroke server search
 * (Gitea's `/users/search` could support one, but every user on the instance being a completion
 * candidate is a much bigger surface than this PR's own participants).
 */
val GITEA_MENTION_CANDIDATES_KEY: Key<List<GiteaUser>> = Key.create("Gitea.Mention.Candidates")

/**
 * `@`-triggered mention completion. Registered for `language="any"` in plugin.xml — the comment
 * editor is Markdown-typed when the bundled Markdown plugin is enabled, plain text otherwise (see
 * `CodeReviewMarkdownEditor.create`), so the language can't be pinned statically. Scoping is done
 * entirely via [GITEA_MENTION_CANDIDATES_KEY]: [fillCompletionVariants] contributes nothing unless
 * that key is present on the editor, so this never fires in unrelated editors elsewhere in the IDE.
 */
class GiteaMentionCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val candidates = parameters.editor.getUserData(GITEA_MENTION_CANDIDATES_KEY) ?: return
        val prefix = mentionPrefix(parameters) ?: return
        val scoped = result.withPrefixMatcher(prefix)
        candidates.forEach { user ->
            scoped.addElement(
                LookupElementBuilder.create(user.login)
                    .withTypeText(user.fullName?.takeIf { it.isNotBlank() }, true),
            )
        }
    }

    /**
     * Auto-popup on `@` everywhere — not just scoped editors. Harmless: [fillCompletionVariants]
     * still contributes nothing outside a scoped editor, and other contributors for that language
     * (if any) are unaffected. Avoided the alternative (resolving the [Editor] from [position] via
     * `PsiEditorUtil` to check the scope key here too) since that lookup is unreliable for an
     * editor embedded in a Swing panel rather than opened through `FileEditorManager`.
     *
     * `invokeAutoPopup` is soft-deprecated in favor of a `TypedHandlerDelegate` (its documented
     * caveat — `position` can come from uncommitted PSI — doesn't affect this simple `'@'` check,
     * and a manual Ctrl+Space always still works regardless), so a second extension-point class
     * isn't worth it just for the auto-popup nicety.
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean = typeChar == '@'

    /** The `@`-prefixed run of mention characters immediately before the caret, or `null` if the
     * caret isn't in one (e.g. no `@` found before hitting whitespace/start of document). */
    private fun mentionPrefix(parameters: CompletionParameters): String? {
        val offset = parameters.offset
        val text = parameters.editor.document.charsSequence
        var start = offset
        while (start > 0 && isMentionChar(text[start - 1])) start--
        if (start == 0 || text[start - 1] != '@') return null
        return text.subSequence(start, offset).toString()
    }

    private fun isMentionChar(c: Char): Boolean = c.isLetterOrDigit() || c == '-' || c == '_'
}
