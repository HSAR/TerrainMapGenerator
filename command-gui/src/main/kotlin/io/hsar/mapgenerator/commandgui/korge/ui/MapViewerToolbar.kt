package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingMode
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.onStageResized
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.awt.Color

internal class MapViewerToolbar(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val selectedTool: () -> MapViewerTool,
    private val selectedDrawingMode: () -> MapDrawingMode,
    private val onToolSelected: (MapViewerTool) -> Unit,
    private val onDrawingModeSelected: (MapDrawingMode) -> Unit,
) {
    private val overlay = root.container()
    private val content = overlay.container()
    private var currentViewportWidth = viewportWidth
    private var currentViewportHeight = viewportHeight
    private var bounds = MapViewerToolbarLayoutBuilder.build(viewportWidth, viewportHeight)

    init {
        overlay.zIndex = Z_INDEX
        overlay.onStageResized { width, height ->
            relayout(width, height)
        }
        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        bounds.contains(point)

    fun refresh() {
        render()
    }

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        currentViewportWidth = viewportWidth
        currentViewportHeight = viewportHeight
        bounds = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            drawingModesVisible = selectedTool() == MapViewerTool.DRAW,
        )
        render()
    }

    private fun render() {
        content.removeChildren()
        content.xy(bounds.left, bounds.top)
        addButton(MapViewerTool.SELECT, "Select", 0.0)
        addButton(MapViewerTool.MEASURE, "Measure", BUTTON_WIDTH + BUTTON_GAP)
        addButton(MapViewerTool.DRAW, "Draw", (BUTTON_WIDTH + BUTTON_GAP) * 2.0)
        if (selectedTool() == MapViewerTool.DRAW) {
            addDrawingModeButton(MapDrawingMode.FREEFORM, "Freeform", 0.0)
            addDrawingModeButton(MapDrawingMode.RECTANGLE, "Rectangle", DRAW_MODE_WIDTH + BUTTON_GAP)
            addDrawingModeButton(MapDrawingMode.CIRCLE, "Circle", (DRAW_MODE_WIDTH + BUTTON_GAP) * 2.0)
        }
    }

    private fun addButton(tool: MapViewerTool, label: String, left: Double) {
        val active = selectedTool() == tool
        val button = content.container()
        button.xy(left, 0.0)
        button.solidRect(BUTTON_WIDTH, BUTTON_HEIGHT, RGBA(255, 255, 255, 255))
        button.solidRect(BUTTON_WIDTH - 4.0, BUTTON_HEIGHT - 4.0, toRgba(if (active) Palette.Colours.MAIN else Palette.Colours.DARK, 220)) {
            x = 2.0
            y = 2.0
        }
        button.image(MapViewerTextBitmapRenderer.render(label, Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = (BUTTON_WIDTH - width) * 0.5
            y = (BUTTON_HEIGHT - height) * 0.5
        }
        button.mouse {
            onClick {
                onToolSelected(tool)
                relayout(currentViewportWidth, currentViewportHeight)
            }
        }
    }

    private fun addDrawingModeButton(mode: MapDrawingMode, label: String, left: Double) {
        val active = selectedDrawingMode() == mode
        val button = content.container()
        button.xy(left, BUTTON_HEIGHT + DRAW_MODE_TOP_GAP)
        button.solidRect(DRAW_MODE_WIDTH, DRAW_MODE_HEIGHT, RGBA(255, 255, 255, 255))
        button.solidRect(DRAW_MODE_WIDTH - 4.0, DRAW_MODE_HEIGHT - 4.0, toRgba(if (active) Palette.Colours.MAIN else Palette.Colours.DARK, 220)) {
            x = 2.0
            y = 2.0
        }
        button.image(MapViewerTextBitmapRenderer.render(label, Palette.Fonts.MAIN.first, Palette.Colours.WHITE)) {
            x = (DRAW_MODE_WIDTH - width) * 0.5
            y = (DRAW_MODE_HEIGHT - height) * 0.5
        }
        button.mouse {
            onClick {
                onDrawingModeSelected(mode)
                refresh()
            }
        }
    }

    private companion object {
        const val Z_INDEX = 30_000.0
        const val BUTTON_WIDTH = 120.0
        const val BUTTON_HEIGHT = 40.0
        const val BUTTON_GAP = 12.0
        const val DRAW_MODE_WIDTH = 120.0
        const val DRAW_MODE_HEIGHT = 34.0
        const val DRAW_MODE_TOP_GAP = 8.0

        fun toRgba(color: Color, alpha: Int = color.alpha) =
            RGBA(color.red, color.green, color.blue, alpha)
    }
}

internal object MapViewerToolbarLayoutBuilder {
    private const val TOP_PADDING = 16.0
    private const val TOTAL_WIDTH = (120.0 * 3) + (12.0 * 2)
    private const val TOP_LEVEL_HEIGHT = 40.0
    private const val DRAW_MODE_HEIGHT = 34.0
    private const val DRAW_MODE_TOP_GAP = 8.0

    fun build(
        viewportWidth: Int,
        viewportHeight: Int,
        drawingModesVisible: Boolean = false,
    ) = ScreenBounds(
        left = (viewportWidth - TOTAL_WIDTH) * 0.5,
        top = TOP_PADDING,
        width = TOTAL_WIDTH,
        height = if (drawingModesVisible) {
            TOP_LEVEL_HEIGHT + DRAW_MODE_TOP_GAP + DRAW_MODE_HEIGHT
        } else {
            TOP_LEVEL_HEIGHT
        },
    )
}
