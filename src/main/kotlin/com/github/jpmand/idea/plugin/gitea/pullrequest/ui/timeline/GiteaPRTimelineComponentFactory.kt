package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.codereview.avatar.Avatar
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import java.awt.datatransfer.StringSelection
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Hand-built, read-only rendering of a PR's activity timeline (Conversation): header (title +
 * description + author) followed by a chronological list of comment / commit / review / event rows,
 * each with the actor's avatar, name and timestamp. Milestone-1 scope — no editing, no reactions,
 * no collapsible threads; markdown bodies are shown as plain wrapped text.
 */
@Suppress("UnstableApiUsage")
object GiteaPRTimelineComponentFactory {

    private val DATE_FORMAT = SimpleDateFormat("d MMM yyyy, HH:mm")

    fun create(
        cs: CoroutineScope,
        vm: GiteaPRTimelineViewModel,
        avatars: IconsProvider<GiteaUser>,
        onRefresh: () -> Unit,
    ): JComponent {
        val list = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(12, 16)
        }

        val headerActions = listOf(
            browserAction("pull.request.action.open.pr.in.browser", vm.pr.htmlUrl),
            copyAction("pull.request.action.copy.pr.link", vm.pr.htmlUrl),
        )

        fun rebuild(rows: List<JComponent>) {
            list.removeAll()
            list.add(header(vm, avatars).withPopup(headerActions))
            list.add(strut(12))
            rows.forEachIndexed { i, row ->
                if (i > 0) list.add(strut(10))
                list.add(row)
            }
            list.revalidate()
            list.repaint()
        }

        cs.launch {
            vm.items.collect { computed ->
                val res = computed?.result
                if (res == null) {
                    rebuild(listOf(infoRow(GiteaBundle.message("pull.request.timeline.loading"))))
                } else {
                    res.fold(
                        onSuccess = { items -> rebuild(items.map { row(it, avatars) }) },
                        onFailure = { rebuild(listOf(infoRow(GiteaBundle.message("pull.request.timeline.error")))) },
                    )
                }
            }
        }

