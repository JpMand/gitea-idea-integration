package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.text.SimpleDateFormat
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants

/**
 * Two-row cell renderer for a pull-request list item.
 *
 * Row 1 (full-width):
 *  [Title text — truncates with "…" as needed] [🏷️ label icon] [💬 N]
 *
 * Row 2 (smaller grey text):
 *  [#number]  [created MM/dd/yyyy]  [by author]
 */
class GiteaPRListItemComponent : JPanel(GridBagLayout()), ListCellRenderer<GiteaPullRequest> {

    // ── Row 1 widgets ──────────────────────────────────────────────────────────
    private val titleLabel = JLabel().apply {
        font = UIUtil.getLabelFont()
        horizontalAlignment = SwingConstants.LEFT
    }

    /** Tag icon — always present; tooltip carries HTML-coloured label names. */
    private val labelIconLabel = JLabel(AllIcons.Nodes.Tag).apply {
        // Start slightly faded; will be re-enabled when labels are present.
        isEnabled = false
    }

    /** Balloon/comment icon + count — hidden when there are no comments. */
    private val commentLabel = JLabel().apply {
        icon = AllIcons.General.Balloon
        iconTextGap = JBUI.scale(2)
        horizontalTextPosition = SwingConstants.RIGHT
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
    }

    // ── Row 2 widget ───────────────────────────────────────────────────────────
    private val metaLabel = JLabel().apply {
        font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
        foreground = UIUtil.getContextHelpForeground()
    }

    // ── Row 1 sub-layout ───────────────────────────────────────────────────────
    /** Right-side icon cluster: [label icon] [comment icon + count] */
    private val iconsPanel = JPanel(FlowLayout(FlowLayout.TRAILING, JBUI.scale(4), 0)).apply {
        isOpaque = false
        add(labelIconLabel)
        add(commentLabel)
    }

    /** Full row 1 panel: title fills all available width; icons cluster fixed on the right. */
    private val row1 = JPanel(BorderLayout(JBUI.scale(4), 0)).apply {
        isOpaque = false
        add(titleLabel, BorderLayout.CENTER)
        add(iconsPanel, BorderLayout.EAST)
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    init {
        isOpaque = true
        border = JBUI.Borders.empty(JBUI.scale(5), JBUI.scale(8))

        val gbc = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            weighty = 0.0
            anchor = GridBagConstraints.NORTHWEST
        }

        // Row 1 with a small bottom gap
        gbc.gridy = 0
        gbc.insets = Insets(0, 0, JBUI.scale(2), 0)
        add(row1, gbc)

        // Row 2
        gbc.gridy = 1
        gbc.insets = Insets(0, 0, 0, 0)
        add(metaLabel, gbc)
    }

    // ── Renderer ──────────────────────────────────────────────────────────────
    override fun getListCellRendererComponent(
        list: JList<out GiteaPullRequest>,
        value: GiteaPullRequest?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        value ?: return this

        // ── Selection colours ────────────────────────────────────────────────
        val bg = if (isSelected) list.selectionBackground else list.background
        val fg = if (isSelected) list.selectionForeground else list.foreground
        val secondaryFg = if (isSelected) list.selectionForeground
        else UIUtil.getContextHelpForeground()

        background = bg
        titleLabel.foreground = fg
        metaLabel.foreground = secondaryFg
        commentLabel.foreground = secondaryFg

        // ── Row 1: title ──────────────────────────────────────────────────────
        titleLabel.text = value.title

        // ── Row 1: label icon ─────────────────────────────────────────────────
        if (value.labels.isNotEmpty()) {
            val labelsHtml = value.labels.joinToString(", ") { label ->
                // Gitea color is already "#rrggbb"; guard against missing '#'
                val hex = label.color.let { if (it.startsWith("#")) it else "#$it" }
                "<font color='$hex'>\u25a0 ${label.name}</font>"
            }
            labelIconLabel.toolTipText = "<html>$labelsHtml</html>"
            labelIconLabel.isEnabled = true
        } else {
            labelIconLabel.toolTipText = null
            labelIconLabel.isEnabled = false
        }

        // ── Row 1: comment count ──────────────────────────────────────────────
        val commentCount = value.reviewComments
        if (commentCount > 0) {
            commentLabel.text = commentCount.toString()
            commentLabel.isVisible = true
        } else {
            commentLabel.text = ""
            commentLabel.isVisible = false
        }

        // ── Row 2: meta info ──────────────────────────────────────────────────
        val dateStr = DATE_FORMAT.format(value.createdAt)
        val draftMarker = if (value.draft) "  [draft]" else ""
        metaLabel.text = "#${value.number}   created $dateStr   by ${value.author.login}$draftMarker"

        return this
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("MM/dd/yyyy")
    }
}