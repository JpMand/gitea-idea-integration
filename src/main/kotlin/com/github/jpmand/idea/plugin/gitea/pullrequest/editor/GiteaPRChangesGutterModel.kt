package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRLiveDiffSync
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterActionableChangesModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterChangesRenderer
import com.intellij.collaboration.ui.codereview.editor.asLst
import com.intellij.collaboration.util.ExcludingApproximateChangedRangesShifter
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.diff.util.LineRange
import com.intellij.diff.util.Range
import com.intellij.diff.util.Side
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.ex.LstRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

private val LOG = logger<GiteaPRChangesGutterModel>()

/**
 * The lines the PR itself changes — base (merge base) to head — marked in a regular project
 * editor's gutter in the platform's review color, as the bundled GitHub/GitLab plugins do in their
 * in-editor review. The IDE's own gutter bar keeps showing only local changes against git HEAD,
 * which are empty right after checking a PR out.
 *
 * [prRanges] (base → head) are moved onto the live document through [localRanges] (head → live,
 * from [GiteaPRLiveDiffSync]); the lines a local edit covers are cut out of them, so the IDE's
 * own local-changes bar shows there instead.
 *
 * Clicking a marker opens the platform's popup: the base text of the range, copy, next/previous
 * change, and "Show Diff" ([showDiff]), a diff of the base file against the live document.
 *
 * Holds its ranges itself rather than delegating to the platform's
 * `MutableCodeReviewEditorGutterChangesModel`, which is `@ApiStatus.Internal`.
 */
@Suppress("UnstableApiUsage")
class GiteaPRChangesGutterModel(
    cs: CoroutineScope,
    private val project: Project,
    private val filename: String,
    private val baseSha: String,
    baseContent: String,
    private val document: Document,
    prRanges: List<Range>,
    localRanges: StateFlow<List<Range>>,
) : CodeReviewEditorGutterActionableChangesModel {

    private val baseContent = StringUtil.convertLineSeparators(baseContent)
    private val settings = project.service<GiteaPullRequestsSettings>()
    private val highlightListeners = CopyOnWriteArrayList<() -> Unit>()

    private val _reviewRanges = MutableStateFlow<List<LstRange>?>(null)
    override val reviewRanges: StateFlow<List<LstRange>?> = _reviewRanges.asStateFlow()

    override fun isValid(): Boolean = _reviewRanges.value != null

    init {
        cs.launchNow {
            localRanges.collect { local -> _reviewRanges.value = shiftPRRanges(prRanges, local).map { it.asLst() } }
        }
        // The setting is shared by every editor, so each one repaints when any of them toggles it.
        cs.launch {
            settings.highlightDiffRangesInEditorState.drop(1).collect { highlightListeners.forEach { it() } }
        }
    }

    override var shouldHighlightDiffRanges: Boolean
        get() = settings.highlightDiffRangesInEditor
        set(value) {
            settings.highlightDiffRangesInEditor = value
        }

    override fun addDiffHighlightListener(disposable: Disposable, listener: () -> Unit) {
        highlightListeners.add(listener)
        Disposer.register(disposable) { highlightListeners.remove(listener) }
    }

    override fun getBaseContent(lines: LineRange): String? = linesText(baseContent, lines)

    override fun showDiff(lineIdx: Int?) {
        val contentFactory = DiffContentFactory.getInstance()
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(filename)
        val request = SimpleDiffRequest(
            filename,
            contentFactory.create(project, baseContent, fileType),
            contentFactory.create(project, document),
            GiteaBundle.message("pull.request.diff.side.base", baseSha.take(7)),
            GiteaBundle.message("pull.request.editor.changes.diff.side.local"),
        )
        if (lineIdx != null) request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.RIGHT, lineIdx))
        LOG.info("Showing the PR changes diff of '$filename'")
        DiffManager.getInstance().showDiff(project, request)
    }
}

/**
 * Shows [GiteaPRChangesGutterModel]'s markers in [editor] while the review threads are shown at
 * all: "Do Not Show" in the review toolbar turns the in-editor review off, as in the GitHub plugin.
 */
@Suppress("UnstableApiUsage")
fun CoroutineScope.installPRChangesGutter(
    project: Project,
    editor: Editor,
    filename: String,
    baseSha: String,
    baseContent: String,
    headContent: String,
    fileAdded: Boolean,
    sync: GiteaPRLiveDiffSync,
    discussionsVm: GiteaPRDiscussionsViewModels,
) {
    launch {
        val prRanges = withContext(Dispatchers.Default) { prChangedRanges(baseContent, headContent, fileAdded) }
        LOG.debug("PR changes gutter on $filename: ${prRanges.size} ranges against $baseSha")
        discussionsVm.discussionsViewOption
            .map { it != DiscussionsViewOption.DONT_SHOW }
            .distinctUntilChanged()
            .collectLatest { shown ->
                if (!shown) return@collectLatest
                coroutineScope {
                    val model = GiteaPRChangesGutterModel(
                        this, project, filename, baseSha, baseContent, editor.document, prRanges, sync.ranges,
                    )
                    // Never returns: shows the markers until this scope is cancelled.
                    CodeReviewEditorGutterChangesRenderer.render(model, editor)
                }
            }
    }
}

/**
 * The PR's own line changes, [baseContent] → [headContent], numbered as an editor [Document]
 * numbers lines. An added file is a single insertion of all of its lines.
 */
internal fun prChangedRanges(baseContent: String, headContent: String, fileAdded: Boolean): List<Range> {
    val head = StringUtil.convertLineSeparators(headContent)
    if (fileAdded) return listOf(Range(0, 0, 0, LineOffsetsUtil.create(head).lineCount))
    val base = StringUtil.convertLineSeparators(baseContent)
    return ComparisonManager.getInstance()
        .compareLines(base, head, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE)
        .map { Range(it.startLine1, it.endLine1, it.startLine2, it.endLine2) }
}

/**
 * [prRanges] (base → head) moved onto the live document through [localRanges] (head → live): the
 * base side stays, the head side becomes live lines. The lines a local edit covers are cut out —
 * the IDE's own local-changes bar marks those.
 */
internal fun shiftPRRanges(prRanges: List<Range>, localRanges: List<Range>): List<Range> =
    if (localRanges.isEmpty()) prRanges else ExcludingApproximateChangedRangesShifter.shift(prRanges, localRanges)

/** The text of [lines] in [content] (`\n`-separated), without the last line's newline; null when out of range. */
internal fun linesText(content: String, lines: LineRange): String? {
    if (lines.start == lines.end) return ""
    val offsets = LineOffsetsUtil.create(content)
    if (lines.start < 0 || lines.end > offsets.lineCount) return null
    return content.substring(offsets.getLineStart(lines.start), offsets.getLineEnd(lines.end - 1))
}
