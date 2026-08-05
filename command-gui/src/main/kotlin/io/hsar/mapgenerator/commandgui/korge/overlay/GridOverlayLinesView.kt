package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.MapViewerGridLineSegment
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlay
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayLayoutBuilder
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
    private var lastLayout: MapViewerGridOverlay? = null
    private var lastProjection: Triple<Double, Double, Double>? = null

    init {
        addChild(lineContainer)
    }

    fun update(state: GridOverlayState) {
        currentState = state
        val layout = MapViewerGridOverlayLayoutBuilder.build(state.copy(gridLayer = gridLayer).toNeutralRequest())

        update(layout)
    }

    fun update(render: GridOverlayRender) {
        currentState = render.state
        update(render.layout)
    }

    private fun update(layout: MapViewerGridOverlay) {
        val state = currentState ?: return
        val projection = Triple(state.mapOffsetX, state.mapOffsetY, state.scale)
        if (layout == lastLayout && projection == lastProjection) {
            return
        }

        val startedAt = System.nanoTime()
        lineContainer.removeChildren()
        layout.lineSegments.forEach { line ->
            val rect = line.toKorgeGridRect(state)
            lineContainer.solidRect(
                width = rect.width,
                height = rect.height,
                color = RGBA(line.colour.red, line.colour.green, line.colour.blue, line.colour.alpha),
            ) {
                x = rect.x
                y = rect.y
            }
        }
        lastLayout = layout
        lastProjection = projection
        logSlowUpdate(startedAt, layout.lineSegments.size)
    }

    private var currentState: GridOverlayState? = null

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

internal data class KorgeGridRect(val x: Double, val y: Double, val width: Double, val height: Double)

internal fun MapViewerGridLineSegment.toKorgeGridRect(state: GridOverlayState): KorgeGridRect {
    val thickness = (thickness / state.scale).coerceAtLeast(0.25)
    val x1 = (start.x - state.mapOffsetX) / state.scale
    val y1 = (start.y - state.mapOffsetY) / state.scale
    val x2 = (end.x - state.mapOffsetX) / state.scale
    val y2 = (end.y - state.mapOffsetY) / state.scale
    return KorgeGridRect(
        x = if (x1 == x2) x1 - thickness / 2.0 else x1,
        y = if (y1 == y2) y1 - thickness / 2.0 else y1,
        width = if (x1 == x2) thickness else x2 - x1,
        height = if (y1 == y2) thickness else y2 - y1,
    )
}
