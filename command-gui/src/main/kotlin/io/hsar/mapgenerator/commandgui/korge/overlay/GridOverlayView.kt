package io.hsar.mapgenerator.commandgui.korge.overlay

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
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(KorgeExperimental::class)
internal class GridOverlayView(
    private val gridLayer: GridLayer,
) : Container() {
    private val labelContainer = container()

    init {
        addChild(labelContainer)
    }

    fun update(state: GridOverlayState) {
        val layout = GridOverlayLayoutBuilder.build(
            state.copy(gridLayer = gridLayer),
        )

        labelContainer.removeChildren()
        layout.overlayArrows.forEach { arrow ->
            drawChevronArrow(arrow)
        }

        layout.labels.forEach { label ->
            label.shadows.forEach { shadow ->
                renderLabel(label.text, label.style, shadow.color) {
                    val bounds = MapViewerTextViews.measure(label.text, label.style.role, label.style.size)
                    x = label.x - bounds.width / 2 + shadow.offsetX
                    y = label.y - bounds.height / 2 + shadow.offsetY
                }
            }
            renderLabel(label.text, label.style, label.style.color) {
                val bounds = MapViewerTextViews.measure(label.text, label.style.role, label.style.size)
                x = label.x - bounds.width / 2
                y = label.y - bounds.height / 2
            }
        }
    }

    private fun renderLabel(text: String, style: GridLabelStyle, color: java.awt.Color, block: Text.() -> Unit) =
        MapViewerTextViews.label(
            parent = labelContainer,
            text = text,
            role = style.role,
            size = style.size,
            color = color,
            block = block,
        )

    private fun drawChevronArrow(arrow: GridArrow) {
        val color = RGBA(arrow.color.red, arrow.color.green, arrow.color.blue, arrow.color.alpha)
        val dx = arrow.x2 - arrow.x1
        val dy = arrow.y2 - arrow.y1
        val length = hypot(dx, dy)
        if (length <= 0.0) {
            return
        }

        val angle = atan2(dy, dx)
        val arrowContainer = labelContainer.container()
        arrowContainer.xy(arrow.x1, arrow.y1)
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
        private const val ARROW_HALF_HEIGHT = 5.0
        private const val ARROW_HEAD_LENGTH = 10.0
        private const val ARROW_TAIL_LENGTH = 10.0
    }
}
