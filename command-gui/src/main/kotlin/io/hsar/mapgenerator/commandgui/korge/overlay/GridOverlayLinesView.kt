package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.grid.GridLayer
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import org.apache.logging.log4j.LogManager

internal class GridOverlayLinesView(
    private val gridLayer: GridLayer,
) : Container() {
    private val lineContainer = container()
    private val logger = LogManager.getLogger(GridOverlayLinesView::class.java)
    private var lastLayout: GridOverlayLayout? = null

    init {
        addChild(lineContainer)
    }

    fun update(state: GridOverlayState) {
        val layout = GridOverlayLayoutBuilder.build(
            state.copy(gridLayer = gridLayer),
        )

        update(layout)
    }

    fun update(render: GridOverlayRender) =
        update(render.layout)

    private fun update(layout: GridOverlayLayout) {
        if (layout == lastLayout) {
            return
        }

        val startedAt = System.nanoTime()
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
        lastLayout = layout
        logSlowUpdate(startedAt, layout.lineRects.size)
    }

    private fun logSlowUpdate(startedAt: Long, rectCount: Int) {
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        if (elapsedMs >= SLOW_UPDATE_MS) {
            logger.info("Grid line overlay update took {}ms for {} rects", elapsedMs, rectCount)
        }
    }

    private companion object {
        private const val SLOW_UPDATE_MS = 16
    }
}
