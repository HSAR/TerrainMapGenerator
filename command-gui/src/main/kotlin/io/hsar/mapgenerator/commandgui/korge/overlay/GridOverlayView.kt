package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.grid.GridLayer
import korlibs.image.awt.toBMP32
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage

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
        labelContainer.removeChildren()
        GridOverlayLayoutBuilder.build(
            state.copy(gridLayer = gridLayer),
        ).labels.forEach { label ->
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
}
