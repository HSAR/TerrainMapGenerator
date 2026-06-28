package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiStateStyle
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUnitEditDialog
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.image
import korlibs.math.geom.Point
import korlibs.math.geom.Vector2D
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(KorgeInternal::class)
internal class MapPlacedUnitManager(
    private val root: Container,
    private val unitLayer: Container,
    private val geometry: ViewerGeometry,
    private val mapMetresPerPixel: Double,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val gridLayer: GridLayer? = null,
    private val viewportProvider: () -> CameraViewport,
    private val contextMenuManager: ContextMenuManager,
    private val deferredActionRunner: ContextMenuDeferredActionRunner,
    private val canInteractWithUnits: () -> Boolean = { true },
    private val isHigherPriorityDragInProgress: () -> Boolean = { false },
    private val onMovePreviewChanged: (MapObjectMovePreview?) -> Unit = {},
    private val onObjectMoved: (MapPlacedObjectSnapshot, MapPlacedObjectSnapshot) -> Unit = { _, _ -> },
    private val onObjectDeleted: (MapPlacedObjectSnapshot) -> Unit = {},
    private val onUnitEdited: (UnitSnapshot, UnitSnapshot) -> Unit = { _, _ -> },
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private val units = mutableListOf<PlacedUnit>()
    private val dragPreviewLayer = unitLayer.container()
    private var selectedUnitIds = emptySet<MapPlacedObjectId>()
    private var dragState: DragState? = null
    private var editDialog: MapViewerUnitEditDialog? = null
    private var editDialogUnitId: MapPlacedObjectId? = null
    private var shouldRestoreEditSnapshot = true

    fun install() {
        dragPreviewLayer.zIndex = DRAG_PREVIEW_Z_INDEX
        dragPreviewLayer.mouseEnabled = false

        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
        }
    }

    val isDragging: Boolean
        get() = dragState != null

    val isContextMenuOpen: Boolean
        get() = contextMenuManager.isOpen

    val isEditDialogOpen: Boolean
        get() = editDialog != null

    fun updateSelectedUnits(selectedIds: Set<MapPlacedObjectId>) {
        selectedUnitIds = selectedIds
        units.forEach { it.updateFacingOverlay() }
    }

    fun replaceUnitTemplate(snapshot: UnitTemplateSnapshot) {
        units
            .filter { it.unitTemplateId == snapshot.id }
            .forEach { it.updateTemplate(snapshot.template) }
    }

    private fun interactionsEnabled(): Boolean =
        canInteractWithUnits() && editDialog == null

    fun blocksScreenPoint(point: Vector2D): Boolean {
        if (!interactionsEnabled()) {
            return false
        }
        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(point, viewport)
        return units.any { unit -> unit.containsMapCoordinate(mapCoordinate) }
    }

    fun hasDragCandidate(point: Vector2D): Boolean {
        if (!interactionsEnabled()) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(point, viewport)
        return units.any { unit -> unit.containsMapCoordinate(mapCoordinate) }
    }

    fun resolvePlacement(screenPoint: Vector2D): MapCoordinate? =
        coordinateSpace.resolveScreenPoint(
            screenPoint = screenPoint,
            viewport = viewportProvider(),
            bounds = coordinateSpace.backgroundBounds,
        )

    fun selectionAt(screenPoint: Vector2D): MapPlacedObjectSelection? {
        if (!interactionsEnabled()) {
            return null
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(screenPoint, viewport)
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        return units
            .asSequence()
            .filter { it.containsMapCoordinate(mapCoordinate, tolerance, coordinateSpace) }
            .minByOrNull {
                val center = coordinateSpace.worldToScreen(coordinateSpace.toWorld(it.mapCoordinate), viewport)
                hypot(center.x - screenPoint.x, center.y - screenPoint.y)
            }
            ?.let { unit ->
                MapPlacedObjectSelection(
                    kind = MapPlacedObjectSelectionKind.UNIT,
                    id = unit.id,
                    distanceToPointer = hypot(
                        coordinateSpace.worldToScreen(coordinateSpace.toWorld(unit.mapCoordinate), viewport).x - screenPoint.x,
                        coordinateSpace.worldToScreen(coordinateSpace.toWorld(unit.mapCoordinate), viewport).y - screenPoint.y,
                    ),
                )
            }
    }

    fun beginDragAt(screenPoint: Vector2D): Boolean {
        if (dragState != null || !interactionsEnabled() || contextMenuManager.isOpen || isHigherPriorityDragInProgress()) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(screenPoint, viewport)
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val target = units
            .asSequence()
            .filter { it.containsMapCoordinate(mapCoordinate, tolerance, coordinateSpace) }
            .minByOrNull {
                val center = coordinateSpace.worldToScreen(coordinateSpace.toWorld(it.mapCoordinate), viewport)
                hypot(center.x - screenPoint.x, center.y - screenPoint.y)
            }
            ?: return false
        beginDrag(target, screenPoint, viewport)
        return true
    }

    fun openContextMenuAt(screenPoint: Vector2D): Boolean {
        if (!interactionsEnabled() || contextMenuManager.isOpen) {
            return false
        }

        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(screenPoint, viewport)
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val target = units
            .asSequence()
            .filter { it.containsMapCoordinate(mapCoordinate, tolerance, coordinateSpace) }
            .minByOrNull {
                val center = coordinateSpace.worldToScreen(coordinateSpace.toWorld(it.mapCoordinate), viewport)
                hypot(center.x - screenPoint.x, center.y - screenPoint.y)
            }
            ?: return false
        openContextMenu(target, screenPoint)
        return true
    }

    fun buildSnapshot(
        id: MapPlacedObjectId,
        unitTemplateId: UnitTemplateId,
        template: MapViewerUnitTemplate,
        instanceName: String,
        mapCoordinate: MapCoordinate,
        destroyed: Boolean = false,
        facingDegrees: Int? = null,
    ): UnitSnapshot {
        val unitSize = worldUnitSize(template.bitmap)
        return UnitSnapshot(
            id = id,
            unitTemplateId = unitTemplateId,
            template = template,
            instanceName = instanceName,
            destroyed = destroyed,
            facingDegrees = facingDegrees,
            mapCoordinate = mapCoordinate,
            normalizedWidth = unitSize.worldWidth / mapWidth,
            normalizedHeight = unitSize.worldHeight / mapHeight,
        )
    }

    fun upsert(snapshot: UnitSnapshot) {
        val existing = units.firstOrNull { it.id == snapshot.id }
        if (existing != null) {
            existing.unitTemplateId = snapshot.unitTemplateId
            existing.template = snapshot.template
            existing.instanceName = snapshot.instanceName
            existing.destroyed = snapshot.destroyed
            existing.facingDegrees = snapshot.facingDegrees
            existing.mapCoordinate = snapshot.mapCoordinate
            existing.normalizedWidth = snapshot.normalizedWidth
            existing.normalizedHeight = snapshot.normalizedHeight
            val unitSize = worldUnitSize(snapshot.template.bitmap)
            existing.worldWidth = unitSize.worldWidth
            existing.worldHeight = unitSize.worldHeight
            existing.rebuildVisual(snapshot.template.bitmap)
            existing.view.scale = unitSize.worldScale
            positionView(existing)
            return
        }

        val unitSize = worldUnitSize(snapshot.template.bitmap)
        val placedUnit = PlacedUnit(
            id = snapshot.id,
            view = unitLayer.container(),
            unitTemplateId = snapshot.unitTemplateId,
            template = snapshot.template,
            instanceName = snapshot.instanceName,
            destroyed = snapshot.destroyed,
            facingDegrees = snapshot.facingDegrees,
            mapCoordinate = snapshot.mapCoordinate,
            normalizedWidth = snapshot.normalizedWidth,
            normalizedHeight = snapshot.normalizedHeight,
            worldWidth = unitSize.worldWidth,
            worldHeight = unitSize.worldHeight,
        )
        placedUnit.view.scale = unitSize.worldScale
        positionView(placedUnit)
        units += placedUnit
    }

    fun remove(id: MapPlacedObjectId): UnitSnapshot? =
        units.indexOfFirst { it.id == id }
            .takeIf { it >= 0 }
            ?.let { index ->
                units.removeAt(index).also { removed ->
                    removed.view.removeFromParent()
                    if (dragState?.unit?.id == removed.id) {
                        dragState?.preview?.container?.removeFromParent()
                        onMovePreviewChanged(null)
                        dragState = null
                    }
                    if (editDialogUnitId == removed.id) {
                        shouldRestoreEditSnapshot = false
                        editDialog?.close()
                    }
                }.snapshot()
            }

    fun snapshot(id: MapPlacedObjectId): UnitSnapshot? =
        units.firstOrNull { it.id == id }?.snapshot()

    fun snapshots(): List<UnitSnapshot> =
        units.map { it.snapshot() }

    fun cancelTransientState(targetId: MapPlacedObjectId? = null) {
        if (targetId == null || dragState?.unit?.id == targetId) {
            dragState?.let { state ->
                upsert(state.originalSnapshot)
                state.preview.container.removeFromParent()
                state.unit.view.visible = true
            }
            onMovePreviewChanged(null)
            dragState = null
        }
        if (targetId == null || editDialogUnitId == targetId) {
            editDialog?.close()
        }
    }

    private fun beginDrag(unit: PlacedUnit, screenPoint: Vector2D, viewport: CameraViewport) {
        val currentScreenPoint = coordinateSpace.worldToScreen(coordinateSpace.toWorld(unit.mapCoordinate), viewport)
        val grabOffset = Vector2D(
            x = screenPoint.x - currentScreenPoint.x,
            y = screenPoint.y - currentScreenPoint.y,
        )
        val mapCoordinate = coordinateSpace.resolveScreenPointClamped(
            screenPoint = Vector2D(
                x = screenPoint.x - grabOffset.x,
                y = screenPoint.y - grabOffset.y,
            ),
            viewport = viewport,
            bounds = coordinateSpace.backgroundBounds,
        )
        val worldPoint = coordinateSpace.toWorld(mapCoordinate)

        unit.view.visible = false
        dragPreviewLayer.removeChildren()
        val preview = createVisual(
            parent = dragPreviewLayer,
            bitmap = unit.template.bitmap,
            destroyed = unit.destroyed,
            facingDegrees = unit.facingDegrees,
            facingActive = true,
        ).also {
            it.container.scale = unit.view.scaleX
            it.container.x = worldPoint.x - unit.worldWidth * 0.5
            it.container.y = worldPoint.y - unit.worldHeight * 0.5
            it.container.zIndex = DRAG_PREVIEW_Z_INDEX
        }

        dragState = DragState(
            unit = unit,
            preview = preview,
            offset = grabOffset,
            originalSnapshot = unit.snapshot(),
        )
        onMovePreviewChanged(
            MapObjectMovePreview(
                origin = unit.snapshot().mapCoordinate,
                destination = mapCoordinate,
                color = unit.snapshot().selectionAccentColor(),
            ),
        )
    }

    private fun updateDrag(pointer: Vector2D, leftHeld: Boolean) {
        val state = dragState ?: return
        if (!interactionsEnabled()) {
            dragState = null
            state.preview.container.removeFromParent()
            upsert(state.originalSnapshot)
            state.unit.view.visible = true
            onMovePreviewChanged(null)
            return
        }
        if (!leftHeld) {
            val updatedSnapshot = state.unit.snapshot()
            if (updatedSnapshot != state.originalSnapshot) {
                onObjectMoved(state.originalSnapshot, updatedSnapshot)
            }
            dragState = null
            state.preview.container.removeFromParent()
            state.unit.view.visible = true
            onMovePreviewChanged(null)
            return
        }

        val mapCoordinate = coordinateSpace.resolveScreenPointClamped(
            screenPoint = Vector2D(
                x = pointer.x - state.offset.x,
                y = pointer.y - state.offset.y,
            ),
            viewport = viewportProvider(),
            bounds = coordinateSpace.backgroundBounds,
        )
        val worldPoint = coordinateSpace.toWorld(mapCoordinate)

        state.unit.mapCoordinate = mapCoordinate
        positionView(state.unit)
        state.preview.container.x = worldPoint.x - state.unit.worldWidth * 0.5
        state.preview.container.y = worldPoint.y - state.unit.worldHeight * 0.5
        onMovePreviewChanged(
            MapObjectMovePreview(
                origin = state.originalSnapshot.mapCoordinate,
                destination = mapCoordinate,
                color = state.unit.snapshot().selectionAccentColor(),
            ),
        )
    }

    private fun openContextMenu(unit: PlacedUnit, screenPoint: Vector2D) {
        if (!interactionsEnabled()) {
            return
        }
        contextMenuManager.open(unit, screenPoint)
    }

    private fun openEditDialog(unit: PlacedUnit) {
        editDialog?.close()
        editDialogUnitId = unit.id
        val originalSnapshot = unit.snapshot()
        val viewport = viewportProvider()
        val position = initialReferenceFields(unit)
        val viewportWidth = viewport.viewportWidth
        val viewportHeight = viewport.viewportHeight
        var saveRequested = false
        shouldRestoreEditSnapshot = true
        onMovePreviewChanged(null)
        editDialog = MapViewerUnitEditDialog(
            root = root,
            unitType = unit.template,
            initialName = unit.instanceName,
            initialPositionX = position.easting,
            initialPositionY = position.northing,
            initialDestroyed = unit.destroyed,
            initialFacing = UnitFacingResolver.format(unit.facingDegrees),
            onPositionPreviewChanged = { previewPositionX, previewPositionY ->
                val updatedCoordinate = UnitEditPositionResolver.resolve(
                    currentCoordinate = originalSnapshot.mapCoordinate,
                    initialPositionX = position.easting,
                    initialPositionY = position.northing,
                    newPositionX = previewPositionX,
                    newPositionY = previewPositionY,
                ) { easting, northing -> referenceToMapCoordinate(easting, northing, viewportWidth, viewportHeight) }
                if (updatedCoordinate != null) {
                    unit.mapCoordinate = updatedCoordinate
                    positionView(unit)
                    onMovePreviewChanged(
                        MapObjectMovePreview(
                            origin = originalSnapshot.mapCoordinate,
                            destination = updatedCoordinate,
                            referenceText = "${previewPositionX.trim()} ${previewPositionY.trim()}",
                            color = unit.snapshot().selectionAccentColor(),
                        ),
                    )
                } else {
                    upsert(originalSnapshot)
                    onMovePreviewChanged(null)
                }
            },
            onDestroyedPreviewChanged = { destroyed ->
                unit.destroyed = destroyed
                unit.updateStateOverlay()
            },
            onFacingPreviewChanged = { facingText ->
                UnitFacingResolver.parse(facingText)
                    .takeIf { it.isValid }
                    ?.let { facing ->
                        unit.facingDegrees = facing.degrees
                        unit.updateFacingOverlay(forceActive = true)
                    }
                    ?: run {
                        unit.facingDegrees = originalSnapshot.facingDegrees
                        unit.updateFacingOverlay(forceActive = true)
                    }
            },
            onSave = save@{ newName, newPositionX, newPositionY, newDestroyed, newFacingText ->
                val nameChanged = newName != originalSnapshot.instanceName
                val positionChanged = newPositionX.trim() != position.easting.trim() ||
                        newPositionY.trim() != position.northing.trim()
                val updatedCoordinate = UnitEditPositionResolver.resolve(
                    currentCoordinate = originalSnapshot.mapCoordinate,
                    initialPositionX = position.easting,
                    initialPositionY = position.northing,
                    newPositionX = newPositionX,
                    newPositionY = newPositionY,
                ) { easting, northing ->
                    referenceToMapCoordinate(easting, northing, viewportWidth, viewportHeight)
                } ?: return@save false
                val updatedFacing = UnitFacingResolver.parse(newFacingText).takeIf { it.isValid }?.degrees ?: return@save false
                val destroyedChanged = newDestroyed != originalSnapshot.destroyed
                val facingChanged = updatedFacing != originalSnapshot.facingDegrees
                if (nameChanged || positionChanged || destroyedChanged || facingChanged) {
                    saveRequested = true
                    unit.instanceName = newName
                    unit.mapCoordinate = updatedCoordinate
                    unit.destroyed = newDestroyed
                    unit.facingDegrees = updatedFacing
                    unit.updateStateOverlay()
                    unit.updateFacingOverlay()
                    positionView(unit)
                    onMovePreviewChanged(null)
                    onUnitEdited(originalSnapshot, unit.snapshot())
                }
                true
            },
            onDismiss = {
                if (!saveRequested && shouldRestoreEditSnapshot) {
                    upsert(originalSnapshot)
                }
                shouldRestoreEditSnapshot = true
                onMovePreviewChanged(null)
                editDialog = null
                editDialogUnitId = null
            },
        ).also {
            it.show(viewportWidth, viewportHeight)
        }
    }

    private fun initialReferenceFields(unit: PlacedUnit): GridReferenceFields {
        val mapPixels = coordinateSpace.toMapPixels(unit.mapCoordinate)
        return gridLayer?.let { layer ->
            val viewport = viewportProvider()
            val viewportWidth = viewport.viewportWidth
            val viewportHeight = viewport.viewportHeight
            GridReferenceFormatter.formatFields(
                layer = layer,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                mapX = mapPixels.x,
                mapY = mapPixels.y,
                gutterWidth = viewportWidth,
                gutterHeight = viewportHeight,
            )
        } ?: GridReferenceFields(
            easting = fallbackReference(mapCoordinate = unit.mapCoordinate.x),
            northing = fallbackReference(mapCoordinate = unit.mapCoordinate.y),
        )
    }

    private fun referenceToMapCoordinate(
        easting: String,
        northing: String,
        gutterWidth: Int,
        gutterHeight: Int,
    ): MapCoordinate? {
        val layer = gridLayer
        return if (layer != null) {
            GridReferenceFormatter.parseFields(
                layer = layer,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                easting = easting,
                northing = northing,
                gutterWidth = gutterWidth,
                gutterHeight = gutterHeight,
            )?.let { mapPixels ->
                coordinateSpace.fromMapPixels(Vector2D(mapPixels.x, mapPixels.y))
            }
        } else {
            val parsedX = easting.trim().toIntOrNull() ?: return null
            val parsedY = northing.trim().toIntOrNull() ?: return null
            MapCoordinate(
                x = parsedX.coerceIn(-9_999, 9_999) / 9_999.0,
                y = parsedY.coerceIn(-9_999, 9_999) / 9_999.0,
            )
        }
    }

    private fun fallbackReference(mapCoordinate: Double): String {
        val scaled = (mapCoordinate.coerceIn(-1.0, 1.0) * 9_999).toInt()
        return if (scaled < 0) {
            "-${scaled.unaryMinus().toString().padStart(4, '0')}"
        } else {
            scaled.toString().padStart(4, '0')
        }
    }

    private inner class PlacedUnit(
        override val id: MapPlacedObjectId,
        val view: Container,
        var unitTemplateId: UnitTemplateId,
        var template: MapViewerUnitTemplate,
        var instanceName: String,
        var destroyed: Boolean,
        var facingDegrees: Int?,
        var mapCoordinate: MapCoordinate,
        var normalizedWidth: Double,
        var normalizedHeight: Double,
        var worldWidth: Double,
        var worldHeight: Double,
    ) : MapPlacedObject {
        private var facingLayer: Container = view.container().also { it.zIndex = 0.0 }
        var icon: Image = view.image(template.bitmap).also { it.zIndex = 1.0 }
            private set
        private var overlayLayer: Container = view.container().also { it.zIndex = 2.0 }

        init {
            updateFacingOverlay()
            updateStateOverlay()
        }

        override fun snapshot(): UnitSnapshot =
            UnitSnapshot(
                id = id,
                unitTemplateId = unitTemplateId,
                template = template,
                instanceName = instanceName,
                destroyed = destroyed,
                facingDegrees = facingDegrees,
                mapCoordinate = mapCoordinate,
                normalizedWidth = normalizedWidth,
                normalizedHeight = normalizedHeight,
            )

        override fun containsMapCoordinate(
            mapCoordinate: MapCoordinate,
            tolerance: Double,
            coordinateSpace: MapCoordinateSpace?,
        ): Boolean {
            val halfWidth = normalizedWidth * 0.5
            val halfHeight = normalizedHeight * 0.5
            return mapCoordinate.x in (this.mapCoordinate.x - halfWidth - tolerance)..(this.mapCoordinate.x + halfWidth + tolerance) &&
                    mapCoordinate.y in (this.mapCoordinate.y - halfHeight - tolerance)..(this.mapCoordinate.y + halfHeight + tolerance)
        }

        override fun contextMenuActions(): List<MapPlacedObjectMenuAction> =
            listOf(
                MapPlacedObjectMenuAction(
                    label = "Edit Unit",
                    onSelected = { deferredActionRunner.launch { openEditDialog(this) } },
                ),
                MapPlacedObjectMenuAction(
                    label = "Delete Unit",
                    onSelected = { onObjectDeleted(snapshot()) },
                ),
            )

        override fun moveBy(mapDelta: MapCoordinateDelta) {
            mapCoordinate = MapCoordinate(
                x = mapCoordinate.x + mapDelta.x,
                y = mapCoordinate.y + mapDelta.y,
            )
            positionView(this)
        }

        fun rebuildVisual(bitmap: Bitmap32) {
            view.removeChildren()
            facingLayer = view.container().also { it.zIndex = 0.0 }
            icon = view.image(bitmap).also { it.zIndex = 1.0 }
            overlayLayer = view.container().also { it.zIndex = 2.0 }
            updateFacingOverlay()
            updateStateOverlay()
        }

        fun updateTemplate(template: MapViewerUnitTemplate) {
            unitTemplateId = template.id
            this.template = template
            val unitSize = worldUnitSize(template.bitmap)
            worldWidth = unitSize.worldWidth
            worldHeight = unitSize.worldHeight
            normalizedWidth = unitSize.worldWidth / mapWidth
            normalizedHeight = unitSize.worldHeight / mapHeight
            rebuildVisual(template.bitmap)
            view.scale = unitSize.worldScale
            positionView(this)
        }

        fun updateFacingOverlay(forceActive: Boolean = false) {
            facingLayer.removeChildren()
            facingDegrees?.let { degrees ->
                facingLayer.alpha = facingOverlayAlpha(forceActive || id in selectedUnitIds || dragState?.unit?.id == id)
                drawFacingIndicator(facingLayer, icon.bitmap.width.toDouble(), icon.bitmap.height.toDouble(), degrees)
            }
        }

        fun updateStateOverlay() {
            overlayLayer.removeChildren()
            overlayLayer.alpha = 1.0
            if (!destroyed) {
                return
            }
            overlayLayer.alpha = 0.5
            drawStateCross(overlayLayer, icon.bitmap.width.toDouble(), icon.bitmap.height.toDouble(), toRgba(Palette.Colours.RED))
        }
    }

    private data class DragState(
        val unit: PlacedUnit,
        val preview: UnitVisual,
        val offset: Vector2D,
        val originalSnapshot: UnitSnapshot,
    )

    private fun positionView(unit: PlacedUnit) {
        val worldPoint = coordinateSpace.toWorld(unit.mapCoordinate)
        unit.view.x = worldPoint.x - unit.worldWidth * 0.5
        unit.view.y = worldPoint.y - unit.worldHeight * 0.5
    }

    private data class UnitVisual(
        val container: Container,
        val icon: Image,
    )

    private fun createVisual(
        parent: Container,
        bitmap: Bitmap32,
        destroyed: Boolean,
        facingDegrees: Int? = null,
        facingActive: Boolean = false,
    ): UnitVisual {
        val container = parent.container()
        val facingLayer = container.container().also { it.zIndex = 0.0 }
        val icon = container.image(bitmap).also { it.zIndex = 1.0 }
        facingDegrees?.let { degrees ->
            facingLayer.alpha = facingOverlayAlpha(facingActive)
            drawFacingIndicator(facingLayer, bitmap.width.toDouble(), bitmap.height.toDouble(), degrees)
        }
        val overlayLayer = container.container().also { it.zIndex = 2.0 }
        if (destroyed) {
            overlayLayer.alpha = 0.5
            drawStateCross(overlayLayer, bitmap.width.toDouble(), bitmap.height.toDouble(), toRgba(Palette.Colours.RED))
        }
        return UnitVisual(container = container, icon = icon)
    }

    private fun drawFacingIndicator(
        parent: Container,
        width: Double,
        height: Double,
        facingDegrees: Int,
    ) {
        val center = Point(width * 0.5, height * 0.5)
        val baseRadius = minOf(width, height) * 0.72
        val outerRadius = baseRadius
        val innerRadius = baseRadius * 0.86
        val color = toRgba(Palette.Colours.MAIN)

        repeat(FACING_RING_SEGMENTS) { index ->
            val start = (index.toDouble() / FACING_RING_SEGMENTS) * PI * 2.0
            val end = ((index + 1.0) / FACING_RING_SEGMENTS) * PI * 2.0
            parent.graphics {
                fill(color) {
                    moveTo(center.x + cos(start) * outerRadius, center.y + sin(start) * outerRadius)
                    lineTo(center.x + cos(end) * outerRadius, center.y + sin(end) * outerRadius)
                    lineTo(center.x + cos(end) * innerRadius, center.y + sin(end) * innerRadius)
                    lineTo(center.x + cos(start) * innerRadius, center.y + sin(start) * innerRadius)
                    close()
                }
            }
        }

        val radians = Math.toRadians(facingDegrees.toDouble())
        val direction = Vector2D(sin(radians), -cos(radians))
        val perpendicular = Vector2D(cos(radians), sin(radians))
        val tip = Point(
            center.x + direction.x * outerRadius * 1.22,
            center.y + direction.y * outerRadius * 1.22,
        )
        val base = Point(
            center.x + direction.x * outerRadius * 0.72,
            center.y + direction.y * outerRadius * 0.72,
        )
        val halfWidth = minOf(width, height) * 0.16

        parent.graphics {
            fill(color) {
                moveTo(tip.x, tip.y)
                lineTo(base.x + perpendicular.x * halfWidth, base.y + perpendicular.y * halfWidth)
                lineTo(base.x - perpendicular.x * halfWidth, base.y - perpendicular.y * halfWidth)
                close()
            }
        }
    }

    private fun facingOverlayAlpha(active: Boolean): Double =
        if (active) {
            MapViewerUiStateStyle.ACTIVE_ALPHA / 255.0
        } else {
            MapViewerUiStateStyle.inactiveFillAlpha / 255.0
        }

    private fun drawStateCross(
        parent: Container,
        width: Double,
        height: Double,
        color: RGBA,
    ) {
        val thickness = minOf(width, height) * 0.18
        val cornerInset = thickness * 0.85
        val centerInset = thickness * 0.48

        fun arm(points: List<Point>) {
            parent.graphics {
                fill(color) {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
            }
        }

        arm(
            listOf(
                Point(0.0, cornerInset),
                Point(cornerInset, 0.0),
                Point(width * 0.5 - centerInset, height * 0.5 - centerInset),
                Point(width * 0.5 + centerInset, height * 0.5 + centerInset),
                Point(width - cornerInset, height),
                Point(width, height - cornerInset),
                Point(width * 0.5 + centerInset, height * 0.5 - centerInset),
                Point(width * 0.5 - centerInset, height * 0.5 + centerInset),
            ),
        )

        arm(
            listOf(
                Point(0.0, height - cornerInset),
                Point(cornerInset, height),
                Point(width * 0.5 - centerInset, height * 0.5 + centerInset),
                Point(width * 0.5 + centerInset, height * 0.5 - centerInset),
                Point(width - cornerInset, 0.0),
                Point(width, cornerInset),
                Point(width * 0.5 + centerInset, height * 0.5 + centerInset),
                Point(width * 0.5 - centerInset, height * 0.5 - centerInset),
            ),
        )
    }

    private fun worldUnitSize(bitmap: korlibs.image.bitmap.Bitmap32): WorldUnitSize {
        val worldWidth = UNIT_ICON_WIDTH_METRES / mapMetresPerPixel
        val worldScale = worldWidth / bitmap.width
        return WorldUnitSize(
            worldWidth = worldWidth,
            worldHeight = bitmap.height * worldScale,
            worldScale = worldScale,
        )
    }

    private data class WorldUnitSize(
        val worldWidth: Double,
        val worldHeight: Double,
        val worldScale: Double,
    )

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private companion object {
        const val UNIT_ICON_WIDTH_METRES = 250.0
        const val DRAG_PREVIEW_Z_INDEX = 15_000.0
        private const val FACING_RING_SEGMENTS = 48
        private const val HIT_TOLERANCE = 8.0
    }
}

internal object UnitEditPositionResolver {
    fun resolve(
        currentCoordinate: MapCoordinate,
        initialPositionX: String,
        initialPositionY: String,
        newPositionX: String,
        newPositionY: String,
        parse: (String, String) -> MapCoordinate?,
    ): MapCoordinate? =
        if (newPositionX.trim() == initialPositionX.trim() && newPositionY.trim() == initialPositionY.trim()) {
            currentCoordinate
        } else {
            parse(newPositionX, newPositionY)
        }
}

internal data class UnitFacingParseResult(
    val degrees: Int?,
    val isValid: Boolean,
)

internal object UnitFacingResolver {
    fun format(facingDegrees: Int?): String =
        facingDegrees?.toString() ?: ""

    fun parse(input: String): UnitFacingParseResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return UnitFacingParseResult(degrees = null, isValid = true)
        }

        return trimmed.toIntOrNull()
            ?.let { UnitFacingParseResult(degrees = normalize(it), isValid = true) }
            ?: UnitFacingParseResult(degrees = null, isValid = false)
    }

    private fun normalize(degrees: Int): Int =
        ((degrees % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES

    private const val FULL_CIRCLE_DEGREES = 360
}
