package io.hsar.mapgenerator.contourgenerator

import io.hsar.mapgenerator.image.Palette
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

data class TopographyStyle(
    val bandColours: List<Color> = listOf(
        Color(10, 8, 8),
        Color(18, 13, 13),
        Color(25, 20, 20),
        Color(34, 29, 29),
        Color(43, 38, 38),
    ),
    val minorContourColour: Color = Color(Palette.Colours.LIGHT.red, Palette.Colours.LIGHT.green, Palette.Colours.LIGHT.blue, 150),
    val majorContourColour: Color = Color(Palette.Colours.BACKING.red, Palette.Colours.BACKING.green, Palette.Colours.BACKING.blue, 190),
    val minorContourWidth: Float = 1.0f,
    val majorContourWidth: Float = 1.5f,
    val majorContourInterval: Int = 5,
) {
    init {
        require(bandColours.isNotEmpty())
        require(minorContourWidth > 0.0f)
        require(majorContourWidth > 0.0f)
        require(majorContourInterval > 0)
    }
}

object TopographyRenderer {
    fun render(map: TopographyMap, style: TopographyStyle = TopographyStyle()): BufferedImage =
        BufferedImage(map.heightField.width, map.heightField.height, BufferedImage.TYPE_INT_ARGB).also { image ->
            renderBands(image, map.heightField, style.bandColours)
            image.createGraphics().also { graphics ->
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val levels = map.contours.map(ContourPath::level).distinct().sorted()
                map.contours.forEach { contour ->
                    val major = levels.indexOf(contour.level) % style.majorContourInterval == 0
                    graphics.color = if (major) style.majorContourColour else style.minorContourColour
                    graphics.stroke = BasicStroke(if (major) style.majorContourWidth else style.minorContourWidth)
                    graphics.draw(contour.toShape())
                }
                graphics.dispose()
            }
        }

    fun renderHeight(map: TopographyMap): BufferedImage =
        BufferedImage(map.heightField.width, map.heightField.height, BufferedImage.TYPE_INT_ARGB).also { image ->
            (0 until map.heightField.height).forEach { y ->
                (0 until map.heightField.width).forEach { x ->
                    val value = (map.heightField[x, y] * 255.0).roundToInt().coerceIn(0, 255)
                    image.setRGB(x, y, Color(value, value, value).rgb)
                }
            }
        }

    private fun renderBands(image: BufferedImage, field: HeightField, colours: List<Color>) {
        (0 until field.height).forEach { y ->
            (0 until field.width).forEach { x ->
                val index = (field[x, y] * colours.size).toInt().coerceAtMost(colours.lastIndex)
                image.setRGB(x, y, colours[index].rgb)
            }
        }
    }
}

private fun ContourPath.toShape(): Path2D.Double = Path2D.Double().also { shape ->
    points.firstOrNull()?.let { first ->
        shape.moveTo(first.x, first.y)
        points.drop(1).forEach { point -> shape.lineTo(point.x, point.y) }
        if (closed) shape.closePath()
    }
}
