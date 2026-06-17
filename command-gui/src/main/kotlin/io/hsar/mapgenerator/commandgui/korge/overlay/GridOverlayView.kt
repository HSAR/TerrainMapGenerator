package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.image.awt.toBMP32
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.image
import korlibs.korge.view.xy
import korlibs.math.geom.radians
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(KorgeExperimental::class)
internal class GridOverlayView(
    private val gridLayer: GridLayer,
) : Container() {
    private val labelContainer = container()
    private val labelCache = mutableMapOf<LabelCacheKey, korlibs.image.bitmap.Bitmap32>()

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
                val shadowRendered = renderLabel(label.text, label.font, shadow.color)
                labelContainer.addChild(
                    shadowRendered.apply {
                        x = label.x + shadow.offsetX
                        y = label.y + shadow.offsetY
                    }
                )
            }
            val rendered = renderLabel(label.text, label.font, label.color)
            labelContainer.addChild(
                rendered.apply {
                    x = label.x
                    y = label.y
                }
            )
        }
    }

    private fun renderLabel(text: String, font: Font, color: Color): korlibs.korge.view.Image =
        image(renderTextBitmap(text, font, color))

    private fun drawChevronArrow(arrow: GridArrow) {
        val color = RGBA(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, Palette.Colours.RED.alpha)
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

    private fun renderTextBitmap(text: String, font: Font, color: Color): korlibs.image.bitmap.Bitmap32 =
        labelCache.getOrPut(LabelCacheKey(text, font.name, font.style, font.size, color.rgb)) {
            val textBounds = AwtGridTextMeasurer.measure(text, font)
            val width = textBounds.width.toInt().coerceAtLeast(1)
            val height = textBounds.height.toInt().coerceAtLeast(1)
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                )
                graphics.composite = AlphaComposite.SrcOver
                graphics.font = font
                graphics.color = color
                graphics.drawString(text, 0f, graphics.fontMetrics.ascent.toFloat())
            } finally {
                graphics.dispose()
            }
            image.toBMP32().premultipliedIfRequired()
        }

    private fun Color.toRGBA() =
        RGBA(red, green, blue, alpha)

    private data class LabelCacheKey(
        val text: String,
        val fontName: String,
        val fontStyle: Int,
        val fontSize: Int,
        val rgb: Int,
    )

    private companion object {
        private const val ARROW_HALF_HEIGHT = 5.0
        private const val ARROW_HEAD_LENGTH = 10.0
        private const val ARROW_TAIL_LENGTH = 10.0
    }
}
