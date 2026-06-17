package io.hsar.mapgenerator.commandgui.korge.ui

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
    private val onToolSelected: (MapViewerTool) -> Unit,
) {
    private val overlay = root.container()
    private val content = overlay.container()
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
        bounds = MapViewerToolbarLayoutBuilder.build(viewportWidth, viewportHeight)
        render()
    }

    private fun render() {
        content.removeChildren()
        content.xy(bounds.left, bounds.top)
        addButton(MapViewerTool.SELECT, "Select", 0.0)
        addButton(MapViewerTool.MEASURE, "Measure", BUTTON_WIDTH + BUTTON_GAP)
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
                refresh()
            }
        }
    }

    private companion object {
        const val Z_INDEX = 30_000.0
        const val BUTTON_WIDTH = 120.0
        const val BUTTON_HEIGHT = 40.0
        const val BUTTON_GAP = 12.0

        fun toRgba(color: Color, alpha: Int = color.alpha) =
            RGBA(color.red, color.green, color.blue, alpha)
    }
}

internal object MapViewerToolbarLayoutBuilder {
    private const val TOP_PADDING = 16.0
    private const val TOTAL_WIDTH = (120.0 * 2) + 12.0
    private const val HEIGHT = 40.0

    fun build(viewportWidth: Int, viewportHeight: Int) = ScreenBounds(
        left = (viewportWidth - TOTAL_WIDTH) * 0.5,
        top = TOP_PADDING,
        width = TOTAL_WIDTH,
        height = HEIGHT,
    )
}