        val refreshBar = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(4, 12)
            add(ActionLink(GiteaBundle.message("pull.request.timeline.refresh")) { onRefresh() })
        }

        return JPanel(java.awt.BorderLayout()).apply {
            add(refreshBar, java.awt.BorderLayout.NORTH)
            add(ScrollPaneFactory.createScrollPane(list, true), java.awt.BorderLayout.CENTER)
        }
    }

    // ── header ─────────────────────────────────────────────────────────────

    private fun header(vm: GiteaPRTimelineViewModel, avatars: IconsProvider<GiteaUser>): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JBLabel("${vm.title} ${vm.number}").apply { font = JBFont.h2() }.leftAligned())
            add(strut(6))
            add(actorLine(vm.author, vm.createdAt, avatars).leftAligned())
            vm.descriptionMarkdown?.let {
                add(strut(8))
                add(SimpleHtmlPane(StringUtil.escapeXmlEntities(it).replace("\n", "<br>")).leftAligned())
            }
        }

    // ── rows ───────────────────────────────────────────────────────────────

    private fun row(item: GiteaTimelineItem, avatars: IconsProvider<GiteaUser>): JComponent = when (item) {
        is GiteaTimelineItem.Comment -> commentRow(item, avatars).withPopup(
            listOfNotNull(
                item.htmlUrl?.let { browserAction("pull.request.action.open.comment.in.browser", it) },
                item.htmlUrl?.let { copyAction("pull.request.action.copy.comment.link", it) },
            ),
        )
        is GiteaTimelineItem.Review -> reviewRow(item, avatars).withPopup(
            listOfNotNull(
                item.htmlUrl?.let { browserAction("pull.request.action.open.comment.in.browser", it) },
                item.htmlUrl?.let { copyAction("pull.request.action.copy.comment.link", it) },
            ),
        )
        is GiteaTimelineItem.Commit -> commitRow(item, avatars).withPopup(
            listOfNotNull(
                item.htmlUrl?.let { browserAction("pull.request.action.open.commit.in.browser", it) },
                copyAction("pull.request.action.copy.commit.hash", item.sha),
            ),
        )
        is GiteaTimelineItem.Event -> eventRow(item, avatars)
    }

    private fun browserAction(bundleKey: String, url: String): AnAction =
        object : AnAction(GiteaBundle.message(bundleKey)) {
            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(url)
        }

    private fun copyAction(bundleKey: String, value: String): AnAction =
        object : AnAction(GiteaBundle.message(bundleKey)) {
            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun actionPerformed(e: AnActionEvent) =
                CopyPasteManager.getInstance().setContents(StringSelection(value))
        }

    private fun <T : JComponent> T.withPopup(actions: List<AnAction>): T = apply {
        if (actions.isEmpty()) return@apply
        val group = DefaultActionGroup(actions)
        PopupHandler.installPopupMenu(this, group, "GiteaPRTimelinePopup")
    }

    private fun commentRow(item: GiteaTimelineItem.Comment, avatars: IconsProvider<GiteaUser>): JComponent =
        verticalCard {
            add(actorLine(item.actor, item.timestamp, avatars).leftAligned())
            add(strut(4))
            add(SimpleHtmlPane(bodyHtml(item.body)).leftAligned())
        }

    private fun reviewRow(item: GiteaTimelineItem.Review, avatars: IconsProvider<GiteaUser>): JComponent =
        verticalCard {
            val verb = when (item.state) {
                GiteaReviewState.APPROVED -> GiteaBundle.message("pull.request.timeline.review.approved")
                GiteaReviewState.REQUEST_CHANGES -> GiteaBundle.message("pull.request.timeline.review.changes")
                else -> GiteaBundle.message("pull.request.timeline.review.commented")
            }
            add(actorLine(item.actor, item.timestamp, avatars, suffix = verb).leftAligned())
            if (!item.body.isNullOrBlank()) {
                add(strut(4))
                add(SimpleHtmlPane(bodyHtml(item.body)).leftAligned())
            }
            item.threads.forEach { thread ->
                add(strut(6))
                val location = buildString {
                    append(thread.path ?: "")
                    (thread.newLine ?: thread.oldLine)?.let { append(":").append(it) }
                }
                add(JBLabel(location).apply {
                    foreground = UIUtil.getContextHelpForeground()
                    font = JBFont.small()
                }.leftAligned())
                thread.comments.forEach { c ->
                    add(strut(2))
                    add(JBLabel("<html><b>${esc(c.author?.login ?: "")}</b>: ${esc(c.body ?: "")}</html>").leftAligned())
                }
            }
        }

    private fun commitRow(item: GiteaTimelineItem.Commit, avatars: IconsProvider<GiteaUser>): JComponent {
        val link = ActionLink("${item.shortSha}  ${item.messageTitle}") {
            item.htmlUrl?.let { BrowserUtil.browse(it) }
        }
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(actorLine(item.actor, item.timestamp, avatars,
                suffix = GiteaBundle.message("pull.request.timeline.commit.added"),
                icon = CollaborationToolsIcons.Review.Branch).leftAligned())
            add(strut(2))
            add(link.leftAligned())
        }
    }

    private fun eventRow(item: GiteaTimelineItem.Event, avatars: IconsProvider<GiteaUser>): JComponent {
        val actor = item.actor?.let { it.fullName ?: it.login } ?: ""
        val detail = when (item.kind) {
            GiteaTimelineItem.Event.Kind.CLOSED -> GiteaBundle.message("pull.request.timeline.event.closed")
            GiteaTimelineItem.Event.Kind.REOPENED -> GiteaBundle.message("pull.request.timeline.event.reopened")
            GiteaTimelineItem.Event.Kind.MERGED -> GiteaBundle.message("pull.request.timeline.event.merged")
            GiteaTimelineItem.Event.Kind.LABEL_ADDED ->
                GiteaBundle.message("pull.request.timeline.event.label.added", item.label?.name ?: "")
            GiteaTimelineItem.Event.Kind.LABEL_REMOVED ->
                GiteaBundle.message("pull.request.timeline.event.label.removed", item.label?.name ?: "")
            GiteaTimelineItem.Event.Kind.MILESTONE_CHANGED ->
                GiteaBundle.message("pull.request.timeline.event.milestone.changed")
            GiteaTimelineItem.Event.Kind.ASSIGNED -> GiteaBundle.message("pull.request.timeline.event.assigned")
            GiteaTimelineItem.Event.Kind.UNASSIGNED -> GiteaBundle.message("pull.request.timeline.event.unassigned")
            GiteaTimelineItem.Event.Kind.REVIEW_REQUESTED ->
                GiteaBundle.message("pull.request.timeline.event.review.requested", item.user?.login ?: "")
            GiteaTimelineItem.Event.Kind.REVIEW_REQUEST_REMOVED ->
                GiteaBundle.message("pull.request.timeline.event.review.request.removed", item.user?.login ?: "")
            GiteaTimelineItem.Event.Kind.REVIEW_DISMISSED ->
                GiteaBundle.message("pull.request.timeline.event.review.dismissed")
            GiteaTimelineItem.Event.Kind.TITLE_CHANGED ->
                GiteaBundle.message("pull.request.timeline.event.title.changed")
            GiteaTimelineItem.Event.Kind.BASE_BRANCH_CHANGED ->
                GiteaBundle.message("pull.request.timeline.event.base.changed", item.oldValue ?: "", item.newValue ?: "")
            GiteaTimelineItem.Event.Kind.HEAD_BRANCH_DELETED ->
                GiteaBundle.message("pull.request.timeline.event.head.deleted")
            GiteaTimelineItem.Event.Kind.LOCKED -> GiteaBundle.message("pull.request.timeline.event.locked")
            GiteaTimelineItem.Event.Kind.UNLOCKED -> GiteaBundle.message("pull.request.timeline.event.unlocked")
            GiteaTimelineItem.Event.Kind.REFERENCED -> GiteaBundle.message("pull.request.timeline.event.referenced")
        }
        return JBLabel("<html><b>${esc(actor)}</b> ${esc(detail)} <span color='gray'>· ${fmt(item.timestamp)}</span></html>")
            .apply { foreground = UIUtil.getContextHelpForeground() }
            .leftAligned()
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun verticalCard(build: JPanel.() -> Unit): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(4, 0)
            build()
        }

    private fun actorLine(
        actor: GiteaUser?,
        date: Date,
        avatars: IconsProvider<GiteaUser>,
        suffix: String? = null,
        icon: Icon? = null,
    ): JComponent {
        val name = actor?.let { it.fullName ?: it.login } ?: "—"
        val text = buildString {
            append("<html><b>").append(esc(name)).append("</b>")
            if (suffix != null) append(" ").append(esc(suffix))
            append(" <span color='gray'>· ").append(fmt(date)).append("</span></html>")
        }
        return JBLabel(text, icon ?: actor?.let { avatars.getIcon(it, Avatar.Sizes.BASE) }, SwingConstants.LEADING)
    }

    private fun infoRow(text: String): JComponent =
        JBLabel(text).apply { foreground = UIUtil.getContextHelpForeground() }.leftAligned()

    private fun strut(height: Int) = javax.swing.Box.createVerticalStrut(JBUI.scale(height))

    private fun bodyHtml(body: String?): String =
        if (body.isNullOrBlank()) "<i>(no description)</i>" else "<html>${esc(body).replace("\n", "<br>")}</html>"

    private fun esc(s: String): String = StringUtil.escapeXmlEntities(s)

    private fun fmt(date: Date): String = DATE_FORMAT.format(date)

    private fun <T : JComponent> T.leftAligned(): T = apply { alignmentX = java.awt.Component.LEFT_ALIGNMENT }
}
