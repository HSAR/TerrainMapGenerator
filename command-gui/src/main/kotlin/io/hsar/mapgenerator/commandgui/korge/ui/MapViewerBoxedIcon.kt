package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect

internal object MapViewerBoxedIcon {
    const val STROKE_WIDTH = 2.0

    fun frame(parent: Container, width: Double, height: Double, sharedRightEdge: Boolean = false): Container =
        parent.container().also { frame ->
            if (!sharedRightEdge) {
                MapViewerUiBoxes.button(frame, width, height)
            } else {
                frame.solidRect(width, height, MapViewerUiStateStyle.backing(active = true))
                frame.solidRect(width, STROKE_WIDTH, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE))
                frame.solidRect(width, STROKE_WIDTH, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE)) {
                    y = height - STROKE_WIDTH
                }
                frame.solidRect(STROKE_WIDTH, height, MapViewerUiBoxes.toRgba(Palette.Colours.WHITE))
            }
        }

    fun iconArm(width: Double, height: Double): Double =
        (minOf(width, height) * 0.22).coerceAtLeast(3.0)
}
