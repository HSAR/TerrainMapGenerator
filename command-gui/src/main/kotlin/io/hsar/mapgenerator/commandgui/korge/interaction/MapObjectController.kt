package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.JsonNode
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingState
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import java.util.*

internal class MapObjectController(
    private val unitManager: MapPlacedUnitManager,
    private val drawingStore: MapDrawingStore,
    private val onDrawingsChanged: (MapDrawingState) -> Unit = {},
    private val clearTransientState: () -> Unit = {},
    private val uuidGenerator: () -> UUID = UUID::randomUUID,
) {
    fun createUnit(
        template: MapViewerUnitTemplate,
        mapCoordinate: MapCoordinate,
        instanceName: String = template.name,
    ) = unitManager.buildSnapshot(
        id = MapPlacedObjectId(uuidGenerator()),
        unitTemplateId = template.id,
        template = template,
        instanceName = instanceName,
        mapCoordinate = mapCoordinate,
    ).also(::createObject)

    fun createDrawing(drawing: MapDrawing) = DrawingSnapshot(
        id = MapPlacedObjectId(uuidGenerator()),
        drawing = drawing,
    ).also(::createObject)

    fun createObject(snapshot: MapPlacedObjectSnapshot) {
        when (snapshot) {
            is UnitSnapshot -> unitManager.upsert(snapshot)
            is DrawingSnapshot -> {
                drawingStore.upsert(snapshot)
                onDrawingsChanged(drawingStore.state)
            }
        }
        clearTransientState()
    }

    fun updateObject(snapshot: MapPlacedObjectSnapshot) {
        when (snapshot) {
            is UnitSnapshot -> unitManager.upsert(snapshot)
            is DrawingSnapshot -> {
                drawingStore.upsert(snapshot)
                onDrawingsChanged(drawingStore.state)
            }
        }
        clearTransientState()
    }

    fun applyObjectEdit(id: MapPlacedObjectId, changes: Map<String, JsonNode?>) {
        snapshot(id)
            ?.let { MapPlacedObjectPatch.apply(it, changes) }
            ?.let(::updateObject)
    }

    fun deleteObject(id: MapPlacedObjectId): MapPlacedObjectSnapshot? {
        val removed = unitManager.remove(id) ?: drawingStore.remove(id)?.also {
            onDrawingsChanged(drawingStore.state)
        }
        clearTransientState()
        return removed
    }

    fun snapshot(id: MapPlacedObjectId): MapPlacedObjectSnapshot? =
        unitManager.snapshot(id) ?: drawingStore.snapshot(id)

    fun snapshots(): List<MapPlacedObjectSnapshot> =
        unitManager.snapshots() + drawingStore.snapshots()
}
