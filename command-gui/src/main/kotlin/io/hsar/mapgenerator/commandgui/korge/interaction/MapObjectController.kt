package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingState
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import java.util.*

internal class MapObjectController(
    private val unitManager: MapPlacedUnitManager,
    private val graphicManager: MapPlacedGraphicManager? = null,
    private val drawingStore: MapDrawingStore,
    private val mapWidth: Int = 1,
    private val mapHeight: Int = 1,
    private val onDrawingsChanged: (MapDrawingState) -> Unit = {},
    private val clearTransientState: () -> Unit = {},
    private val uuidGenerator: () -> UUID = UUID::randomUUID,
) {
    private var nextUnitInstanceNumber = 1

    fun createUnit(
        template: MapViewerUnitTemplate,
        mapCoordinate: MapCoordinate,
        instanceName: String? = null,
    ) = unitManager.buildSnapshot(
        id = MapPlacedObjectId(uuidGenerator()),
        unitTemplateId = template.id,
        template = template,
        instanceName = instanceName ?: nextUnitInstanceName(template.name),
        mapCoordinate = mapCoordinate,
    ).also(::createObject)

    fun createDrawing(drawing: MapDrawing) = DrawingSnapshot(
        id = MapPlacedObjectId(uuidGenerator()),
        drawing = drawing,
    ).also(::createObject)

    fun createGraphic(template: GraphicTemplateSnapshot, mapCoordinate: MapCoordinate): GraphicSnapshot {
        val bitmap = template.template.bitmap
        val width = DEFAULT_GRAPHIC_WIDTH
        return GraphicSnapshot(
            id = MapPlacedObjectId(uuidGenerator()),
            graphicTemplateId = template.id,
            template = template.template,
            instanceName = template.template.name,
            mapCoordinate = mapCoordinate,
            normalizedWidth = width,
            normalizedHeight = width * mapWidth.toDouble() / mapHeight * bitmap.height.toDouble() / bitmap.width.toDouble(),
        ).also(::createObject)
    }

    fun createObject(snapshot: MapPlacedObjectSnapshot) {
        when (snapshot) {
            is UnitSnapshot -> unitManager.upsert(snapshot)
            is DrawingSnapshot -> {
                drawingStore.upsert(snapshot)
                onDrawingsChanged(drawingStore.state)
            }
            is GraphicSnapshot -> graphicManager?.upsert(snapshot)
        }
        clearTransientState()
    }

    fun restoreAll(snapshots: List<MapPlacedObjectSnapshot>) {
        unitManager.restoreAll(snapshots.filterIsInstance<UnitSnapshot>())
        drawingStore.replaceAll(snapshots.filterIsInstance<DrawingSnapshot>())
        graphicManager?.restoreAll(snapshots.filterIsInstance<GraphicSnapshot>())
        onDrawingsChanged(drawingStore.state)
        clearTransientState()
    }

    fun updateObject(snapshot: MapPlacedObjectSnapshot) {
        when (snapshot) {
            is UnitSnapshot -> unitManager.upsert(snapshot)
            is DrawingSnapshot -> {
                drawingStore.upsert(snapshot)
                onDrawingsChanged(drawingStore.state)
            }
            is GraphicSnapshot -> graphicManager?.upsert(snapshot)
        }
        clearTransientState()
    }

    fun previewObject(snapshot: MapPlacedObjectSnapshot) {
        when (snapshot) {
            is UnitSnapshot -> unitManager.preview(snapshot)
            is DrawingSnapshot -> {
                drawingStore.upsert(snapshot)
                onDrawingsChanged(drawingStore.state)
            }
            is GraphicSnapshot -> graphicManager?.preview(snapshot)
        }
    }

    fun applyObjectEdit(id: MapPlacedObjectId, changes: Map<String, JsonNode?>) {
        snapshot(id)
            ?.let { MapPlacedObjectPatch.apply(it, changes) }
            ?.let(::updateObject)
    }

    fun deleteObject(id: MapPlacedObjectId): MapPlacedObjectSnapshot? {
        val removed = unitManager.remove(id) ?: graphicManager?.remove(id) ?: drawingStore.remove(id)?.also {
            onDrawingsChanged(drawingStore.state)
        }
        clearTransientState()
        return removed
    }

    fun snapshot(id: MapPlacedObjectId): MapPlacedObjectSnapshot? =
        unitManager.snapshot(id) ?: graphicManager?.snapshot(id) ?: drawingStore.snapshot(id)

    fun snapshots(): List<MapPlacedObjectSnapshot> =
        unitManager.snapshots() + (graphicManager?.snapshots().orEmpty()) + drawingStore.snapshots()

    private fun nextUnitInstanceName(baseName: String): String =
        "$baseName ${nextUnitInstanceNumber++}"
}

private const val DEFAULT_GRAPHIC_WIDTH = 0.032
