package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.*
import io.hsar.mapgenerator.commandgui.operation.*
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import kotlin.math.*

internal class MapThumbnailRenderer {
    fun renderOperationThumbnail(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        viewport: MapViewport,
        gridLayer: GridLayer?,
        content: OperationContent,
        outputWidth: Int = DEFAULT_WIDTH,
    ) = render(mapImage, geometry, viewport.thumbnailViewport(outputWidth), gridLayer, content)

    fun renderTurnThumbnail(
        mapImage: BufferedImage,
        geometry: ViewerGeometry,
        gridLayer: GridLayer?,
        content: OperationContent,
        outputWidth: Int = DEFAULT_WIDTH,
    ): BufferedImage {
        val scale = outputWidth.toDouble() / mapImage.width
        val outputHeight = (outputWidth / TURN_ASPECT).roundToInt().coerceAtLeast(1)
        return render(
            mapImage,
            geometry,
            MapViewport(
                center = ViewerPoint(geometry.mapOffsetX + mapImage.width / 2.0, geometry.mapOffsetY + mapImage.height / 2.0),
                zoom = scale,
                viewportWidth = outputWidth,
                viewportHeight = outputHeight,
                worldWidth = geometry.backgroundWidth.toDouble(),
                worldHeight = max(geometry.backgroundHeight.toDouble(), outputHeight / scale),
            ),
            gridLayer,
            content,
        )
    }

    private fun render(mapImage: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport, gridLayer: GridLayer?, content: OperationContent) =
        BufferedImage(viewport.viewportWidth, viewport.viewportHeight, BufferedImage.TYPE_INT_RGB).also { output ->
            output.createGraphics().highQuality {
                color = Color.BLACK
                fillRect(0, 0, output.width, output.height)
                drawImage(
                    mapImage,
                    viewport.screenX(geometry.mapOffsetX.toDouble()).roundToInt(),
                    viewport.screenY(geometry.mapOffsetY.toDouble()).roundToInt(),
                    (mapImage.width * viewport.zoom).roundToInt(),
                    (mapImage.height * viewport.zoom).roundToInt(),
                    null,
                )
                gridLayer?.let { drawGrid(it, mapImage, geometry, viewport) }
                content.objects.filterIsInstance<OperationDrawingObject>().forEach { drawDrawing(it, mapImage, geometry, viewport) }
                content.objects.filterIsInstance<OperationUnit>().forEach { drawUnit(it, content, mapImage, geometry, viewport) }
                content.objects.filterIsInstance<OperationGraphic>().forEach { drawGraphic(it, content, mapImage, geometry, viewport) }
            }
        }

