package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal object MapViewerCollapsiblePanel {
    fun header(
        parent: Container,
        width: Double,
        height: Double,
        label: String,
        expanded: Boolean,
        onToggle: () -> Unit,
    ): Container {
        val header = parent.container()
        MapViewerUiBoxes.box(header, width, height)
        MapViewerTextViews.leftBox(
            parent = header,
            text = label,
            width = width - ARROW_WIDTH - LABEL_GAP,
            height = height,
            color = MapViewerUiStateStyle.text(expanded),
        ).xy(LABEL_GAP, 0.0)
        val arrow = header.container().also { it.xy(width - ARROW_WIDTH, 0.0) }
        MapViewerUiBoxes.button(arrow, ARROW_WIDTH, height)
        MapViewerTextViews.box(
            parent = arrow,
            text = if (expanded) "^" else "v",
            width = ARROW_WIDTH,
            height = height,
            color = MapViewerUiStateStyle.text(expanded),
        )
        header.mouse { onClick { onToggle() } }
        return header
    }

    private const val ARROW_WIDTH = 36.0
    private const val LABEL_GAP = 10.0
}
