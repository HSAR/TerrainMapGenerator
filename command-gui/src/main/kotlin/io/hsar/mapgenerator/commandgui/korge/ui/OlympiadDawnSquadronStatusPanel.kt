package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnSquadronStatus
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal object OlympiadDawnSquadronStatusPanel {
    fun height(status: OlympiadDawnSquadronStatus, collapsed: Boolean): Double =
        if (collapsed) HEADER_HEIGHT else HEADER_HEIGHT + OlympiadDawnStatusRows.POINT_ROW_HEIGHT + status.weapons.size * ROW_HEIGHT

    fun render(
        parent: Container,
        width: Double,
        status: OlympiadDawnSquadronStatus,
        collapsed: Boolean,
        editable: Boolean,
        onCollapsedChanged: () -> Unit,
        onRemainingChanged: (Int) -> Unit,
    ) {
        MapViewerCollapsiblePanel.header(parent, width, HEADER_HEIGHT, "Squadron Status", !collapsed, onCollapsedChanged)
        if (collapsed) return

        val remaining = status.remaining.coerceIn(0, status.count)
        OlympiadDawnStatusRows.renderPoints(
            parent = parent,
            top = HEADER_HEIGHT,
            width = width,
            label = "Health",
            count = status.count,
            destroyed = (remaining until status.count).toSet(),
            editable = editable,
        ) { index ->
            onRemainingChanged(if (index < remaining) remaining - 1 else remaining + 1)
        }
        status.weapons.forEachIndexed { index, weapon ->
            val row = parent.container().also { it.xy(0.0, HEADER_HEIGHT + OlympiadDawnStatusRows.POINT_ROW_HEIGHT + index * ROW_HEIGHT) }
            MapViewerUiBoxes.box(row, width, ROW_HEIGHT, active = remaining > 0)
            val arcs = weapon.arcs.takeIf(List<String>::isNotEmpty)?.joinToString(", ")?.let { " [$it]" }.orEmpty()
            MapViewerTextViews.leftBox(
                row,
                "${remaining} ${weapon.name}$arcs",
                width - TEXT_INSET,
                ROW_HEIGHT,
                color = if (remaining == 0) Palette.Colours.RED else Palette.Colours.WHITE,
            ) { x = TEXT_INSET }
        }
    }

    private const val HEADER_HEIGHT = 34.0
    private const val ROW_HEIGHT = 30.0
    private const val TEXT_INSET = 6.0
}
