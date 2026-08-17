package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.MapViewerDialogChromeTokens
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.solidRect

internal object MapViewerCornerBrackets {
    fun renderAll(parent: Container, width: Double, height: Double, inset: Double = 0.0) {
        listOf(
            Corner.TOP_LEFT,
            Corner.TOP_RIGHT,
            Corner.BOTTOM_RIGHT,
            Corner.BOTTOM_LEFT,
        ).forEach { corner -> render(parent, width, height, corner, inset) }
    }

    fun render(parent: Container, width: Double, height: Double, corner: Corner, inset: Double = 0.0) {
        val color = MapViewerUiBoxes.toRgba(Palette.Colours.WHITE, BRACKET_ALPHA)
        val left = when (corner) {
            Corner.TOP_LEFT,
            Corner.BOTTOM_LEFT -> inset

            Corner.TOP_RIGHT,
            Corner.BOTTOM_RIGHT -> width - inset - MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH
        }
        val top = when (corner) {
            Corner.TOP_LEFT,
            Corner.TOP_RIGHT -> inset

            Corner.BOTTOM_RIGHT,
            Corner.BOTTOM_LEFT -> height - inset - MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS
        }
        parent.solidRect(MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH, MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS, color) {
            x = left
            y = top
        }

        val verticalLeft = when (corner) {
            Corner.TOP_LEFT,
            Corner.BOTTOM_LEFT -> inset

            Corner.TOP_RIGHT,
            Corner.BOTTOM_RIGHT -> width - inset - MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS
        }
        val verticalTop = when (corner) {
            Corner.TOP_LEFT,
            Corner.TOP_RIGHT -> inset

            Corner.BOTTOM_RIGHT,
            Corner.BOTTOM_LEFT -> height - inset - MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH
        }
        parent.solidRect(MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS, MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH, color) {
            x = verticalLeft
            y = verticalTop
        }
    }

    enum class Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
    }

    private const val BRACKET_ALPHA = 230
}
