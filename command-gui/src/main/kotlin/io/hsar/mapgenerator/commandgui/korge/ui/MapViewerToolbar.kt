package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingColour
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingMode
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
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
    private val selectedDrawingColour: () -> MapDrawingColour,
    private val onToolSelected: (MapViewerTool) -> Unit,
    private val onDrawingModeSelected: (MapDrawingMode) -> Unit,
    private val onDrawingColourSelected: (MapDrawingColour) -> Unit,
) {
    private val toolButtons = listOf(
        ToolbarButtonSpec(MapViewerTool.SELECT, "Select"),
        ToolbarButtonSpec(MapViewerTool.MEASURE, "Measure"),
        ToolbarButtonSpec(MapViewerTool.DRAW, "Draw"),
    )
    private val drawingModeButtons = MapDrawingMode.entries
    private val drawingColourButtons = MapDrawingColour.entries
    private val overlay = root.container()
    private val content = overlay.container()
    private var currentViewportWidth = viewportWidth
    private var currentViewportHeight = viewportHeight
    private var layout = MapViewerToolbarLayoutBuilder.build(viewportWidth, viewportHeight)

    init {
        overlay.zIndex = Z_INDEX
        overlay.onStageResized { width, height ->
            relayout(width, height)
        }
        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        layout.bounds.contains(point)

    fun refresh() {
        render()
    }

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        currentViewportWidth = viewportWidth
        currentViewportHeight = viewportHeight
        layout = MapViewerToolbarLayoutBuilder.build(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            drawingModesVisible = selectedTool() == MapViewerTool.DRAW,
            toolCount = toolButtons.size,
            drawingModeCount = drawingModeButtons.size,
            drawingColourCount = drawingColourButtons.size,
        )
        render()
    }

    private fun render() {
        content.removeChildren()
        content.xy(0.0, layout.bounds.top)
        toolButtons.forEachIndexed { index, button ->
            addButton(button.tool, button.label, layout.toolRowLeft + index * (BUTTON_WIDTH + BUTTON_GAP))
        }
        if (selectedTool() == MapViewerTool.DRAW) {
            drawingModeButtons.forEachIndexed { index, button ->
                addDrawingModeButton(button, layout.drawingModeRowLeft + index * (DRAW_MODE_WIDTH + BUTTON_GAP))
            }
            drawingColourButtons.forEachIndexed { index, colour ->
                addDrawingColourButton(colour, layout.drawingColourRowLeft + index * (COLOUR_SWATCH_SIZE + COLOUR_SWATCH_GAP))
            }
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
        MapViewerTextViews.box(button, label, BUTTON_WIDTH, BUTTON_HEIGHT, color = Palette.Colours.WHITE)
        button.mouse {
            onClick {
                onToolSelected(tool)
                relayout(currentViewportWidth, currentViewportHeight)
            }
        }
    }

    private fun addDrawingModeButton(mode: MapDrawingMode, left: Double) {
        val active = selectedDrawingMode() == mode
        val button = content.container()
        button.xy(left, BUTTON_HEIGHT + DRAW_MODE_TOP_GAP)
        button.solidRect(DRAW_MODE_WIDTH, DRAW_MODE_HEIGHT, RGBA(255, 255, 255, 255))
        button.solidRect(DRAW_MODE_WIDTH - 4.0, DRAW_MODE_HEIGHT - 4.0, toRgba(if (active) Palette.Colours.MAIN else Palette.Colours.DARK, 220)) {
            x = 2.0
            y = 2.0
        }
        MapViewerTextViews.box(button, mode.label, DRAW_MODE_WIDTH, DRAW_MODE_HEIGHT, color = Palette.Colours.WHITE)
        button.mouse {
            onClick {
                onDrawingModeSelected(mode)
                refresh()
            }
        }
    }

    private fun addDrawingColourButton(colour: MapDrawingColour, left: Double) {
        val active = selectedDrawingColour() == colour
        val button = content.container()
        button.xy(left, BUTTON_HEIGHT + DRAW_MODE_TOP_GAP + DRAW_MODE_HEIGHT + COLOUR_ROW_TOP_GAP)
        button.solidRect(COLOUR_SWATCH_SIZE, COLOUR_SWATCH_SIZE, RGBA(255, 255, 255, 255))
        button.solidRect(COLOUR_SWATCH_SIZE - 4.0, COLOUR_SWATCH_SIZE - 4.0, toRgba(colour.color)) {
            x = 2.0
            y = 2.0
        }
        if (active) {
            button.solidRect(COLOUR_SWATCH_SIZE - 4.0, COLOUR_SWATCH_SIZE - 4.0, toRgba(Palette.Colours.DARK, 120)) {
                x = 2.0
                y = 2.0
            }
        }
        button.mouse {
            onClick {
                onDrawingColourSelected(colour)
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
        const val COLOUR_SWATCH_SIZE = 28.0
        const val COLOUR_SWATCH_GAP = 10.0
        const val COLOUR_ROW_TOP_GAP = 8.0

        fun toRgba(color: Color, alpha: Int = color.alpha) =
            RGBA(color.red, color.green, color.blue, alpha)
    }

    private data class ToolbarButtonSpec(
        val tool: MapViewerTool,
        val label: String,
    )
}

internal data class MapViewerToolbarLayout(
    val bounds: ScreenBounds,
    val toolRowLeft: Double,
    val drawingModeRowLeft: Double,
    val drawingColourRowLeft: Double,
)

internal object MapViewerToolbarLayoutBuilder {
    private const val TOP_PADDING = 16.0
    private const val BUTTON_WIDTH = 120.0
    private const val BUTTON_GAP = 12.0
    private const val DRAW_MODE_WIDTH = 120.0
    private const val TOP_LEVEL_HEIGHT = 40.0
    private const val DRAW_MODE_HEIGHT = 34.0
    private const val DRAW_MODE_TOP_GAP = 8.0
    private const val COLOUR_SWATCH_SIZE = 28.0
    private const val COLOUR_SWATCH_GAP = 10.0
    private const val COLOUR_ROW_TOP_GAP = 8.0

    fun build(
        viewportWidth: Int,
        viewportHeight: Int,
        drawingModesVisible: Boolean = false,
        toolCount: Int = 3,
        drawingModeCount: Int = 4,
        drawingColourCount: Int = 6,
    ): MapViewerToolbarLayout {
        val toolRowWidth = sequenceWidth(toolCount, BUTTON_WIDTH)
        val drawingModeRowWidth = sequenceWidth(drawingModeCount, DRAW_MODE_WIDTH)
        val drawingColourRowWidth = colourSequenceWidth(drawingColourCount)
        val visibleDrawingWidth = if (drawingModesVisible) {
            maxOf(drawingModeRowWidth, drawingColourRowWidth)
        } else {
            0.0
        }
        val boundsWidth = maxOf(toolRowWidth, visibleDrawingWidth)
        val boundsLeft = (viewportWidth - boundsWidth) * 0.5
        val toolRowLeft = (viewportWidth - toolRowWidth) * 0.5
        val drawingModeRowLeft = (viewportWidth - drawingModeRowWidth) * 0.5
        val drawingColourRowLeft = (viewportWidth - drawingColourRowWidth) * 0.5

        return MapViewerToolbarLayout(
            bounds = ScreenBounds(
                left = boundsLeft,
                top = TOP_PADDING,
                width = boundsWidth,
                height = if (drawingModesVisible) {
                    TOP_LEVEL_HEIGHT + DRAW_MODE_TOP_GAP + DRAW_MODE_HEIGHT + COLOUR_ROW_TOP_GAP + COLOUR_SWATCH_SIZE
                } else {
                    TOP_LEVEL_HEIGHT
                },
            ),
            toolRowLeft = toolRowLeft,
            drawingModeRowLeft = drawingModeRowLeft,
            drawingColourRowLeft = drawingColourRowLeft,
        )
    }

    private fun sequenceWidth(count: Int, itemWidth: Double): Double =
        if (count <= 0) {
            0.0
        } else {
            (count * itemWidth) + ((count - 1) * BUTTON_GAP)
        }

    private fun colourSequenceWidth(count: Int): Double =
        if (count <= 0) {
            0.0
        } else {
            (count * COLOUR_SWATCH_SIZE) + ((count - 1) * COLOUR_SWATCH_GAP)
        }
}
