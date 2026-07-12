package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy

internal object MapViewerCollapsiblePanel {
    fun chevronButton(
        parent: Container,
        width: Double,
        height: Double,
        direction: MapViewerChevronDirection,
        color: korlibs.image.color.RGBA = MapViewerUiBoxes.toRgba(Palette.Colours.WHITE),
    ): Container {
        val button = MapViewerBoxedIcon.frame(parent, width, height)
        MapViewerChevron.render(button, width, height, direction, color)
        return button
    }

    fun header(
        parent: Container,
        width: Double,
        height: Double,
        label: String,
        expanded: Boolean,
        onToggle: () -> Unit,
    ): Container {
        val header = parent.container()
        val hitArea = MapViewerUiBoxes.box(header, width, height)
        MapViewerTextViews.leftBox(
            parent = header,
            text = label,
            width = width - CHEVRON_WIDTH - LABEL_GAP,
            height = height,
            color = MapViewerUiStateStyle.text(expanded),
        ).xy(LABEL_GAP, 0.0)
        val arrow = header.container().also { it.xy(width - CHEVRON_WIDTH, 0.0) }
        chevronButton(
            parent = arrow,
            width = CHEVRON_WIDTH,
            height = height,
            direction = if (expanded) MapViewerChevronDirection.UP else MapViewerChevronDirection.DOWN,
            color = MapViewerUiBoxes.toRgba(MapViewerUiStateStyle.text(expanded)),
        )
        hitArea.mouse { onClick { onToggle() } }
        return header
    }

    const val CHEVRON_WIDTH = 36.0
    private const val LABEL_GAP = 10.0
}