    private fun Graphics2D.drawGrid(layer: GridLayer, mapImage: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport) {
        val overlay = MapViewerGridOverlayLayoutBuilder.build(
            MapViewerGridOverlayRequest(layer, viewport, geometry.mapOffsetX.toDouble(), geometry.mapOffsetY.toDouble(), mapImage.width, mapImage.height),
        ) { text, size, strong ->
            font = Font(Font.SANS_SERIF, if (strong) Font.BOLD else Font.PLAIN, size.roundToInt().coerceAtLeast(1))
            ViewerPoint(fontMetrics.stringWidth(text).toDouble(), fontMetrics.height.toDouble())
        }
        overlay.lineSegments.forEach { segment ->
            color = segment.colour
            stroke = BasicStroke(segment.thickness.toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
            draw(Line2D.Double(segment.start.x, segment.start.y, segment.end.x, segment.end.y))
        }
        overlay.labels.forEach { label ->
            font = Font(Font.SANS_SERIF, if (label.strong) Font.BOLD else Font.PLAIN, label.fontSize.roundToInt().coerceAtLeast(1))
            color = label.colour
            drawCentered(label.text, label.centre)
        }
    }

    private fun Graphics2D.drawDrawing(value: OperationDrawingObject, mapImage: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport) {
        color = Color(value.drawing.colour.value, true)
        stroke = BasicStroke(DRAWING_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        when (val drawing = value.drawing) {
            is OperationFreeformDrawing -> drawing.points.zipWithNext()
                .forEach { (a, b) -> drawLine(a.screen(mapImage, geometry, viewport), b.screen(mapImage, geometry, viewport)) }

            is OperationLineDrawing -> drawLine(drawing.start.screen(mapImage, geometry, viewport), drawing.end.screen(mapImage, geometry, viewport))
            is OperationRectangleDrawing -> {
                val a = drawing.start.screen(mapImage, geometry, viewport);
                val b = drawing.end.screen(mapImage, geometry, viewport)
                withRotation((a.x + b.x) / 2, (a.y + b.y) / 2, drawing.rotationDegrees) { draw(Rectangle2D.Double(min(a.x, b.x), min(a.y, b.y), abs(b.x - a.x), abs(b.y - a.y))) }
            }

            is OperationOvalDrawing -> {
                val center = drawing.centre.screen(mapImage, geometry, viewport)
                val rx = drawing.xRadius * mapImage.width * viewport.zoom;
                val ry = drawing.yRadius * mapImage.height * viewport.zoom
                withRotation(center.x, center.y, drawing.rotationDegrees) { draw(Ellipse2D.Double(center.x - rx, center.y - ry, rx * 2, ry * 2)) }
            }
        }
        value.customProperties["displayName"]?.takeIf(String::isNotBlank)?.let { label ->
            val bounds = value.selectionBounds(mapImage.width.toDouble() / mapImage.height)
            drawLabel(label, MapCoordinate((bounds.left + bounds.right) / 2, bounds.top).screen(mapImage, geometry, viewport))
        }
    }

    private fun Graphics2D.drawUnit(value: OperationUnit, content: OperationContent, mapImage: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport) {
        val template = content.unitTemplate(value.unitTemplateId) ?: return
        val center = value.coordinate.screen(mapImage, geometry, viewport)
        val width = value.normalizedWidth * mapImage.width * viewport.zoom
        val height = value.normalizedHeight * mapImage.height * viewport.zoom
        val x = center.x - width / 2;
        val y = center.y - height / 2
        value.facingDegrees?.let { drawFacing(x, y, width, height, it) }
        drawImage(template.image, x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt(), null)
        if (value.destroyed) drawDestroyed(x, y, width, height)
    }

    private fun Graphics2D.drawGraphic(value: OperationGraphic, content: OperationContent, mapImage: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport) {
        val template = content.graphicTemplate(value.graphicTemplateId) ?: return
        val center = value.coordinate.screen(mapImage, geometry, viewport)
        val width = value.normalizedWidth * mapImage.width * viewport.zoom;
        val height = value.normalizedHeight * mapImage.height * viewport.zoom
        drawImage(template.image, (center.x - width / 2).roundToInt(), (center.y - height / 2).roundToInt(), width.roundToInt(), height.roundToInt(), null)
        (value.customProperties["displayName"]?.takeIf(String::isNotBlank) ?: value.instanceName.takeIf(String::isNotBlank))?.let {
            drawLabel(
                it,
                ViewerPoint(center.x, center.y - height / 2)
            )
        }
    }

    private fun Graphics2D.drawFacing(x: Double, y: Double, width: Double, height: Double, degrees: Int) {
        val cx = x + width / 2;
        val cy = y + height / 2;
        val radius = min(width, height) * .72
        color = Color(Palette.Colours.MAIN.red, Palette.Colours.MAIN.green, Palette.Colours.MAIN.blue, 160)
        stroke = BasicStroke((min(width, height) * .08).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        draw(Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2))
        val radians = Math.toRadians(degrees.toDouble());
        val dx = sin(radians);
        val dy = -cos(radians);
        val px = cos(radians);
        val py = sin(radians)
        val tipX = cx + dx * radius * 1.22;
        val tipY = cy + dy * radius * 1.22;
        val baseX = cx + dx * radius * .72;
        val baseY = cy + dy * radius * .72;
        val half = min(width, height) * .16
        fill(Path2D.Double().apply { moveTo(tipX, tipY); lineTo(baseX + px * half, baseY + py * half); lineTo(baseX - px * half, baseY - py * half); closePath() })
    }

    private fun Graphics2D.drawDestroyed(x: Double, y: Double, width: Double, height: Double) {
        color = Color(Palette.Colours.RED.red, Palette.Colours.RED.green, Palette.Colours.RED.blue, 160)
        stroke = BasicStroke((min(width, height) * .18).toFloat())
        draw(Line2D.Double(x, y, x + width, y + height)); draw(Line2D.Double(x, y + height, x + width, y))
    }

    private fun Graphics2D.drawCentered(text: String, center: ViewerPoint) =
        fontMetrics.let { drawString(text, (center.x - it.stringWidth(text) / 2).toFloat(), (center.y + (it.ascent - it.descent) / 2).toFloat()) }

    private fun Graphics2D.drawLine(start: ViewerPoint, end: ViewerPoint) = draw(Line2D.Double(start.x, start.y, end.x, end.y))
    private fun Graphics2D.drawLabel(text: String, anchor: ViewerPoint) {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 12); color = Palette.Colours.WHITE; drawString(
            text,
            (anchor.x - fontMetrics.stringWidth(text) / 2).toFloat(),
            (anchor.y - 3).toFloat()
        )
    }

    private fun Graphics2D.withRotation(x: Double, y: Double, degrees: Double, block: Graphics2D.() -> Unit) {
        val old = transform; rotate(Math.toRadians(degrees), x, y); try {
            block()
        } finally {
            transform = old
        }
    }

    private fun OperationDrawingPoint.screen(image: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport) = MapCoordinate(x, y).screen(image, geometry, viewport)
    private fun MapCoordinate.screen(image: BufferedImage, geometry: ViewerGeometry, viewport: MapViewport) =
        ViewerPoint(viewport.screenX(geometry.mapOffsetX + x * image.width), viewport.screenY(geometry.mapOffsetY + y * image.height))

    private fun MapViewport.thumbnailViewport(width: Int) =
        copy(zoom = width / visibleWidth, viewportWidth = width, viewportHeight = (width * viewportHeight.toDouble() / viewportWidth).roundToInt().coerceAtLeast(1))

    private fun Graphics2D.highQuality(block: Graphics2D.() -> Unit) {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        ); setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); try {
            block()
        } finally {
            dispose()
        }
    }

    private companion object {
        const val DEFAULT_WIDTH = 512;
        const val TURN_ASPECT = 16.0 / 9.0;
        const val DRAWING_STROKE = 2f
    }
}
