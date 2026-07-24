package io.hsar.mapgenerator.commandgui.korge.drawing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateDelta
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObject
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectMenuAction
import java.awt.Color

internal data class MapDrawingPoint(
    val x: Double,
    val y: Double,
)

internal sealed interface MapDrawing {
    val color: Color

    @get:JsonIgnore
    val name: String
}

internal data class FreeformDrawing(
    val points: List<MapDrawingPoint>,
    override val color: Color,
) : MapDrawing {
    override val name: String = "Freeform Drawing"
}

internal data class LineDrawing(
    val start: MapDrawingPoint,
    val end: MapDrawingPoint,
    override val color: Color,
) : MapDrawing {
    override val name: String = "Line Drawing"
}

internal data class RectangleDrawing(
    val start: MapDrawingPoint,
    val end: MapDrawingPoint,
    override val color: Color,
    val rotationDegrees: Double = 0.0,
) : MapDrawing {
    override val name: String = "Rectangle Drawing"
}

internal data class OvalDrawing(
    val center: MapDrawingPoint,
    @get:JsonProperty("xRadius")
    @param:JsonProperty("xRadius")
    val xRadius: Double,
    @get:JsonProperty("yRadius")
    @param:JsonProperty("yRadius")
    val yRadius: Double,
    override val color: Color,
    val rotationDegrees: Double = 0.0,
) : MapDrawing {
    override val name: String = "Oval Drawing"
}

internal data class PlacedDrawing(
    override val id: MapPlacedObjectId,
    var drawing: MapDrawing,
    var customProperties: Map<String, String> = emptyMap(),
) : MapPlacedObject {
    override fun snapshot(): DrawingSnapshot =
        DrawingSnapshot(
            id = id,
            drawing = drawing,
            customProperties = customProperties,
        )

    override fun containsMapCoordinate(
        mapCoordinate: MapCoordinate,
        tolerance: Double,
        coordinateSpace: io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace?,
    ): Boolean =
        MapDrawingHitTest.contains(
            drawing = drawing,
            point = MapDrawingPoint(mapCoordinate.x, mapCoordinate.y),
            tolerance = tolerance,
            coordinateSpace = coordinateSpace,
        )

    override fun contextMenuActions(): List<MapPlacedObjectMenuAction> =
        listOf(
            MapPlacedObjectMenuAction(
                label = "Delete",
                onSelected = {},
            ),
        )

    override fun moveBy(mapDelta: MapCoordinateDelta) {
        drawing = MapDrawingTranslator.translate(drawing, mapDelta)
    }
}

internal data class MapDrawingState(
    val drawings: List<PlacedDrawing> = emptyList(),
    val preview: MapDrawing? = null,
)
