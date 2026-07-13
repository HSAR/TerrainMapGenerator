package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

internal object OlympiadDawnStatusRows {
    const val POINT_ROW_HEIGHT = 30.0

    fun renderPoints(
        parent: Container,
        top: Double,
        width: Double,
        label: String,
        count: Int,
        destroyed: Set<Int>,
        editable: Boolean,
        trailingText: String? = null,
        onToggle: (Int) -> Unit,
    ) {
        MapViewerTextViews.leftBox(parent, label, LABEL_WIDTH, POINT_ROW_HEIGHT, color = Palette.Colours.WHITE) { y = top }
        val trailingWidth = trailingText?.let { MapViewerTextViews.measure(it).width + STATUS_GAP } ?: 0.0
        val pointWidth = ((width - LABEL_WIDTH - trailingWidth) / count.coerceAtLeast(1)).coerceAtMost(POINT_SIZE)
        repeat(count) { index ->
            val point = parent.container().also { it.xy(LABEL_WIDTH + index * pointWidth, top + POINT_INSET) }
            val pointButton = MapViewerUiBoxes.box(point, pointWidth - POINT_GAP, POINT_SIZE - POINT_INSET * 2.0, active = index !in destroyed)
            if (index in destroyed) point.solidRect(pointWidth - POINT_GAP, POINT_SIZE - POINT_INSET * 2.0, RGBA(120, 20, 20, 255))
            if (editable) pointButton.mouse { onClick { onToggle(index) } }
        }
        trailingText?.let { text ->
            MapViewerTextViews.box(parent, text, trailingWidth, POINT_ROW_HEIGHT, color = Palette.Colours.WHITE, alignment = TextAlignment.MIDDLE_RIGHT) {
                x = width - trailingWidth
                y = top
            }
        }
    }

    private const val LABEL_WIDTH = 82.0
    private const val POINT_SIZE = 28.0
    private const val POINT_GAP = 3.0
    private const val POINT_INSET = 2.0
    private const val STATUS_GAP = 12.0
}
