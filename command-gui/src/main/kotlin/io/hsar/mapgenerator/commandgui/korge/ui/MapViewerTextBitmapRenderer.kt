package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object MapViewerTextBitmapRenderer {
    private val labelCache = mutableMapOf<LabelCacheKey, Bitmap32>()

    fun render(text: String, font: Font, color: Color): Bitmap32 =
        labelCache.getOrPut(LabelCacheKey(text, font.name, font.style, font.size, color.rgb)) {
            val lines = text.split('\n').ifEmpty { listOf("") }
            val scratch = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            val graphics = scratch.createGraphics()
            try {
                graphics.font = font
                val metrics = graphics.fontMetrics
                val lineHeight = metrics.height.coerceAtLeast(1)
                val width = lines.maxOf { metrics.stringWidth(it) }.coerceAtLeast(1)
                val height = (lineHeight * lines.size).coerceAtLeast(1)
                val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val painter = image.createGraphics()
                try {
                    painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    painter.composite = AlphaComposite.SrcOver
                    painter.font = font
                    painter.color = color
                    val lineMetrics = painter.fontMetrics
                    lines.forEachIndexed { index, line ->
                        painter.drawString(line, 0f, (lineMetrics.ascent + index * lineMetrics.height).toFloat())
                    }
                } finally {
                    painter.dispose()
                }
                image.toBMP32().premultipliedIfRequired()
            } finally {
                graphics.dispose()
            }
        }

    private data class LabelCacheKey(
        val text: String,
        val fontName: String,
        val fontStyle: Int,
        val fontSize: Int,
        val rgb: Int,
    )
}
