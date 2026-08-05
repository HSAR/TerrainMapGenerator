package io.hsar.mapgenerator.commandgui.korge.overlay

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerGridFontRole
import io.hsar.mapgenerator.commandgui.MapViewerGridOverlayLayoutBuilder
import io.hsar.mapgenerator.commandgui.MapViewerGridReference
import io.hsar.mapgenerator.commandgui.MapViewerMeasurement
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.grid.GridLayer
import java.awt.Color

internal data class LegacyGridOverlayState(
    val viewport: Rectangle,
    val mapWorldX: Double,
    val mapWorldY: Double,
    val mapOffsetX: Double,
    val mapOffsetY: Double,
    val scale: Double,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val backgroundWidth: Int,
    val backgroundHeight: Int,
    val visibleCanvasLeft: Double,
    val visibleCanvasTop: Double,
    val visibleCanvasRight: Double,
    val visibleCanvasBottom: Double,
    val activeReference: LegacyGridCursorReference?,
    val measurement: LegacyGridMeasurement?,
    val gridLayer: GridLayer,
) {
    fun production() = GridOverlayState(
        viewport, mapWorldX, mapWorldY, mapOffsetX, mapOffsetY, scale, pixelWidth, pixelHeight,
        backgroundWidth, backgroundHeight, visibleCanvasLeft, visibleCanvasTop, visibleCanvasRight, visibleCanvasBottom,
        activeReference?.let {
            MapViewerGridReference(
                MapCoordinate((it.screenX - mapOffsetX) / scale / pixelWidth, (it.screenY - mapOffsetY) / scale / pixelHeight),
                it.text,
                it.color,
            )
        },
        measurement?.let {
            MapViewerMeasurement(
                MapCoordinate(it.originMapX / pixelWidth, it.originMapY / pixelHeight),
                MapCoordinate(it.destinationMapX / pixelWidth, it.destinationMapY / pixelHeight),
                it.distanceUnits, it.unitLabel, it.bearingDegrees,
            )
        },
        gridLayer,
    )
}

internal data class LegacyGridCursorReference(val screenX: Double, val screenY: Double, val text: String, val color: Color? = null)
internal data class LegacyGridMeasurement(
    val originMapX: Double, val originMapY: Double, val destinationMapX: Double, val destinationMapY: Double,
    val distanceUnits: Double, val unitLabel: String, val bearingDegrees: Int,
) {
    val distanceText get() = MapViewerMeasurement(MapCoordinate(0.0, 0.0), MapCoordinate(0.0, 0.0), distanceUnits, unitLabel, bearingDegrees).distanceText
    val bearingText get() = bearingDegrees.toString().padStart(3, '0') + "°"
}

internal data class LegacyGridRect(val x: Double, val y: Double, val width: Double, val height: Double, val color: Color)
internal data class LegacyGridArrow(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val color: Color)
internal data class LegacyGridLabelStyle(val role: MapViewerFontRole, val size: Double, val color: Color)
internal data class LegacyGridLabelShadow(val offsetX: Double, val offsetY: Double, val color: Color)
internal data class LegacyGridLabel(val text: String, val x: Double, val y: Double, val style: LegacyGridLabelStyle, val shadows: List<LegacyGridLabelShadow>)
internal data class LegacyGridOverlayLayout(val lineRects: List<LegacyGridRect>, val overlayArrows: List<LegacyGridArrow>, val labels: List<LegacyGridLabel>)

internal object NeutralGridOverlayParityBuilder {
    fun build(state: LegacyGridOverlayState, measureText: (String, LegacyGridLabelStyle) -> Rectangle): LegacyGridOverlayLayout {
        val production = state.production()
        val neutral = MapViewerGridOverlayLayoutBuilder.build(production.toNeutralRequest()) { text, size, role ->
            measureText(text, LegacyGridLabelStyle(role.korge(), size, state.gridLayer.colour)).let { ViewerPoint(it.width, it.height) }
        }
        return LegacyGridOverlayLayout(
            neutral.lineSegments.map { segment ->
                val rect = segment.toKorgeGridRect(production)
                LegacyGridRect(rect.x, rect.y, rect.width, rect.height, segment.colour)
            },
            neutral.arrows.map { LegacyGridArrow(it.start.x, it.start.y, it.end.x, it.end.y, it.colour) },
            neutral.labels.map { label ->
                LegacyGridLabel(
                    label.text, label.centre.x, label.centre.y,
                    LegacyGridLabelStyle(label.role.korge(), label.fontSize, label.colour),
                    label.shadows.map { LegacyGridLabelShadow(it.offset.x, it.offset.y, it.colour) },
                )
            },
        )
    }

    private fun MapViewerGridFontRole.korge() = when (this) {
        MapViewerGridFontRole.MAIN -> MapViewerFontRole.MAIN
        MapViewerGridFontRole.STRONG -> MapViewerFontRole.STRONG
    }
}
