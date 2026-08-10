package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Renders the selected hostile OCT marker as a high-resolution, transparent app-icon source. */
object MapViewerAppIconExporter {
    const val DEFAULT_SIZE = 512

    fun export(target: Path, size: Int = DEFAULT_SIZE) {
        require(size.isPowerOfTwo()) { "App icon size must be a positive power of two." }
        target.parent?.toFile()?.mkdirs()
        ImageIO.write(renderImage(size), "png", target.toFile())
    }

    fun render(size: Int = DEFAULT_SIZE): Bitmap32 {
        require(size.isPowerOfTwo()) { "App icon size must be a positive power of two." }
        val image = renderImage(size)

        return Bitmap32(size, size) { x, y ->
            image.getRGB(x, y).let { argb -> RGBA(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF, argb ushr 24) }
        }.premultipliedIfRequired()
    }

    private fun renderImage(size: Int): BufferedImage {
        MapViewerKorgeFonts.preloadAll()
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

        image.createGraphics().use { graphics ->
            graphics.configure()
            drawDiamond(graphics, size)
            drawLabel(graphics, size)
            drawSelectionBrackets(graphics, size)
        }

        return image
    }

    private fun Graphics2D.configure() {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }

    private fun drawDiamond(graphics: Graphics2D, size: Int) {
        val centre = size / 2.0
        val radius = size * DIAMOND_RADIUS_RATIO
        val diamond = Path2D.Double().apply {
            moveTo(centre, centre - radius)
            lineTo(centre + radius, centre)
            lineTo(centre, centre + radius)
            lineTo(centre - radius, centre)
            closePath()
        }
        graphics.color = Palette.Colours.RED
        graphics.fill(diamond)
        graphics.color = Palette.Colours.WHITE
        graphics.stroke = BasicStroke((size * DIAMOND_KEYLINE_RATIO).toFloat(), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER)
        graphics.draw(diamond)
    }

    private fun drawLabel(graphics: Graphics2D, size: Int) {
        val font = boldNordFont(size * LABEL_SIZE_RATIO)
        val bounds = font.createGlyphVector(graphics.fontRenderContext, LABEL).visualBounds
        graphics.font = font
        graphics.color = Palette.Colours.WHITE
        graphics.drawString(
            LABEL,
            (size / 2.0 - bounds.centerX).roundToInt(),
            (size / 2.0 - bounds.centerY).roundToInt(),
        )
    }

    private fun drawSelectionBrackets(graphics: Graphics2D, size: Int) {
        val inset = size * BRACKET_INSET_RATIO
        val length = size * BRACKET_LENGTH_RATIO
        val thickness = size * BRACKET_THICKNESS_RATIO
        graphics.color = java.awt.Color(Palette.Colours.WHITE.red, Palette.Colours.WHITE.green, Palette.Colours.WHITE.blue, BRACKET_ALPHA)

        listOf(
            inset to inset,
            size - inset to inset,
            size - inset to size - inset,
            inset to size - inset,
        ).forEachIndexed { index, (x, y) ->
            val horizontalDirection = if (index == 0 || index == 3) 1 else -1
            val verticalDirection = if (index == 0 || index == 1) 1 else -1
            val horizontal = Rectangle2D.Double(
                (if (horizontalDirection > 0) x else x - length).roundToInt().toDouble(),
                (if (verticalDirection > 0) y else y - thickness).roundToInt().toDouble(),
                length.roundToInt().toDouble(),
                thickness.roundToInt().toDouble(),
            )
            val vertical = Rectangle2D.Double(
                (if (horizontalDirection > 0) x else x - thickness).roundToInt().toDouble(),
                (if (verticalDirection > 0) y else y - length).roundToInt().toDouble(),
                thickness.roundToInt().toDouble(),
                length.roundToInt().toDouble(),
            )
            val bracket = Area(horizontal).apply { add(Area(vertical)) }
            val outlineWidth = (thickness * BRACKET_OUTLINE_WIDTH_RATIO).roundToInt().coerceAtLeast(1).toFloat()
            val outline = Area(BasicStroke(outlineWidth * 2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER).createStrokedShape(bracket))
                .apply { subtract(Area(bracket)) }
            graphics.color = Palette.Colours.DARK
            graphics.fill(outline)
            graphics.color = java.awt.Color(Palette.Colours.WHITE.red, Palette.Colours.WHITE.green, Palette.Colours.WHITE.blue, BRACKET_ALPHA)
            graphics.fill(bracket)
        }
    }

    private fun boldNordFont(size: Double): Font =
        MapViewerAppIconExporter::class.java.classLoader
            .getResourceAsStream("TV NordEF Bold Con.ttf")
            .let { stream ->
                requireNotNull(stream) { "TV NordEF Bold Condensed font resource is missing." }
                    .use { Font.createFont(Font.TRUETYPE_FONT, it) }
            }
            .deriveFont(size.toFloat())

    private inline fun Graphics2D.use(block: (Graphics2D) -> Unit) {
        try {
            block(this)
        } finally {
            dispose()
        }
    }

    private fun Int.isPowerOfTwo(): Boolean =
        this > 0 && this and (this - 1) == 0

    private const val LABEL = "OCT"
    private const val DIAMOND_RADIUS_RATIO = 0.29
    private const val DIAMOND_KEYLINE_RATIO = 0.026
    private const val LABEL_SIZE_RATIO = 0.22
    private const val BRACKET_INSET_RATIO = 0.13
    private const val BRACKET_LENGTH_RATIO = 0.16
    private const val BRACKET_THICKNESS_RATIO = 0.035
    private const val BRACKET_OUTLINE_WIDTH_RATIO = 0.25
    private const val BRACKET_ALPHA = 230
}
