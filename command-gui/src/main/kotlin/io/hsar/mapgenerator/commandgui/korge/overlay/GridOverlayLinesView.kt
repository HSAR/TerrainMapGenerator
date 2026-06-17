package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.grid.GridLayer
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect

internal class GridOverlayLinesView(
    private val gridLayer: GridLayer,
) : Container() {
    private val lineContainer = container()

    init {
        addChild(lineContainer)
    }

    fun update(state: GridOverlayState) {
        val layout = GridOverlayLayoutBuilder.build(
            state.copy(gridLayer = gridLayer),
        )

        lineContainer.removeChildren()
        layout.lineRects.forEach { line ->
            lineContainer.solidRect(
                width = line.width,
                height = line.height,
                color = RGBA(line.color.red, line.color.green, line.color.blue, line.color.alpha),
            ) {
                x = line.x
                y = line.y
            }
        }
    }
}
