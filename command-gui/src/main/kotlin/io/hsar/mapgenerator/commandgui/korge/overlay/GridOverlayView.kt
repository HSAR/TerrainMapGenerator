package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.MapViewerGridFontRole
import io.hsar.mapgenerator.commandgui.MapViewerGridLabel
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlay
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayLayoutBuilder
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.grid.GridLayer
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.xy
import korlibs.math.geom.radians
import org.apache.logging.log4j.LogManager
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(KorgeExperimental::class)
internal class GridOverlayView(
    private val gridLayer: GridLayer,
) : Container() {
    private val labelContainer = container()
    private val logger = LogManager.getLogger(GridOverlayView::class.java)
    private var lastLayout: MapViewerGridOverlay? = null

    init {
        addChild(labelContainer)
    }

    fun update(state: GridOverlayState) {
        val request = state.copy(gridLayer = gridLayer).toNeutralRequest()
        val layout = MapViewerGridOverlayLayoutBuilder.build(request) { text, size, role ->
            val bounds = MapViewerTextViews.measure(text, role.toKorgeRole(), size)
            io.hsar.mapgenerator.commandgui.ViewerPoint(bounds.width, bounds.height)
        }

        update(layout)
    }

    fun update(render: GridOverlayRender) =
        update(render.layout)

    private fun update(layout: MapViewerGridOverlay) {
        if (layout == lastLayout) {
            return
        }

        val startedAt = System.nanoTime()
        labelContainer.removeChildren()
        layout.arrows.forEach { arrow ->
            drawChevronArrow(arrow)
        }

        layout.labels.forEach { label ->
            label.shadows.forEach { shadow ->
                renderLabel(label, shadow.colour) {
                    val bounds = MapViewerTextViews.measure(label.text, label.role.toKorgeRole(), label.fontSize)
                    x = label.centre.x - bounds.width / 2 + shadow.offset.x
                    y = label.centre.y - bounds.height / 2 + shadow.offset.y
                }
            }
            renderLabel(label, label.colour) {
                val bounds = MapViewerTextViews.measure(label.text, label.role.toKorgeRole(), label.fontSize)
                x = label.centre.x - bounds.width / 2
                y = label.centre.y - bounds.height / 2
            }
        }
        lastLayout = layout
        logSlowUpdate(startedAt, layout.labels.size, layout.arrows.size)
    }

    private fun logSlowUpdate(startedAt: Long, labelCount: Int, arrowCount: Int) {
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        if (elapsedMs >= SLOW_UPDATE_MS) {
            logger.info("Grid label overlay update took {}ms for {} labels and {} arrows", elapsedMs, labelCount, arrowCount)
        }
    }

    private fun renderLabel(label: MapViewerGridLabel, color: java.awt.Color, block: Text.() -> Unit) =
        MapViewerTextViews.label(
            parent = labelContainer,
            text = label.text,
            role = label.role.toKorgeRole(),
            size = label.fontSize,
            color = color,
            block = block,
        )

    private fun drawChevronArrow(arrow: io.hsar.mapgenerator.commandgui.MapViewerGridArrow) {
        val color = RGBA(arrow.colour.red, arrow.colour.green, arrow.colour.blue, arrow.colour.alpha)
        val dx = arrow.end.x - arrow.start.x
        val dy = arrow.end.y - arrow.start.y
        val length = hypot(dx, dy)
        if (length <= 0.0) {
            return
        }

        val angle = atan2(dy, dx)
        val arrowContainer = labelContainer.container()
        arrowContainer.xy(arrow.start.x, arrow.start.y)
        arrowContainer.rotation = angle.toFloat().radians
        arrowContainer.graphics {
            fill(color) {
                moveTo(0.0, -ARROW_HALF_HEIGHT)
                lineTo(length - ARROW_HEAD_LENGTH, -ARROW_HALF_HEIGHT)
                lineTo(length, 0.0)
                lineTo(length - ARROW_HEAD_LENGTH, ARROW_HALF_HEIGHT)
                lineTo(0.0, ARROW_HALF_HEIGHT)
                lineTo(ARROW_TAIL_LENGTH, 0.0)
                close()
            }
        }
    }

    private companion object {
        private const val SLOW_UPDATE_MS = 16
        private const val ARROW_HALF_HEIGHT = 5.0
        private const val ARROW_HEAD_LENGTH = 10.0
        private const val ARROW_TAIL_LENGTH = 10.0
    }

    private fun MapViewerGridFontRole.toKorgeRole() = when (this) {
        MapViewerGridFontRole.MAIN -> io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.MAIN
        MapViewerGridFontRole.STRONG -> io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole.STRONG
    }
}
