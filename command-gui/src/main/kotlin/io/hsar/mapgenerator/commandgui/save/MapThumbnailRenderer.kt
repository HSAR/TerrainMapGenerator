package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.CircleDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.FreeformDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.overlay.GridLabelStyle
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayLayoutBuilder
import io.hsar.mapgenerator.commandgui.korge.overlay.GridOverlayState
import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class MapThumbnailRenderer {
    fun renderOperationThumbnail(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
        gridLayer: GridLayer?,
        snapshots: List<MapPlacedObjectSnapshot>,
        outputWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
    ): BufferedImage =
        render(
            mapImage = mapImage,
            geometry = geometry,
            viewport = viewport.thumbnailViewport(outputWidth),
            gridLayer = gridLayer,
            snapshots = snapshots,
        )

    fun renderTurnThumbnail(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        gridLayer: GridLayer?,
        snapshots: List<MapPlacedObjectSnapshot>,
        outputWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
    ): BufferedImage {
        val scale = outputWidth.toDouble() / mapImage.width
        val outputHeight = (outputWidth / TURN_THUMBNAIL_ASPECT_RATIO).roundToInt().coerceAtLeast(1)
        val visibleHeight = outputHeight / scale
        return render(
            mapImage = mapImage,
            geometry = geometry,
            viewport = CameraViewport(
                centerX = geometry.mapOffsetX + mapImage.width / 2.0,
                centerY = geometry.mapOffsetY + mapImage.height / 2.0,
                zoom = scale,
                viewportWidth = outputWidth,
                viewportHeight = outputHeight,
                worldWidth = geometry.backgroundWidth,
                worldHeight = visibleHeight.roundToInt().coerceAtLeast(outputHeight),
            ),
            gridLayer = gridLayer,
            snapshots = snapshots,
        )
    }

    private fun render(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
        gridLayer: GridLayer?,
        snapshots: List<MapPlacedObjectSnapshot>,
    ): BufferedImage =
        BufferedImage(viewport.viewportWidth, viewport.viewportHeight, BufferedImage.TYPE_INT_RGB).also { output ->
            output.createGraphics().useHighQuality {
                color = Color.BLACK
                fillRect(0, 0, output.width, output.height)
                drawMapImage(mapImage, geometry, viewport)
                gridLayer?.let { drawGrid(it, mapImage, geometry, viewport) }
                snapshots.filterIsInstance<DrawingSnapshot>().forEach { drawDrawing(it, mapImage, geometry, viewport) }
                snapshots.filterIsInstance<UnitSnapshot>().forEach { drawUnit(it, mapImage, geometry, viewport) }
            }
        }

    private fun Graphics2D.drawMapImage(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        drawImage(
            mapImage,
            viewport.screenX(geometry.mapOffsetX.toDouble()).roundToInt(),
            viewport.screenY(geometry.mapOffsetY.toDouble()).roundToInt(),
            (mapImage.width * viewport.zoom).roundToInt(),
            (mapImage.height * viewport.zoom).roundToInt(),
            null,
        )
    }

    private fun Graphics2D.drawGrid(
        gridLayer: GridLayer,
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        val mapScreenX = viewport.screenX(geometry.mapOffsetX.toDouble())
        val mapScreenY = viewport.screenY(geometry.mapOffsetY.toDouble())
        val visibleLeft = (viewport.left - geometry.mapOffsetX).coerceIn(0.0, mapImage.width.toDouble())
        val visibleTop = (viewport.top - geometry.mapOffsetY).coerceIn(0.0, mapImage.height.toDouble())
        val visibleRight = (viewport.right - geometry.mapOffsetX).coerceIn(0.0, mapImage.width.toDouble())
        val visibleBottom = (viewport.bottom - geometry.mapOffsetY).coerceIn(0.0, mapImage.height.toDouble())
        val layout = GridOverlayLayoutBuilder.build(
            GridOverlayState(
                viewport = Rectangle(Point(visibleLeft, visibleTop), Point(visibleRight, visibleBottom)),
                mapWorldX = geometry.mapOffsetX.toDouble(),
                mapWorldY = geometry.mapOffsetY.toDouble(),
                mapOffsetX = mapScreenX,
                mapOffsetY = mapScreenY,
                scale = viewport.zoom,
                pixelWidth = mapImage.width,
                pixelHeight = mapImage.height,
                backgroundWidth = geometry.backgroundWidth,
                backgroundHeight = geometry.backgroundHeight,
                visibleCanvasLeft = 0.0,
                visibleCanvasTop = 0.0,
                visibleCanvasRight = viewport.viewportWidth.toDouble(),
                visibleCanvasBottom = viewport.viewportHeight.toDouble(),
                activeReference = null,
                measurement = null,
                gridLayer = gridLayer,
            ),
            measureText = ::measureGridText,
        )

        layout.lineRects.forEach { rect ->
            color = rect.color
            fill(
                Rectangle2D.Double(
                    mapScreenX + rect.x * viewport.zoom,
                    mapScreenY + rect.y * viewport.zoom,
                    rect.width * viewport.zoom,
                    rect.height * viewport.zoom,
                ),
            )
        }
        layout.labels.forEach { label ->
            label.shadows.forEach { shadow ->
                drawCenteredText(label.text, label.x + shadow.offsetX, label.y + shadow.offsetY, label.style.copy(color = shadow.color))
            }
            drawCenteredText(label.text, label.x, label.y, label.style)
        }
    }

    private fun Graphics2D.drawDrawing(
        snapshot: DrawingSnapshot,
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        color = snapshot.drawing.color
        stroke = BasicStroke(THUMBNAIL_DRAWING_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        when (val drawing = snapshot.drawing) {
            is FreeformDrawing -> drawing.points.zipWithNext().forEach { (start, end) ->
                drawLine(start, end, mapImage, geometry, viewport)
            }

            is LineDrawing -> drawLine(drawing.start, drawing.end, mapImage, geometry, viewport)
            is RectangleDrawing -> drawRectangle(drawing.start, drawing.end, mapImage, geometry, viewport)
            is CircleDrawing -> drawCircle(drawing.center, drawing.radius, mapImage, geometry, viewport)
        }
    }

    private fun Graphics2D.drawUnit(
        snapshot: UnitSnapshot,
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        val center = snapshot.mapCoordinate.toScreen(mapImage, geometry, viewport)
        val width = snapshot.normalizedWidth * mapImage.width * viewport.zoom
        val height = snapshot.normalizedHeight * mapImage.height * viewport.zoom
        val x = center.x - width / 2
        val y = center.y - height / 2
        snapshot.facingDegrees?.let { drawFacingIndicator(x, y, width, height, it) }
        drawImage(snapshot.template.bitmap.toBufferedImage(), x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt(), null)
        if (snapshot.destroyed) {
            drawDestroyedOverlay(x, y, width, height)
        }
    }

    private fun Graphics2D.drawLine(
        start: MapDrawingPoint,
        end: MapDrawingPoint,
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        val startScreen = start.toScreen(mapImage, geometry, viewport)
        val endScreen = end.toScreen(mapImage, geometry, viewport)
        draw(Line2D.Double(startScreen.x, startScreen.y, endScreen.x, endScreen.y))
    }

    private fun Graphics2D.drawRectangle(
        start: MapDrawingPoint,
        end: MapDrawingPoint,
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        val startScreen = start.toScreen(mapImage, geometry, viewport)
        val endScreen = end.toScreen(mapImage, geometry, viewport)
        draw(
            Rectangle2D.Double(
                minOf(startScreen.x, endScreen.x),
                minOf(startScreen.y, endScreen.y),
                kotlin.math.abs(endScreen.x - startScreen.x),
                kotlin.math.abs(endScreen.y - startScreen.y),
            ),
        )
    }

    private fun Graphics2D.drawCircle(
        center: MapDrawingPoint,
        radius: Double,
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) {
        val centerScreen = center.toScreen(mapImage, geometry, viewport)
        val screenRadius = radius * mapImage.height * viewport.zoom
        draw(Ellipse2D.Double(centerScreen.x - screenRadius, centerScreen.y - screenRadius, screenRadius * 2, screenRadius * 2))
    }

    private fun Graphics2D.drawFacingIndicator(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        facingDegrees: Int,
    ) {
        val centerX = x + width / 2
        val centerY = y + height / 2
        val radius = minOf(width, height) * 0.72
        color = Color(Palette.Colours.MAIN.red, Palette.Colours.MAIN.green, Palette.Colours.MAIN.blue, FACING_ALPHA)
        stroke = BasicStroke((minOf(width, height) * 0.08).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        draw(Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2))

        val radians = Math.toRadians(facingDegrees.toDouble())
        val directionX = sin(radians)
        val directionY = -cos(radians)
        val perpendicularX = cos(radians)
        val perpendicularY = sin(radians)
        val tipX = centerX + directionX * radius * 1.22
        val tipY = centerY + directionY * radius * 1.22
        val baseX = centerX + directionX * radius * 0.72
        val baseY = centerY + directionY * radius * 0.72
        val halfWidth = minOf(width, height) * 0.16
        fill(
            Path2D.Double().apply {
                moveTo(tipX, tipY)
                lineTo(baseX + perpendicularX * halfWidth, baseY + perpendicularY * halfWidth)
                lineTo(baseX - perpendicularX * halfWidth, baseY - perpendicularY * halfWidth)
                closePath()
            },
        )
    }

    private fun Graphics2D.drawDestroyedOverlay(x: Double, y: Double, width: Double, height: Double) {
        color = Color(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, DESTROYED_ALPHA)
        stroke = BasicStroke((minOf(width, height) * 0.18).toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
        draw(Line2D.Double(x, y, x + width, y + height))
        draw(Line2D.Double(x, y + height, x + width, y))
    }

    private fun Graphics2D.drawCenteredText(text: String, centerX: Double, centerY: Double, style: GridLabelStyle) {
        font = Font(Font.SANS_SERIF, if (style.role.name == "STRONG") Font.BOLD else Font.PLAIN, style.size.roundToInt().coerceAtLeast(1))
        color = style.color
        val metrics = fontMetrics
        drawString(
            text,
            (centerX - metrics.stringWidth(text) / 2.0).toFloat(),
            (centerY + (metrics.ascent - metrics.descent) / 2.0).toFloat(),
        )
    }

    private fun measureGridText(text: String, style: GridLabelStyle): Rectangle {
        val font = Font(Font.SANS_SERIF, if (style.role.name == "STRONG") Font.BOLD else Font.PLAIN, style.size.roundToInt().coerceAtLeast(1))
        val context = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics()
        return try {
            context.font = font
            val metrics = context.fontMetrics
            Rectangle(Point.ORIGIN, Point(metrics.stringWidth(text).toDouble(), metrics.height.toDouble()))
        } finally {
            context.dispose()
        }
    }

    private fun CameraViewport.thumbnailViewport(outputWidth: Int): CameraViewport {
        val outputHeight = (outputWidth * viewportHeight.toDouble() / viewportWidth).roundToInt().coerceAtLeast(1)
        val scale = outputWidth / visibleWidth
        return copy(
            zoom = scale,
            viewportWidth = outputWidth,
            viewportHeight = outputHeight,
        )
    }

    private fun MapDrawingPoint.toScreen(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) = MapCoordinate(x, y).toScreen(mapImage, geometry, viewport)

    private fun MapCoordinate.toScreen(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: CameraViewport,
    ) = Point(
        viewport.screenX(geometry.mapOffsetX + x * mapImage.width),
        viewport.screenY(geometry.mapOffsetY + y * mapImage.height),
    )

    private fun Bitmap32.toBufferedImage(): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val rgba = getRgba(x, y)
                    image.setRGB(x, y, (rgba.a shl 24) or (rgba.r shl 16) or (rgba.g shl 8) or rgba.b)
                }
            }
        }

    private fun Graphics2D.useHighQuality(block: Graphics2D.() -> Unit) {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        try {
            block()
        } finally {
            dispose()
        }
    }

    private companion object {
        const val DEFAULT_THUMBNAIL_WIDTH = 512
        const val TURN_THUMBNAIL_ASPECT_RATIO = 16.0 / 9.0
        const val THUMBNAIL_DRAWING_STROKE = 2.0f
        const val FACING_ALPHA = 160
        const val DESTROYED_ALPHA = 160
    }
}
