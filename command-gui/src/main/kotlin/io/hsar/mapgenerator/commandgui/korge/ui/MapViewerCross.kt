package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.graphics

internal object MapViewerCross {
    fun render(parent: Container, width: Double, height: Double) {
        val centerX = width * 0.5
        val centerY = height * 0.5
        val arm = MapViewerBoxedIcon.iconArm(width, height)
        parent.graphics {
            lineWidth = MapViewerBoxedIcon.STROKE_WIDTH
            stroke(MapViewerUiBoxes.toRgba(Palette.Colours.WHITE)) {
                moveTo(centerX - arm, centerY - arm)
                lineTo(centerX + arm, centerY + arm)
                moveTo(centerX + arm, centerY - arm)
                lineTo(centerX - arm, centerY + arm)
            }
        }
    }
}
