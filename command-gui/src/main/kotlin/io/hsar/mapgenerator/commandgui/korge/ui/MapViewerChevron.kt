package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.graphics

internal enum class MapViewerChevronDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

internal object MapViewerChevron {
    fun render(
        parent: Container,
        width: Double,
        height: Double,
        direction: MapViewerChevronDirection,
        color: RGBA = MapViewerUiBoxes.toRgba(Palette.Colours.WHITE),
    ) {
        val centerX = width * 0.5
        val centerY = height * 0.5
        val arm = MapViewerBoxedIcon.iconArm(width, height)
        parent.graphics {
            lineWidth = MapViewerBoxedIcon.STROKE_WIDTH
            stroke(color) {
                when (direction) {
                    MapViewerChevronDirection.UP -> {
                        moveTo(centerX - arm, centerY + arm)
                        lineTo(centerX, centerY - arm)
                        lineTo(centerX + arm, centerY + arm)
                    }

                    MapViewerChevronDirection.DOWN -> {
                        moveTo(centerX - arm, centerY - arm)
                        lineTo(centerX, centerY + arm)
                        lineTo(centerX + arm, centerY - arm)
                    }

                    MapViewerChevronDirection.LEFT -> {
                        moveTo(centerX + arm, centerY - arm)
                        lineTo(centerX - arm, centerY)
                        lineTo(centerX + arm, centerY + arm)
                    }

                    MapViewerChevronDirection.RIGHT -> {
                        moveTo(centerX - arm, centerY - arm)
                        lineTo(centerX + arm, centerY)
                        lineTo(centerX - arm, centerY + arm)
                    }
                }
            }
        }
    }
}
