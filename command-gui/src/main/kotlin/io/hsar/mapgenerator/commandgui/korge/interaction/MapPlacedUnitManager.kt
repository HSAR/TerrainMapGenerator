package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlags
import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.DestroyedUnitsDisplay
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogPositionSync
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerGunrepDialog
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerImageViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerSitrepDialog
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTacticalDialog
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiStateStyle
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUnitEditDialog
import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import io.hsar.mapgenerator.commandgui.korge.ui.focusTargetBounds
import io.hsar.mapgenerator.commandgui.save.TacticalFireKey
import io.hsar.mapgenerator.commandgui.save.TacticalFireUsage
import io.hsar.mapgenerator.commandgui.save.TacticalTurnMarker
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.image.Palette
import korlibs.event.Key
import korlibs.event.MouseButton
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.GraphicsRenderer
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.math.geom.Angle
import korlibs.math.geom.Point
import korlibs.math.geom.Vector2D
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(KorgeInternal::class)
internal class MapPlacedUnitManager(
    private val root: Container,
    private val unitLayer: Container,
    private val geometry: ViewerGeometry,
    private val mapUnitsPerPixel: Double,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val gridLayer: GridLayer? = null,
    private val viewportProvider: () -> CameraViewport,
    private val contextMenuManager: ContextMenuManager,
    private val deferredActionRunner: ContextMenuDeferredActionRunner,
    private val dialogManager: MapViewerDialogManager,
    private val onTransientNotice: (String) -> Unit = {},
    initialDestroyedUnitsDisplay: DestroyedUnitsDisplay = DestroyedUnitsDisplay.Full,
    initialUnitIconSizePx: Double = UnitIconDisplaySize.DEFAULT_PX,
    initialFireArcDisplayMode: FireArcDisplayMode = FireArcDisplayMode.AllUnits,
    private val canInteractWithUnits: () -> Boolean = { true },
    private val isHigherPriorityDragInProgress: () -> Boolean = { false },
    private val noteReferenceMenuActionsProvider: (MapPlacedObjectSnapshot) -> List<MapPlacedObjectMenuAction> = { emptyList() },
    private val onMovePreviewChanged: (MapObjectMovePreview?) -> Unit = {},
    private val onUnitDroppedIntoNoteReferenceZone: (Vector2D, MapPlacedObjectId) -> Boolean = { _, _ -> false },
    private val onObjectMoved: (MapPlacedObjectSnapshot, MapPlacedObjectSnapshot) -> Unit = { _, _ -> },
    private val onObjectDeleted: (MapPlacedObjectSnapshot) -> Unit = {},
    private val onUnitEdited: (UnitSnapshot, UnitSnapshot) -> Unit = { _, _ -> },
    private val onUnitsChanged: () -> Unit = {},
    private val turnOriginProvider: (MapPlacedObjectId) -> MapCoordinate? = { null },
    private val onTurnOriginReset: (MapPlacedObjectId, MapCoordinate) -> Unit = { _, _ -> },
    private val sitrepTurnNumberProvider: () -> Int = { 1 },
    private val sitrepContactStateProvider: (Int, MapPlacedObjectId) -> SitrepContactState = { _, _ -> SitrepContactState() },
    private val sitrepPreviousContactStateProvider: (Int, MapPlacedObjectId) -> SitrepContactState = { _, _ -> SitrepContactState() },
    private val onSitrepContactStateChanged: (Int, MapPlacedObjectId, SitrepContactState) -> Unit = { _, _, _ -> },
    private val tacticalTurnNumberProvider: () -> Int = { 1 },
    private val tacticalFireUsageProvider: (TacticalFireKey) -> TacticalFireUsage = { TacticalFireUsage() },
    private val onTacticalFireUsageChanged: (TacticalFireKey, TacticalFireUsage) -> Unit = { _, _ -> },
    private val ecmActivationsProvider: (Int) -> List<EcmActivation> = { emptyList() },
    private val onEcmActivation: (EcmActivation) -> Unit = {},
) {
    private val coordinateSpace = MapCoordinateSpace(geometry, mapWidth, mapHeight)
    private val units = mutableListOf<PlacedUnit>()
    private var unitQueryService = UnitQueryService(emptyList(), mapWidth, mapHeight, mapUnitsPerPixel)
    private val ecmLayer = unitLayer.container()
    private val activeEcmLayer = ecmLayer.container()
    private val previewEcmLayer = ecmLayer.container()
    private val dragPreviewLayer = unitLayer.container()
    private var renderedEcmSignature: Int? = null
    private var renderedEcmPreviewSignature: Int? = null
    private var selectedUnitIds = emptySet<MapPlacedObjectId>()
    private var destroyedUnitsDisplay = initialDestroyedUnitsDisplay
    private var unitIconSizePx = UnitIconDisplaySize.coerce(initialUnitIconSizePx)
    private var fireArcDisplayMode = initialFireArcDisplayMode
    private var lastAppliedViewportZoom: Double? = null
    private var dragState: DragState? = null
    private var ecmPreview: EcmPreview? = null
    private var ecmDragHandle: EcmHandle? = null
    private val editSessions = mutableMapOf<MapPlacedObjectId, UnitEditSession>()
    private val tacticalDialogs = mutableMapOf<MapPlacedObjectId, MapViewerTacticalDialog>()
    private val tacticalSelectedTargetIds = mutableMapOf<MapPlacedObjectId, MapPlacedObjectId>()
    private val openUnitEditDialogs = mutableMapOf<MapPlacedObjectId, MapViewerUnitEditDialog>()
    private var fullVisualRebuildCount = 0
    private var facingGeometryRebuildCount = 0

    fun install() {
        dragPreviewLayer.zIndex = DRAG_PREVIEW_Z_INDEX
        dragPreviewLayer.mouseEnabled = false

        root.addUpdater {
            val input = root.stage?.views?.input ?: return@addUpdater
            refreshIconDisplay()
            refreshEcmFields()
            updateEcmDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT))
            updateDrag(input.mousePos, input.mouseButtonPressed(MouseButton.LEFT), input.keys[Key.SHIFT])
        }
    }

    fun refreshEcmFields() {
        val activations = ecmActivationsProvider(tacticalTurnNumberProvider())
        val activeSignature = (activations.map { activation ->
            activation to units.firstOrNull { it.id == activation.emitterId }
                ?.let { listOf(it.mapCoordinate, it.worldWidth, it.worldHeight) }
        } + viewportProvider().zoom).hashCode()
        if (activeSignature != renderedEcmSignature) {
            renderedEcmSignature = activeSignature
            activeEcmLayer.removeChildren()
            activations.forEach { activation ->
                units.firstOrNull { it.id == activation.emitterId }
                    ?.let { emitter -> drawEcmField(activeEcmLayer, activation, emitter) }
            }
        }

        val previewEmitterGeometry = ecmPreview?.emitterId?.let { emitterId ->
            units.firstOrNull { it.id == emitterId }?.let { listOf(it.mapCoordinate, it.worldWidth, it.worldHeight) }
        }
        val previewSignature = listOf(ecmPreview, previewEmitterGeometry, viewportProvider().zoom).hashCode()
        if (previewSignature != renderedEcmPreviewSignature) {
            renderedEcmPreviewSignature = previewSignature
            previewEcmLayer.removeChildren()
            ecmPreview?.let { preview ->
                units.firstOrNull { it.id == preview.emitterId }?.let { emitter ->
                    drawEcmField(previewEcmLayer, preview.activation(tacticalTurnNumberProvider()), emitter, preview = true)
                    drawEcmHandles(previewEcmLayer, preview, emitter.snapshot())
                }
            }
        }
    }

    val isDragging: Boolean
        get() = dragState != null || ecmDragHandle != null

    val isContextMenuOpen: Boolean
        get() = contextMenuManager.isOpen

    val isEditDialogOpen: Boolean
        get() = dialogManager.hasOpenDialogs()

    fun updateSelectedUnits(selectedIds: Set<MapPlacedObjectId>) {
        if (selectedUnitIds == selectedIds) return

        val previousIds = selectedUnitIds
        val previousAffiliations = selectionAffiliations(previousIds)
        selectedUnitIds = selectedIds
        val affectedUnits = when (fireArcDisplayMode) {
            FireArcDisplayMode.FriendliesOfSelected,
            FireArcDisplayMode.HostilesOfSelected,
                -> if (previousAffiliations != selectionAffiliations(selectedIds)) units else units.filter { it.id in previousIds || it.id in selectedIds }

            else -> units.filter { it.id in previousIds || it.id in selectedIds }
        }
        affectedUnits.forEach { it.updateFacingOverlay() }
    }

    fun updateDestroyedUnitsDisplay(display: DestroyedUnitsDisplay) {
        destroyedUnitsDisplay = display
        units.forEach { it.applyDestroyedUnitsDisplay() }
        if (display != DestroyedUnitsDisplay.Full) {
            cancelTransientState()
        }
    }

    fun updateUnitIconSize(sizePx: Double) {
        val coerced = UnitIconDisplaySize.coerce(sizePx)
        if (coerced == unitIconSizePx) {
            return
        }

        unitIconSizePx = coerced
        refreshIconDisplay(force = true)
    }

    fun updateFireArcDisplayMode(mode: FireArcDisplayMode) {
        if (mode == fireArcDisplayMode) return
        fireArcDisplayMode = mode
        units.forEach { it.updateFacingOverlay() }
    }

    fun refreshIconDisplay(force: Boolean = false) {
        val viewportZoom = viewportProvider().zoom
        if (!force && lastAppliedViewportZoom == viewportZoom) {
            return
        }

        lastAppliedViewportZoom = viewportZoom
        units.forEach { unit ->
            val unitSize = worldUnitSize(unit.template.bitmap, viewportZoom)
            val scaleChanged = unit.view.scaleX != unitSize.worldScale
            unit.worldWidth = unitSize.worldWidth
            unit.worldHeight = unitSize.worldHeight
            unit.normalizedWidth = unitSize.normalizedWidth
            unit.normalizedHeight = unitSize.normalizedHeight
            unit.view.scale = unitSize.worldScale
            positionView(unit)
            if (scaleChanged) unit.updateFacingOverlay()
        }
        dragState?.let { state ->
            val unitSize = worldUnitSize(state.unit.template.bitmap, viewportZoom)
            state.preview.container.scale = unitSize.worldScale
            state.preview.container.x = state.unit.view.x
            state.preview.container.y = state.unit.view.y
        }
        onUnitsChanged()
    }

    fun replaceUnitTemplate(snapshot: UnitTemplateSnapshot) {
        val affectedUnits = units.filter { it.unitTemplateId == snapshot.id }
        val affiliationChanged = affectedUnits.any { it.template.affiliation != snapshot.template.affiliation }
        affectedUnits.forEach { unit ->
                if (OlympiadDawnShip.DAMAGE_KEY !in unit.customProperties) {
                    unit.customProperties += OlympiadDawnShip.instanceProperties(snapshot.template)
                }
                unit.updateTemplate(snapshot.template)
            }
        if (affiliationChanged && fireArcDisplayMode in setOf(FireArcDisplayMode.FriendliesOfSelected, FireArcDisplayMode.HostilesOfSelected)) {
            units.forEach { it.updateFacingOverlay() }
        }
        notifyUnitStateChanged()
    }

    private fun interactionsEnabled(): Boolean =
        canInteractWithUnits()

    fun blocksScreenPoint(point: Vector2D): Boolean =
        dialogManager.blocksScreenPoint(point)

    fun hasDragCandidate(point: Vector2D): Boolean {
        if (!interactionsEnabled()) {
            return false
        }

        if (hasEcmHandleAt(point)) return true
        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(point, viewport)
        return units.any { unit -> unit.canMove() && unit.containsMapCoordinate(mapCoordinate) }
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
            .filter { it.canMove() }
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

        if (beginEcmHandleDragAt(screenPoint)) return true
        val viewport = viewportProvider()
        val mapCoordinate = coordinateSpace.screenToMap(screenPoint, viewport)
        val tolerance = coordinateSpace.screenToleranceToMapTolerance(HIT_TOLERANCE, viewport)
        val target = units
            .asSequence()
            .filter { it.canMove() }
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
            .filter { it.canOpenContextMenu() }
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
        customProperties: Map<String, String> = OlympiadDawnShip.instanceProperties(template),
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
            normalizedWidth = unitSize.normalizedWidth,
            normalizedHeight = unitSize.normalizedHeight,
            customProperties = customProperties,
        )
    }

    fun upsert(snapshot: UnitSnapshot) = upsert(snapshot, notify = true)

    fun preview(snapshot: UnitSnapshot) = upsert(snapshot, notify = false, syncDialog = false)

    internal fun visualUpdateStats(): UnitVisualUpdateStats =
        UnitVisualUpdateStats(fullVisualRebuildCount, facingGeometryRebuildCount)

    internal fun resetVisualUpdateStats() {
        fullVisualRebuildCount = 0
        facingGeometryRebuildCount = 0
    }

    fun restoreAll(snapshots: List<UnitSnapshot>) {
        units.forEach { it.view.removeFromParent() }
        units.clear()
        snapshots.forEach { upsert(it, notify = false) }
        notifyUnitStateChanged()
    }

    private fun upsert(inputSnapshot: UnitSnapshot, notify: Boolean, syncDialog: Boolean = notify) {
        val snapshot = normalizeFighterLink(inputSnapshot)
        val unitSize = worldUnitSize(snapshot.template.bitmap)
        val existing = units.firstOrNull { it.id == snapshot.id }
        if (existing != null) {
            val visualChanged = existing.unitTemplateId != snapshot.unitTemplateId || existing.template != snapshot.template
            val facingChanged = existing.facingDegrees != snapshot.facingDegrees
            val destroyedChanged = existing.destroyed != snapshot.destroyed
            val positionChanged = existing.mapCoordinate != snapshot.mapCoordinate
            val shipDefinitionChanged = existing.customProperties[OlympiadDawnShip.DEFINITION_KEY] !=
                    snapshot.customProperties[OlympiadDawnShip.DEFINITION_KEY]
            val sizeChanged = existing.worldWidth != unitSize.worldWidth ||
                    existing.worldHeight != unitSize.worldHeight ||
                    existing.view.scaleX != unitSize.worldScale
            existing.unitTemplateId = snapshot.unitTemplateId
            existing.template = snapshot.template
            existing.instanceName = snapshot.instanceName
            existing.destroyed = snapshot.destroyed
            existing.facingDegrees = snapshot.facingDegrees
            existing.mapCoordinate = snapshot.mapCoordinate
            existing.normalizedWidth = unitSize.normalizedWidth
            existing.normalizedHeight = unitSize.normalizedHeight
            existing.customProperties = snapshot.customProperties
            existing.worldWidth = unitSize.worldWidth
            existing.worldHeight = unitSize.worldHeight
            existing.view.scale = unitSize.worldScale
            when {
                visualChanged -> existing.rebuildVisual(snapshot.template.bitmap)
                else -> {
                    if (facingChanged || sizeChanged || shipDefinitionChanged) existing.updateFacingOverlay()
                    if (destroyedChanged) existing.updateStateOverlay()
                }
            }
            existing.applyDestroyedUnitsDisplay()
            if (positionChanged || sizeChanged) positionView(existing)
            if (syncDialog) syncEditDialog(existing)
            if (notify) notifyUnitStateChanged()
            return
        }

        val placedUnit = PlacedUnit(
            id = snapshot.id,
            view = unitLayer.container(),
            unitTemplateId = snapshot.unitTemplateId,
            template = snapshot.template,
            instanceName = snapshot.instanceName,
            destroyed = snapshot.destroyed,
            facingDegrees = snapshot.facingDegrees,
            mapCoordinate = snapshot.mapCoordinate,
            normalizedWidth = unitSize.normalizedWidth,
            normalizedHeight = unitSize.normalizedHeight,
            customProperties = snapshot.customProperties,
            worldWidth = unitSize.worldWidth,
            worldHeight = unitSize.worldHeight,
        )
        placedUnit.view.scale = unitSize.worldScale
        placedUnit.applyDestroyedUnitsDisplay()
        positionView(placedUnit)
        units += placedUnit
        if (notify) notifyUnitStateChanged()
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
                    editSessions.remove(removed.id)
                    openUnitEditDialogs.remove(removed.id)
                    dialogManager.closeTarget(removed.id)
                    dialogManager.closeTarget(TacticalDialogKey(removed.id))
                    notifyUnitStateChanged()
                }.snapshot()
            }

    fun snapshot(id: MapPlacedObjectId): UnitSnapshot? =
        units.firstOrNull { it.id == id }?.snapshot()

    fun snapshots(): List<UnitSnapshot> =
        units.map { it.snapshot() }

    fun restoreSitrepDialog(dialog: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog): Boolean =
        dialog.targetId
            ?.let { value -> runCatching { MapPlacedObjectId(java.util.UUID.fromString(value)) }.getOrNull() }
            ?.let { id -> units.firstOrNull { it.id == id } }
            ?.let { unit -> unit.openNavrepVisibilityDialog(unit, dialog); true }
            ?: false

    fun restoreUnitEditDialog(dialog: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog): Boolean =
        dialog.targetId
            ?.let { value -> runCatching { MapPlacedObjectId(java.util.UUID.fromString(value)) }.getOrNull() }
            ?.let { id -> units.firstOrNull { it.id == id } }
            ?.let { unit -> openEditDialog(unit, dialog); true }
            ?: false

    fun restoreTacticalDialog(dialog: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog): Boolean =
        dialog.targetId
            ?.let { value -> runCatching { MapPlacedObjectId(java.util.UUID.fromString(value)) }.getOrNull() }
            ?.let { id -> units.firstOrNull { it.id == id } }
            ?.let { unit -> unit.openTacticalDialog(unit, dialog); true }
            ?: false

    fun consumeTacticalSolution(unitId: MapPlacedObjectId, targetId: MapPlacedObjectId): Boolean =
        units.firstOrNull { it.id == unitId }?.fireTacticalSolution(unitId, targetId) != null

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
        if (targetId == null || ecmPreview?.emitterId == targetId) {
            ecmPreview = null
            ecmDragHandle = null
            renderedEcmPreviewSignature = null
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
            facingColor = unit.template.affiliation.overlayColor(),
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
                color = unit.snapshot().measureTextColor(),
            ),
        )
    }

    private fun updateDrag(pointer: Vector2D, leftHeld: Boolean, shiftHeld: Boolean) {
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
            if (onUnitDroppedIntoNoteReferenceZone(pointer, state.unit.id)) {
                upsert(state.originalSnapshot)
                state.preview.container.removeFromParent()
                state.unit.view.visible = true
                onMovePreviewChanged(null)
                dragState = null
                return
            }
            val updatedSnapshot = state.unit.snapshot()
            if (updatedSnapshot != state.originalSnapshot) {
                onObjectMoved(state.originalSnapshot, updatedSnapshot)
                syncEditDialog(state.unit)
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
        val constrainedCoordinate = constrainMove(state.unit, mapCoordinate, overrideMoveLimit = shiftHeld)
        val worldPoint = coordinateSpace.toWorld(constrainedCoordinate)

        state.unit.mapCoordinate = constrainedCoordinate
        positionView(state.unit)
        notifyUnitStateChanged()
        state.preview.container.x = worldPoint.x - state.unit.worldWidth * 0.5
        state.preview.container.y = worldPoint.y - state.unit.worldHeight * 0.5
        onMovePreviewChanged(
            MapObjectMovePreview(
                origin = state.originalSnapshot.mapCoordinate,
                destination = constrainedCoordinate,
                color = state.unit.snapshot().measureTextColor(),
            ),
        )
    }

    private fun openContextMenu(unit: PlacedUnit, screenPoint: Vector2D) {
        if (!interactionsEnabled()) {
            return
        }
        contextMenuManager.open(
            target = unit,
            screenPoint = screenPoint,
            actions = unit.contextMenuActions() + noteReferenceMenuActionsProvider(unit.snapshot()),
        )
    }

    private fun openEditDialog(
        unit: PlacedUnit,
        restored: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog? = null,
    ) {
        if (dialogManager.hasTarget(unit.id)) {
            return
        }
        val viewport = viewportProvider()
        val position = initialReferenceFields(unit)
        val focusTargetBoundsProvider = { listOf(unit.snapshot().focusTargetBounds(coordinateSpace, viewportProvider())) }
        val session = UnitEditSession(
            baselineSnapshot = unit.snapshot(),
            baselinePosition = position,
        )
        editSessions[unit.id] = session
        val viewportWidth = viewport.viewportWidth
        val viewportHeight = viewport.viewportHeight
        var saveRequested = false
        onMovePreviewChanged(null)
        MapViewerUnitEditDialog(
            root = root,
            dialogManager = dialogManager,
            dialogKey = unit.id,
            persistenceTargetId = unit.id.value.toString(),
            unitType = unit.template,
            initialName = unit.instanceName,
            initialPositionX = position.easting,
            initialPositionY = position.northing,
            initialGridSquare = position.easting.takeIf { gridLayer?.referenceSystem == io.hsar.mapgenerator.grid.ReferenceSystem.EPSILON },
            initialDestroyed = unit.destroyed,
            initialFacing = UnitFacingResolver.format(unit.facingDegrees),
            initialAffiliation = unit.template.affiliation,
            initialCustomProperties = unit.customProperties,
            fighterLaunchStatusesProvider = {
                OlympiadDawnShip.fighterLaunchStatuses(
                    carrier = unit.snapshot(),
                    units = units.map { it.snapshot() },
                )
            },
            squadronStatusProvider = {
                OlympiadDawnShip.squadronStatus(
                    fighter = unit.snapshot(),
                    units = units.map { it.snapshot() },
                )
            },
            onSquadronRemainingChanged = { remaining ->
                updateLinkedSquadronHealth(unit, remaining)
            },
            onShipDamageChanged = { damage ->
                updateShipDamage(unit, damage)
            },
            onPositionPreviewChanged = { previewPositionX, previewPositionY ->
                val updatedCoordinate = UnitEditPositionResolver.resolve(
                    currentCoordinate = session.baselineSnapshot.mapCoordinate,
                    initialPositionX = session.baselinePosition.easting,
                    initialPositionY = session.baselinePosition.northing,
                    newPositionX = previewPositionX,
                    newPositionY = previewPositionY,
                ) { easting, northing -> referenceToMapCoordinate(easting, northing, viewportWidth, viewportHeight) }
                if (updatedCoordinate != null) {
                    val constrainedCoordinate = constrainMove(unit, updatedCoordinate)
                    unit.mapCoordinate = constrainedCoordinate
                    positionView(unit)
                    notifyUnitStateChanged()
                    if (constrainedCoordinate != updatedCoordinate) syncEditDialogPosition(unit)
                    onMovePreviewChanged(
                        MapObjectMovePreview(
                            origin = session.baselineSnapshot.mapCoordinate,
                            destination = constrainedCoordinate,
                            referenceText = "${previewPositionX.trim()} ${previewPositionY.trim()}",
                            color = unit.snapshot().measureTextColor(),
                        ),
                    )
                } else {
                    upsert(session.baselineSnapshot)
                    onMovePreviewChanged(null)
                }
            },
            onDestroyedPreviewChanged = { destroyed ->
                unit.destroyed = destroyed
                unit.updateStateOverlay()
                refreshOpenShipStatusPanels()
            },
            onFacingPreviewChanged = { facingText ->
                UnitFacingResolver.parse(facingText)
                    .takeIf { it.isValid }
                    ?.let { facing ->
                        unit.facingDegrees = facing.degrees
                        unit.updateFacingOverlay(forceActive = true)
                    }
                    ?: run {
                        unit.facingDegrees = session.baselineSnapshot.facingDegrees
                        unit.updateFacingOverlay(forceActive = true)
                    }
            },
            onAffiliationPreviewChanged = { affiliation ->
                unit.template = unit.template.copy(affiliation = affiliation)
                unit.updateStateOverlay()
            },
            focusTargetBoundsProvider = focusTargetBoundsProvider,
            isPositionValid = { positionX, positionY ->
                UnitEditPositionResolver.resolve(
                    currentCoordinate = session.baselineSnapshot.mapCoordinate,
                    initialPositionX = session.baselinePosition.easting,
                    initialPositionY = session.baselinePosition.northing,
                    newPositionX = positionX,
                    newPositionY = positionY,
                ) { easting, northing -> referenceToMapCoordinate(easting, northing, viewportWidth, viewportHeight) } != null
            },
            isFacingValid = { facingText ->
                UnitFacingResolver.parse(facingText).isValid
            },
            onSave = save@{ newName, newPositionX, newPositionY, newDestroyed, newFacingText, newAffiliation, customProperties ->
                val nameChanged = newName != session.baselineSnapshot.instanceName
                val updatedCoordinate = UnitEditPositionResolver.resolve(
                    currentCoordinate = session.baselineSnapshot.mapCoordinate,
                    initialPositionX = session.baselinePosition.easting,
                    initialPositionY = session.baselinePosition.northing,
                    newPositionX = newPositionX,
                    newPositionY = newPositionY,
                ) { easting, northing ->
                    referenceToMapCoordinate(easting, northing, viewportWidth, viewportHeight)
                } ?: return@save false
                val constrainedCoordinate = constrainMove(unit, updatedCoordinate)
                val positionChanged = constrainedCoordinate != session.baselineSnapshot.mapCoordinate
                val parsedFacing = UnitFacingResolver.parseValid(newFacingText) ?: return@save false
                val updatedFacing = parsedFacing.degrees
                val destroyedChanged = newDestroyed != session.baselineSnapshot.destroyed
                val facingChanged = updatedFacing != session.baselineSnapshot.facingDegrees
                val affiliationChanged = newAffiliation != session.baselineSnapshot.template.affiliation
                val prospectiveSnapshot = unit.snapshot().copy(
                    destroyed = newDestroyed,
                    customProperties = customProperties,
                )
                val prospectiveUnits = units.map { existing ->
                    if (existing.id == unit.id) prospectiveSnapshot else existing.snapshot()
                }
                val savedCustomProperties = OlympiadDawnShip.validatedFighterProperties(prospectiveSnapshot, prospectiveUnits)
                val invalidFighterLink = savedCustomProperties != customProperties
                val customPropertiesChanged = savedCustomProperties != session.baselineSnapshot.customProperties
                if (nameChanged || positionChanged || destroyedChanged || facingChanged || affiliationChanged || customPropertiesChanged) {
                    saveRequested = true
                    unit.instanceName = newName
                    unit.mapCoordinate = constrainedCoordinate
                    unit.destroyed = newDestroyed
                    unit.facingDegrees = updatedFacing
                    unit.template = unit.template.copy(affiliation = newAffiliation)
                    unit.customProperties = savedCustomProperties
                    unit.updateStateOverlay()
                    unit.updateFacingOverlay()
                    positionView(unit)
                    notifyUnitStateChanged()
                    onMovePreviewChanged(null)
                    onUnitEdited(session.baselineSnapshot, unit.snapshot())
                }
                if (invalidFighterLink) {
                    onTransientNotice("Fighter unitId could not be linked to exactly one carrier squadron and was cleared.")
                }
                true
            },
            onDismiss = {
                val activeSession = editSessions.remove(unit.id)
                if (!saveRequested && activeSession != null) {
                    upsert(session.baselineSnapshot)
                }
                openUnitEditDialogs.remove(unit.id)
                onMovePreviewChanged(null)
            },
            restored = restored,
        ).also { dialog ->
            openUnitEditDialogs[unit.id] = dialog
            dialog.show(viewportWidth, viewportHeight)
        }
    }

    private fun normalizeFighterLink(snapshot: UnitSnapshot): UnitSnapshot {
        if (snapshot.unitClassLabel() != "F") return snapshot
        val requested = snapshot.customProperties[OlympiadDawnShip.FIGHTER_UNIT_ID_KEY]?.trim()?.takeIf(String::isNotEmpty) ?: return snapshot
        val matches = units.map { it.snapshot() }.mapNotNull { carrier ->
            OlympiadDawnShip.definition(carrier.template.customProperties)
                ?.fighters
                ?.indexOfFirst { requested == it.id }
                ?.takeIf { it >= 0 }
                ?.let { index -> OlympiadDawnShip.definition(carrier.customProperties)?.fighters?.getOrNull(index)?.id }
        }.distinct()
        val replacement = matches.singleOrNull() ?: return snapshot
        return snapshot.copy(customProperties = snapshot.customProperties + (OlympiadDawnShip.FIGHTER_UNIT_ID_KEY to replacement))
    }

    private fun notifyUnitStateChanged() {
        unitQueryService = UnitQueryService(units.map { it.snapshot() }, mapWidth, mapHeight, mapUnitsPerPixel)
        refreshOpenShipStatusPanels()
        refreshTacticalDialogs()
        renderedEcmSignature = null
        renderedEcmPreviewSignature = null
        refreshEcmFields()
        onUnitsChanged()
    }

    private fun refreshOpenShipStatusPanels() =
        openUnitEditDialogs.values.forEach(MapViewerUnitEditDialog::refreshShipStatus)

    private fun refreshTacticalDialogs() =
        tacticalDialogs.forEach { (unitId, dialog) ->
            units.firstOrNull { it.id == unitId }
                ?.snapshot()
                ?.let { snapshot -> dialog.refreshUnitName(snapshot.displayName()) }
            dialog.refresh()
        }

    private fun updateLinkedSquadronHealth(fighter: PlacedUnit, remaining: Int) {
        val status = OlympiadDawnShip.squadronStatus(fighter.snapshot(), units.map { it.snapshot() }) ?: return
        val carrier = units.firstOrNull { it.id == status.carrierId } ?: return
        OlympiadDawnShip.updatedSquadronProperties(carrier.snapshot(), status.squadronId, remaining)
            ?.let { properties -> updateShipDamage(carrier, OlympiadDawnShip.shipDamage(carrier.snapshot().copy(customProperties = properties)) ?: return) }
    }

    private fun updateShipDamage(unit: PlacedUnit, damage: OlympiadDawnShipDamage) {
        val before = unit.snapshot()
        val customProperties = before.customProperties + (OlympiadDawnShip.DAMAGE_KEY to OlympiadDawnShip.encodeDamage(damage))
        if (customProperties == before.customProperties) return
        unit.customProperties = customProperties
        openUnitEditDialogs[unit.id]?.syncShipDamage(damage)
        syncEditDialog(unit)
        notifyUnitStateChanged()
        onUnitEdited(before, unit.snapshot())
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
        var customProperties: Map<String, String>,
        var worldWidth: Double,
        var worldHeight: Double,
    ) : MapPlacedObject {
        private var facingLayer: Container = view.container().also { it.zIndex = 0.0 }
        var iconView: Image = MapViewerImageViews.icon(view, template.bitmap).also { it.zIndex = 1.0 }
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
                customProperties = customProperties,
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
            buildList {
                val snapshot = snapshot()
                add(
                    MapPlacedObjectMenuAction(
                        label = "Edit Unit",
                        onSelected = { deferredActionRunner.launch { openEditDialog(this@PlacedUnit) } },
                    ),
                )
                add(
                    MapPlacedObjectMenuAction(
                        label = "Delete Unit",
                        onSelected = { onObjectDeleted(snapshot) },
                    ),
                )
                add(sitrepMenuAction())
                tacticalMenuAction()?.let(::add)
            }

        private fun tacticalMenuAction(): MapPlacedObjectMenuAction? =
            (shipDefinition()?.let { definition ->
                definition.weapons.any { it.arcs.isNotEmpty() } || definition.fighters.any { fighter -> fighter.weapons.any { it.arcs.isNotEmpty() } }
            } ?: OlympiadDawnShip.squadronStatus(snapshot(), units.map { it.snapshot() })?.weapons?.any { it.arcs.isNotEmpty() })
                .takeIf { it == true }
                ?.let {
                    MapPlacedObjectMenuAction(
                        label = "TACTICAL",
                        onSelected = { deferredActionRunner.launch { openTacticalDialog(this@PlacedUnit) } },
                    ).withRequiredFeatureFlags(MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)
                }

        private fun shipDefinition(): OlympiadDawnShipDefinition? =
            OlympiadDawnShip.definition(customProperties) ?: OlympiadDawnShip.definition(template.customProperties)

        private fun sitrepMenuAction(): MapPlacedObjectMenuAction {
            val validationError = snapshot().sitrepValidationError()
            return MapPlacedObjectMenuAction(
                label = "SITREP",
                onSelected = { deferredActionRunner.launch { openNavrepVisibilityDialog(this@PlacedUnit) } },
                enabled = validationError == null,
                disabledTooltipText = validationError,
            ).withRequiredFeatureFlags(io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL)
        }

        fun openNavrepVisibilityDialog(
            unit: PlacedUnit,
            restored: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog? = null,
        ) {
            val snapshot = unit.snapshot()
            val turnNumber = sitrepTurnNumberProvider()
            MapViewerSitrepDialog(
                root = root,
                dialogManager = dialogManager,
                dialogKey = "navrep-report-${unit.id.value}",
                unitName = snapshot.displayName(),
                observer = snapshot,
                unitQueryService = unitQueryService,
                gridLayer = gridLayer,
                initialContactState = sitrepContactStateProvider(turnNumber, snapshot.id),
                previousContactState = sitrepPreviousContactStateProvider(turnNumber, snapshot.id),
                onContactStateChanged = { state ->
                    onSitrepContactStateChanged(turnNumber, snapshot.id, state)
                },
                onObserverCustomPropertiesChanged = { before, after ->
                    unit.customProperties = after.customProperties
                    unit.updateStateOverlay()
                    notifyUnitStateChanged()
                    onUnitEdited(before, after)
                    dialogManager.broadcast(after)
                },
                ecmIntensityProvider = ::ecmIntensityAt,
                onTransientNotice = onTransientNotice,
                restored = restored,
            ).open()
        }

        fun openTacticalDialog(
            unit: PlacedUnit,
            restored: io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog? = null,
        ) {
            val key = TacticalDialogKey(unit.id)
            if (dialogManager.hasTarget(key)) return
            val dialog = MapViewerTacticalDialog(
                root = root,
                dialogManager = dialogManager,
                dialogKey = key,
                unitName = unit.snapshot().displayName(),
                reportProvider = {
                    units.firstOrNull { it.id == unit.id }
                        ?.snapshot()
                        ?.let { source ->
                            val turnNumber = tacticalTurnNumberProvider()
                            val usage = TacticalTurnMarker.usage(source.customProperties, turnNumber)
                                ?: if (TacticalTurnMarker.hasMarker(source.customProperties)) TacticalFireUsage()
                                else tacticalFireUsageProvider(TacticalFireKey(turnNumber, source.id))
                            TacticalTargetingResolver.resolve(
                                source = source,
                                unitQueryService = unitQueryService,
                                firedWeaponKeys = usage.firedWeapons,
                                fireControlUsed = usage.fireControlUsed,
                            )
                        }
                },
                restored = restored,
                persistenceTargetId = unit.id.value.toString(),
                focusTargetBoundsProvider = { listOf(unit.snapshot().focusTargetBounds(coordinateSpace, viewportProvider())) },
                onDismiss = {
                    tacticalDialogs.remove(unit.id)
                    tacticalSelectedTargetIds.remove(unit.id)
                    unit.updateFacingOverlay()
                },
                onSolutionSelected = { targetId ->
                    if (targetId == null || targetId == unit.id) tacticalSelectedTargetIds.remove(unit.id)
                    else tacticalSelectedTargetIds[unit.id] = targetId
                    unit.updateFacingOverlay()
                },
                onSelectionChanged = { target, weaponKeys -> updateEcmPreview(unit, target, weaponKeys) },
                contributionArc = { target, contribution ->
                    ecmPreview
                        ?.takeIf { target.manualEcm && it.emitterId == unit.id && contribution.weaponKey in it.moduleKeys }
                        ?.let { TacticalTargetingResolver.arcForBearing(it.relativeBearingDegrees, unit.facingDegrees ?: 0) }
                        ?: contribution.arc
                },
                canFire = { target, weaponKeys -> canFireTacticalSelection(target, weaponKeys) },
                onFire = { targetId, weaponKeys -> fireTacticalSolution(unit.id, targetId, weaponKeys) },
                onFireResolved = { resolution ->
                    MapViewerGunrepDialog(root, dialogManager, "gunrep-${java.util.UUID.randomUUID()}", resolution).open()
                },
            )
            tacticalDialogs[unit.id] = dialog
            if (dialog.open()) {
                unit.updateFacingOverlay()
            } else {
                tacticalDialogs.remove(unit.id)
                tacticalSelectedTargetIds.remove(unit.id)
            }
        }

        fun fireTacticalSolution(
            unitId: MapPlacedObjectId,
            targetId: MapPlacedObjectId,
            selectedWeaponKeys: Set<String>? = null,
        ): TacticalFireResolution? {
            val unit = units.firstOrNull { it.id == unitId } ?: return null
            val usageKey = TacticalFireKey(tacticalTurnNumberProvider(), unitId)
            val usage = tacticalUsage(unit)
            val report = TacticalTargetingResolver.resolve(unit.snapshot(), unitQueryService, usage.firedWeapons, usage.fireControlUsed)
            val target = report.targets.firstOrNull { it.targetId == targetId } ?: return null
            val contributions = target.contributions.filter { contribution -> selectedWeaponKeys == null || contribution.weaponKey in selectedWeaponKeys }
            val weaponKeys = contributions.mapTo(linkedSetOf(), TacticalWeaponContribution::weaponKey)
            if (weaponKeys.isEmpty()) return null
            val selectedEcm = report.weapons.filter { it.ecm && it.key in weaponKeys }
            val selectedPreview = ecmPreview?.takeIf { it.emitterId == unit.id && it.moduleKeys == selectedEcm.mapTo(linkedSetOf(), TacticalWeaponStatus::key) }
            val ecmShape = selectedPreview?.shape ?: selectedEcm.takeIf(List<TacticalWeaponStatus>::isNotEmpty)?.let { modules ->
                if (target.manualEcm) EcmRules.manual(modules.size) else EcmRules.automatic(modules.size, target.rangeU)
            }
            if (selectedEcm.isNotEmpty() && ecmShape == null) return null
            val consumesFireControl = report.weapons.any {
                it.key in weaponKeys && it.requiresFireControl && !(resolutionTargetIsFighter(targetId) && it.beamClass == 1)
            }
            if (consumesFireControl && usage.fireControlUsed >= report.fireControlCapacity) return null
            val resolutionTarget = units.firstOrNull { it.id == targetId } ?: return null
            val definition = unit.shipDefinition()
            val consumables = definition?.let { sourceDefinition ->
                contributions.mapNotNull { contribution ->
                    sourceDefinition.weapons.firstOrNull { weapon ->
                        OlympiadDawnShip.weaponKey(sourceDefinition, sourceDefinition.weapons.indexOf(weapon)) == contribution.weaponKey
                    }
                }
            }.orEmpty()
            val updatedProperties = unit.customProperties.toMutableMap()
            if (consumables.isNotEmpty()) {
                consumables.forEach { weapon ->
                    val magazine = weapon.magazineId?.let { id -> definition?.systems?.firstOrNull { it.id == id } }
                    val capacityId = magazine?.id ?: weapon.takeIf { it.submunition }
                        ?.let { definition?.let { sourceDefinition -> OlympiadDawnShip.weaponKey(sourceDefinition, sourceDefinition.weapons.indexOf(it)) } }
                    val capacity = magazine?.capacity ?: weapon.takeIf { it.submunition }?.let { 1 } ?: return@forEach
                    val remaining = OlympiadDawnShip.capacityRemaining(updatedProperties, capacityId!!, capacity)
                    if (remaining <= 0) return null
                    updatedProperties[OlympiadDawnShip.magazinePropertyKey(capacityId)] = (remaining - 1).toString()
                }
            }
            val updatedUsage = usage.copy(
                firedWeapons = usage.firedWeapons + weaponKeys,
                fireControlUsed = usage.fireControlUsed + if (consumesFireControl) 1 else 0,
            )
            val activation = ecmShape?.let { shape ->
                val bearing = selectedPreview?.relativeBearingDegrees
                    ?: if (target.manualEcm) 0.0 else EcmRules.absoluteBearing(unit.snapshot(), resolutionTarget.snapshot().mapCoordinate, unitQueryService)
                EcmActivation(
                    tacticalTurnNumberProvider(), unit.id, selectedEcm.mapTo(linkedSetOf(), TacticalWeaponStatus::key),
                    bearing, shape.rangeU, shape.widthDegrees, shape.intensity,
                ).also(onEcmActivation)
            }
            val beforeFiring = unit.snapshot()
            unit.customProperties = TacticalTurnMarker.update(updatedProperties, usageKey.turnNumber, updatedUsage, activation)
            if (unit.customProperties != beforeFiring.customProperties) onUnitEdited(beforeFiring, unit.snapshot())
            onTacticalFireUsageChanged(usageKey, updatedUsage)
            if (target.manualEcm) {
                notifyUnitStateChanged()
                return TacticalFireResolution(
                    attackerName = unit.snapshot().displayName(),
                    targetName = "ECM Field",
                    weaponNames = selectedEcm.map(TacticalWeaponStatus::name),
                    diceRolled = 0,
                    damage = 0,
                    ecmActivations = listOfNotNull(activation),
                    effectiveEcmLevel = activation?.intensity ?: 0,
                )
            }
            pendingIntercept(unit, resolutionTarget, report.weapons, contributions, target.rangeU)?.let { pending ->
                openIntercept(pending)
                notifyUnitStateChanged()
                return TacticalFireResolution(
                    targetName = resolutionTarget.snapshot().displayName(),
                    weaponNames = contributions.map(TacticalWeaponContribution::weaponName),
                    diceRolled = 0,
                    damage = 0,
                    interceptPending = true
                )
            }
            val resolution = TacticalCombatResolver.resolve(
                resolutionTarget.snapshot(), report.weapons, contributions,
                ecmLevelsByWeaponKey = ecmLevelsFor(unit.snapshot(), resolutionTarget.snapshot(), report.weapons, contributions),
            )
            val outcome = applyTacticalDamage(resolutionTarget, resolution.damage)
            notifyUnitStateChanged()
            return resolution.copy(
                attackerName = unit.snapshot().displayName(),
                fightersDestroyed = outcome.fightersDestroyed,
                targetDestroyed = outcome.targetDestroyed,
                ecmActivations = listOfNotNull(activation),
            )
        }

        private fun applyTacticalDamage(target: PlacedUnit, hits: Int): TacticalDamageOutcome {
            if (hits <= 0) return TacticalDamageOutcome()
            val definition = target.shipDefinition()
            if (definition != null) {
                val updated = TacticalCombatResolver.applyShipDamage(
                    definition,
                    OlympiadDawnShip.instanceDamage(target.customProperties, definition),
                    hits,
                )
                updateShipDamage(target, updated)
                val destroyed = updated.destroyedHull.size == definition.hullPoints
                if (destroyed) markDestroyed(target)
                return TacticalDamageOutcome(targetDestroyed = destroyed)
            }
            val status = OlympiadDawnShip.squadronStatus(target.snapshot(), units.map { it.snapshot() }) ?: return TacticalDamageOutcome()
            val remaining = TacticalCombatResolver.remainingFightersAfterDamage(status.remaining, hits)
            updateLinkedSquadronHealth(target, remaining)
            val destroyed = remaining == 0
            if (destroyed) markDestroyed(target)
            return TacticalDamageOutcome(status.remaining - remaining, destroyed)
        }

        private fun resolutionTargetIsFighter(targetId: MapPlacedObjectId): Boolean =
            units.firstOrNull { it.id == targetId }?.snapshot()?.unitClassLabel() == "F"

        private fun tacticalUsage(unit: PlacedUnit): TacticalFireUsage {
            val turnNumber = tacticalTurnNumberProvider()
            return TacticalTurnMarker.usage(unit.customProperties, turnNumber)
                ?: TacticalTurnMarker.hasMarker(unit.customProperties).let { marked ->
                    if (marked) TacticalFireUsage() else tacticalFireUsageProvider(TacticalFireKey(turnNumber, unit.id))
                }
        }

        private fun pendingIntercept(
            attacker: PlacedUnit,
            defender: PlacedUnit,
            weapons: List<TacticalWeaponStatus>,
            incoming: List<TacticalWeaponContribution>,
            rangeU: Double
        ): PendingIntercept? {
            if (incoming.none { contribution ->
                    weapons.firstOrNull { it.key == contribution.weaponKey }?.let { it.salvo || it.salvoRack || it.torpedo || it.fighterWeapon } == true
                }) return null
            val key = TacticalFireKey(tacticalTurnNumberProvider(), defender.id)
            val defensiveWeapons = TacticalTargetingResolver.resolve(
                defender.snapshot(),
                unitQueryService,
                tacticalUsage(defender).firedWeapons,
                tacticalUsage(defender).fireControlUsed
            )
                .weapons.filter { !it.destroyed && !it.fired && (it.pointDefense || it.beamClass == 1 || it.ecm) }
            val defensiveContributions = defensiveWeapons.mapNotNull { weapon ->
                (if (weapon.ecm) 0 else if (weapon.pointDefense) 1 else OlympiadDawnBeamRules.firepowerAtRange(1, rangeU))
                    .takeIf { it > 0 }?.let { TacticalWeaponContribution(weapon.key, weapon.name, "F", it) }
                    ?: weapon.takeIf(TacticalWeaponStatus::ecm)?.let { TacticalWeaponContribution(it.key, it.name, "F", 0) }
            }
            return defensiveContributions.takeIf(List<TacticalWeaponContribution>::isNotEmpty)
                ?.let { PendingIntercept(attacker.id, defender.id, weapons, incoming, defensiveWeapons, it) }
        }

        private fun openIntercept(pending: PendingIntercept) {
            val defender = units.firstOrNull { it.id == pending.defenderId } ?: return
            MapViewerTacticalDialog(
                root = root,
                dialogManager = dialogManager,
                dialogKey = "intercept-" + defender.id.value,
                unitName = defender.snapshot().displayName(),
                reportProvider = {
                    TacticalTargetingReport(
                        pending.defensiveWeapons,
                        listOf(
                            TacticalTarget(
                                pending.attackerId,
                                "Incoming attack",
                                0.0,
                                listOf("F"),
                                pending.defensiveContributions.sumOf(TacticalWeaponContribution::firepower),
                                pending.defensiveContributions
                            )
                        ),
                    )
                },
                dialogName = "Intercept",
                fireActionLabel = "Intercept",
                persistenceTargetId = defender.id.value.toString(),
                onFire = { _, selected -> resolvePendingIntercept(pending, selected) },
                onFireResolved = { resolution ->
                    MapViewerGunrepDialog(
                        root,
                        dialogManager,
                        "gunrep-" + java.util.UUID.randomUUID(),
                        resolution,
                        onDismiss = { resolveIncomingAttack(pending)?.openGunrep() },
                    ).open()
                },
                onDismiss = {
                    if (!pending.defensiveFireResolved) resolveIncomingAttack(pending)?.openGunrep()
                },
            ).open()
        }

        private fun resolvePendingIntercept(pending: PendingIntercept, selected: Set<String>): TacticalFireResolution? {
            if (pending.defensiveFireResolved) return null
            pending.defensiveFireResolved = true
            val defender = units.firstOrNull { it.id == pending.defenderId } ?: return null
            val attacker = units.firstOrNull { it.id == pending.attackerId }?.snapshot() ?: return null
            val chosen = pending.defensiveContributions.filter { it.weaponKey in selected }
            val chosenEcm = pending.defensiveWeapons.filter { it.ecm && it.key in selected }
            val defence = TacticalCombatResolver.resolve(attacker, pending.defensiveWeapons, chosen)
            val key = TacticalFireKey(tacticalTurnNumberProvider(), defender.id)
            val usage = tacticalUsage(defender)
            val updatedUsage = usage.copy(firedWeapons = usage.firedWeapons + chosen.mapTo(linkedSetOf(), TacticalWeaponContribution::weaponKey))
            val activation = chosenEcm.takeIf(List<TacticalWeaponStatus>::isNotEmpty)?.let { modules ->
                EcmActivation(
                    tacticalTurnNumberProvider(), defender.id, modules.mapTo(linkedSetOf(), TacticalWeaponStatus::key),
                    EcmRules.absoluteBearing(defender.snapshot(), attacker.mapCoordinate, unitQueryService), 1.0,
                    (EcmRules.CAPABILITY_PER_MODULE * modules.size).coerceAtMost(EcmRules.MAXIMUM_WIDTH_DEGREES), 1,
                ).also(onEcmActivation)
            }
            val beforeFiring = defender.snapshot()
            defender.customProperties = TacticalTurnMarker.update(defender.customProperties, key.turnNumber, updatedUsage, activation)
            if (defender.customProperties != beforeFiring.customProperties) onUnitEdited(beforeFiring, defender.snapshot())
            onTacticalFireUsageChanged(key, updatedUsage)
            pending.interceptedDamage = defence.damage
            units.firstOrNull { it.id == pending.attackerId }
                ?.takeIf { attackerUnit -> attackerUnit.snapshot().unitClassLabel() == "F" }
                ?.let { attackerUnit -> applyTacticalDamage(attackerUnit, defence.damage) }
            notifyUnitStateChanged()
            return defence.copy(
                attackerName = defender.snapshot().displayName(),
                ecmActivations = listOfNotNull(activation),
                effectiveEcmLevel = activation?.intensity ?: 0,
            )
        }

        private fun resolveIncomingAttack(pending: PendingIntercept): TacticalFireResolution? {
            if (pending.incomingResolved) return null
            pending.incomingResolved = true
            val defender = units.firstOrNull { it.id == pending.defenderId } ?: return null
            val attacker = units.firstOrNull { it.id == pending.attackerId }?.snapshot() ?: return null
            val reduced = TacticalCombatResolver.removeInterceptedAttackers(pending.incomingWeapons, pending.incomingContributions, pending.interceptedDamage)
            val attack = TacticalCombatResolver.resolve(
                defender.snapshot(), pending.incomingWeapons, reduced,
                ecmLevelsByWeaponKey = ecmLevelsFor(attacker, defender.snapshot(), pending.incomingWeapons, reduced),
            )
            val outcome = applyTacticalDamage(defender, attack.damage)
            notifyUnitStateChanged()
            return attack.copy(attackerName = attacker.displayName(), fightersDestroyed = outcome.fightersDestroyed, targetDestroyed = outcome.targetDestroyed)
        }

        private fun TacticalFireResolution.openGunrep() {
            MapViewerGunrepDialog(root, dialogManager, "gunrep-" + java.util.UUID.randomUUID(), this).open()
        }

        private fun ecmLevelsFor(
            attacker: UnitSnapshot,
            target: UnitSnapshot,
            weapons: List<TacticalWeaponStatus>,
            contributions: List<TacticalWeaponContribution>,
        ): Map<String, Int> {
            val weaponsByKey = weapons.associateBy(TacticalWeaponStatus::key)
            val activations = ecmActivationsProvider(tacticalTurnNumberProvider())
            val distance = unitQueryService.distanceBetween(attacker, target)
            return contributions.associate { contribution ->
                val weapon = weaponsByKey[contribution.weaponKey]
                val ordnance = weapon?.torpedo == true || weapon?.salvo == true || weapon?.salvoRack == true
                val affecting = activations.filter { activation ->
                    val emitter = units.firstOrNull { it.id == activation.emitterId }?.snapshot() ?: return@filter false
                    val sourceAffected = activation.emitterId != attacker.id &&
                            EcmRules.contains(activation, emitter, attacker.mapCoordinate, unitQueryService)
                    val destinationAffected = ordnance && EcmRules.contains(activation, emitter, target.mapCoordinate, unitQueryService)
                    sourceAffected || destinationAffected
                }
                val intensity = (affecting.sumOf(EcmActivation::intensity) +
                        if (!ordnance && attacker.unitClassLabel() == "F" && affecting.isNotEmpty()) 1 else 0).coerceAtMost(4)
                val profile = SensorObserverProfile.LOWEST_FIGHTER_SEEKER.takeIf { ordnance }
                val severity = SensorDetection.visibilityCheck(
                    attacker, target, distance, SensorReportMode.SCIREP, intensity, profile,
                )?.legacyEcmLevel ?: 0
                contribution.weaponKey to severity
            }
        }

        private fun ecmIntensityAt(receiver: UnitSnapshot): Int {
            val affecting = ecmActivationsProvider(tacticalTurnNumberProvider()).filter { activation ->
                if (activation.emitterId == receiver.id) return@filter false
                val emitter = units.firstOrNull { it.id == activation.emitterId }?.snapshot() ?: return@filter false
                EcmRules.contains(activation, emitter, receiver.mapCoordinate, unitQueryService)
            }
            return (affecting.sumOf(EcmActivation::intensity) +
                    if (receiver.unitClassLabel() == "F" && affecting.isNotEmpty()) 1 else 0).coerceAtMost(4)
        }

        private fun markDestroyed(unit: PlacedUnit) {
            if (unit.destroyed) return
            val before = unit.snapshot()
            unit.destroyed = true
            unit.updateStateOverlay()
            syncEditDialog(unit)
            onUnitEdited(before, unit.snapshot())
        }

        override fun moveBy(mapDelta: MapCoordinateDelta) {
            mapCoordinate = MapCoordinate(
                x = mapCoordinate.x + mapDelta.x,
                y = mapCoordinate.y + mapDelta.y,
            )
            positionView(this)
        }

        fun rebuildVisual(bitmap: Bitmap32) {
            fullVisualRebuildCount++
            view.removeChildren()
            facingLayer = view.container().also { it.zIndex = 0.0 }
            iconView = MapViewerImageViews.icon(view, bitmap).also { it.zIndex = 1.0 }
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
            normalizedWidth = unitSize.normalizedWidth
            normalizedHeight = unitSize.normalizedHeight
            rebuildVisual(template.bitmap)
            view.scale = unitSize.worldScale
            applyDestroyedUnitsDisplay()
            positionView(this)
        }

        fun updateFacingOverlay(forceActive: Boolean = false) {
            val degrees = facingDegrees ?: run {
                facingLayer.visible = false
                return
            }
            val width = iconView.bitmap.width.toDouble()
            val height = iconView.bitmap.height.toDouble()
            val fireArcsEnabled = MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL in MapViewerFeatureFlags.enabled &&
                    (id in tacticalDialogs || shouldDrawFireArcs(this))
            val definition = if (fireArcsEnabled) {
                OlympiadDawnShip.definition(customProperties) ?: OlympiadDawnShip.definition(template.customProperties)
            } else {
                null
            }
            val squadron = if (fireArcsEnabled && definition == null) OlympiadDawnShip.squadronStatus(snapshot(), units.map { it.snapshot() }) else null
            facingLayer.visible = true
            facingLayer.alpha = facingOverlayAlpha(forceActive || id in selectedUnitIds || dragState?.unit?.id == id)
            facingLayer.removeChildren()
            (definition?.weapons ?: squadron?.weapons)?.let { weapons ->
                drawBeamFireArcs(
                    parent = facingLayer,
                    width = width,
                    height = height,
                    facingDegrees = degrees,
                    color = template.affiliation.overlayColor(),
                    localPixelsPerUnit = 1.0 / (mapUnitsPerPixel * view.scaleX),
                    weapons = if (definition != null) weapons + definition.fighters.flatMap { fighter -> fighter.weapons } else weapons,
                    originMapCoordinate = mapCoordinate,
                    highlightTarget = tacticalSelectedTargetIds[id]?.let { targetId -> units.firstOrNull { candidate -> candidate.id == targetId }?.mapCoordinate },
                )
            }
            drawFacingIndicator(
                parent = facingLayer,
                width = width,
                height = height,
                facingDegrees = degrees,
                color = template.affiliation.overlayColor(),
            )
            facingGeometryRebuildCount++
        }

        fun updateStateOverlay() {
            overlayLayer.removeChildren()
            overlayLayer.alpha = 1.0
            if (!destroyed) {
                applyDestroyedUnitsDisplay()
                return
            }
            overlayLayer.alpha = 0.5
            drawStateCross(overlayLayer, iconView.bitmap.width.toDouble(), iconView.bitmap.height.toDouble(), toRgba(Palette.Colours.RED))
            applyDestroyedUnitsDisplay()
        }

        fun applyDestroyedUnitsDisplay() {
            view.visible = !destroyed || destroyedUnitsDisplay != DestroyedUnitsDisplay.Hide
            iconView.alpha = if (destroyed && destroyedUnitsDisplay == DestroyedUnitsDisplay.Partial) {
                DESTROYED_PARTIAL_ICON_ALPHA
            } else {
                1.0
            }
        }

        fun canMove(): Boolean =
            !destroyed || destroyedUnitsDisplay == DestroyedUnitsDisplay.Full

        fun canOpenContextMenu(): Boolean =
            !destroyed || destroyedUnitsDisplay != DestroyedUnitsDisplay.Hide
    }

    private fun updateEcmPreview(unit: PlacedUnit, target: TacticalTarget?, selectedWeaponKeys: Set<String>) {
        val moduleKeys = selectedWeaponKeys.filterTo(linkedSetOf()) { it.startsWith("ecm-") }
        if (target == null || moduleKeys.isEmpty()) {
            if (ecmPreview?.emitterId == unit.id) ecmPreview = null
            renderedEcmPreviewSignature = null
            return
        }
        val existing = ecmPreview?.takeIf { it.emitterId == unit.id && it.manual == target.manualEcm }
        val shape = if (target.manualEcm) {
            EcmRules.manual(moduleKeys.size, existing?.shape?.widthDegrees ?: 60.0, existing?.shape?.intensity ?: 1)
        } else {
            EcmRules.automatic(moduleKeys.size, target.rangeU)
        } ?: run {
            ecmPreview = null
            renderedEcmPreviewSignature = null
            return
        }
        val bearing = if (target.manualEcm) existing?.relativeBearingDegrees ?: 0.0 else {
            units.firstOrNull { it.id == target.targetId }?.snapshot()
                ?.let { EcmRules.absoluteBearing(unit.snapshot(), it.mapCoordinate, unitQueryService) }
                ?: 0.0
        }
        ecmPreview = EcmPreview(unit.id, target.targetId, target.manualEcm, moduleKeys, bearing, shape)
        renderedEcmPreviewSignature = null
    }

    private fun canFireTacticalSelection(target: TacticalTarget, selectedWeaponKeys: Set<String>): Boolean {
        if (selectedWeaponKeys.isEmpty()) return false
        val moduleCount = selectedWeaponKeys.count { it.startsWith("ecm-") }
        if (target.manualEcm) return moduleCount > 0 && ecmPreview?.let { EcmRules.valid(moduleCount, it.shape.rangeU, it.shape.widthDegrees, it.shape.intensity) } == true
        return moduleCount == 0 || EcmRules.automatic(moduleCount, target.rangeU) != null
    }

    fun hasEcmHandleAt(screenPoint: Vector2D): Boolean = ecmHandleAt(screenPoint) != null

    fun beginEcmHandleDragAt(screenPoint: Vector2D): Boolean {
        if (!interactionsEnabled() || contextMenuManager.isOpen || isHigherPriorityDragInProgress()) return false
        val handle = ecmHandleAt(screenPoint) ?: return false
        ecmDragHandle = handle
        return true
    }

    private fun ecmHandleAt(screenPoint: Vector2D): EcmHandle? {
        val preview = ecmPreview?.takeIf(EcmPreview::manual) ?: return null
        val emitter = units.firstOrNull { it.id == preview.emitterId }?.snapshot() ?: return null
        return ecmHandlePolygons(preview, emitter).entries.firstOrNull { (_, polygon) ->
            polygon.containsPoint(screenPoint) || polygon.minOf { point -> hypot(point.x - screenPoint.x, point.y - screenPoint.y) } <= ECM_HANDLE_TOLERANCE
        }?.key
    }

    private fun updateEcmDrag(pointer: Vector2D, leftHeld: Boolean) {
        val handle = ecmDragHandle ?: return
        if (!leftHeld) {
            ecmDragHandle = null
            return
        }
        val preview = ecmPreview ?: return
        val emitter = units.firstOrNull { it.id == preview.emitterId }?.snapshot() ?: return
        val point = coordinateSpace.screenToMap(pointer, viewportProvider())
        val pointerBearing = EcmRules.absoluteBearing(emitter, point, unitQueryService)
        val updated = when (handle) {
            EcmHandle.ROTATE -> preview.copy(relativeBearingDegrees = pointerBearing)
            EcmHandle.INTENSITY -> {
                val desiredRange = unitQueryService.distanceBetween(emitter.mapCoordinate, point)
                EcmRules.closestManualIntensity(preview.moduleKeys.size, preview.shape.widthDegrees, desiredRange)
                    ?.let { preview.copy(shape = it) } ?: preview
            }

            EcmHandle.WIDTH_START, EcmHandle.WIDTH_END -> {
                val delta = angularDistance(pointerBearing, preview.relativeBearingDegrees)
                EcmRules.resizedManual(
                    preview.moduleKeys.size,
                    delta * 2.0,
                    preview.shape.intensity,
                )?.let { preview.copy(shape = it) } ?: preview
            }
        }
        if (updated != preview) {
            val previousArc = TacticalTargetingResolver.arcForBearing(preview.relativeBearingDegrees, emitter.facingDegrees ?: 0)
            val updatedArc = TacticalTargetingResolver.arcForBearing(updated.relativeBearingDegrees, emitter.facingDegrees ?: 0)
            ecmPreview = updated
            renderedEcmPreviewSignature = null
            if (updatedArc != previousArc) tacticalDialogs[preview.emitterId]?.refreshPresentation()
        }
    }

    private fun EcmPreview.handlePoints(emitter: UnitSnapshot): Map<EcmHandle, MapCoordinate> {
        val absoluteBearing = relativeBearingDegrees
        val placed = units.firstOrNull { it.id == emitter.id }
        val innerRange = placed?.let { ecmInnerRadius(it.worldWidth, it.worldHeight) * mapUnitsPerPixel } ?: 0.0
        return mapOf(
            EcmHandle.ROTATE to ecmPoint(emitter.mapCoordinate, absoluteBearing, (innerRange + shape.rangeU) * 0.5),
            EcmHandle.INTENSITY to ecmPoint(emitter.mapCoordinate, absoluteBearing, shape.rangeU),
            EcmHandle.WIDTH_START to ecmPoint(emitter.mapCoordinate, absoluteBearing - shape.widthDegrees * 0.5, (innerRange + shape.rangeU) * 0.5),
            EcmHandle.WIDTH_END to ecmPoint(emitter.mapCoordinate, absoluteBearing + shape.widthDegrees * 0.5, (innerRange + shape.rangeU) * 0.5),
        )
    }

    private fun ecmHandlePolygons(preview: EcmPreview, emitter: UnitSnapshot): Map<EcmHandle, List<Vector2D>> {
        val viewport = viewportProvider()
        val centers = preview.handlePoints(emitter).mapValues { (_, point) -> coordinateSpace.worldToScreen(coordinateSpace.toWorld(point), viewport) }
        val bearing = Math.toRadians(preview.relativeBearingDegrees)
        val tangent = Vector2D(cos(bearing), sin(bearing))
        val radial = Vector2D(sin(bearing), -cos(bearing))
        val intensityCenter = centers.getValue(EcmHandle.INTENSITY)
        val rotateCenter = centers.getValue(EcmHandle.ROTATE)
        fun offset(center: Vector2D, first: Vector2D, firstScale: Double, second: Vector2D, secondScale: Double) =
            Vector2D(center.x + first.x * firstScale + second.x * secondScale, center.y + first.y * firstScale + second.y * secondScale)

        val handles = mutableMapOf(
            EcmHandle.INTENSITY to listOf(
                offset(intensityCenter, tangent, -ECM_INTENSITY_HANDLE_LENGTH * 0.5, radial, -ECM_HANDLE_THICKNESS * 0.5),
                offset(intensityCenter, tangent, ECM_INTENSITY_HANDLE_LENGTH * 0.5, radial, -ECM_HANDLE_THICKNESS * 0.5),
                offset(intensityCenter, tangent, ECM_INTENSITY_HANDLE_LENGTH * 0.5, radial, ECM_HANDLE_THICKNESS * 0.5),
                offset(intensityCenter, tangent, -ECM_INTENSITY_HANDLE_LENGTH * 0.5, radial, ECM_HANDLE_THICKNESS * 0.5),
            ),
            EcmHandle.ROTATE to listOf(
                Vector2D(rotateCenter.x, rotateCenter.y - ECM_DIRECTION_HANDLE_RADIUS),
                Vector2D(rotateCenter.x + ECM_DIRECTION_HANDLE_RADIUS, rotateCenter.y),
                Vector2D(rotateCenter.x, rotateCenter.y + ECM_DIRECTION_HANDLE_RADIUS),
                Vector2D(rotateCenter.x - ECM_DIRECTION_HANDLE_RADIUS, rotateCenter.y),
            ),
        )
        listOf(EcmHandle.WIDTH_START to -preview.shape.widthDegrees * 0.5, EcmHandle.WIDTH_END to preview.shape.widthDegrees * 0.5)
            .forEach { (handle, offsetDegrees) ->
                val edgeBearing = Math.toRadians(preview.relativeBearingDegrees + offsetDegrees)
                val edgeRadial = Vector2D(sin(edgeBearing), -cos(edgeBearing))
                val edgeTangent = Vector2D(cos(edgeBearing), sin(edgeBearing))
                val center = centers.getValue(handle)
                handles[handle] = listOf(
                    offset(center, edgeRadial, -ECM_WIDTH_HANDLE_LENGTH * 0.5, edgeTangent, -ECM_HANDLE_THICKNESS * 0.5),
                    offset(center, edgeRadial, ECM_WIDTH_HANDLE_LENGTH * 0.5, edgeTangent, -ECM_HANDLE_THICKNESS * 0.5),
                    offset(center, edgeRadial, ECM_WIDTH_HANDLE_LENGTH * 0.5, edgeTangent, ECM_HANDLE_THICKNESS * 0.5),
                    offset(center, edgeRadial, -ECM_WIDTH_HANDLE_LENGTH * 0.5, edgeTangent, ECM_HANDLE_THICKNESS * 0.5),
                )
            }
        return handles
    }

    private fun angularDistance(first: Double, second: Double): Double =
        kotlin.math.abs(((first - second + 540.0).mod(360.0)) - 180.0)

    private fun List<Vector2D>.containsPoint(point: Vector2D): Boolean = indices.count { index ->
        val start = this[index]
        val end = this[(index + 1) % size]
        (start.y > point.y) != (end.y > point.y) &&
                point.x < (end.x - start.x) * (point.y - start.y) / (end.y - start.y) + start.x
    } % 2 == 1

    private fun ecmPoint(origin: MapCoordinate, bearingDegrees: Double, rangeU: Double): MapCoordinate {
        val radians = Math.toRadians(bearingDegrees)
        val pixels = rangeU / mapUnitsPerPixel
        return MapCoordinate(origin.x + sin(radians) * pixels / mapWidth, origin.y - cos(radians) * pixels / mapHeight)
    }

    private fun shouldDrawFireArcs(unit: PlacedUnit): Boolean =
        FireArcSelectionResolver.shouldDraw(
            mode = fireArcDisplayMode,
            selectedUnitIds = selectedUnitIds,
            selectedAffiliations = units.filter { it.id in selectedUnitIds }.map { it.template.affiliation },
            targetId = unit.id,
            targetAffiliation = unit.template.affiliation,
        )

    private fun selectionAffiliations(ids: Set<MapPlacedObjectId>): Set<UnitAffiliation?> =
        units.filter { it.id in ids }.mapTo(linkedSetOf()) { it.template.affiliation }

    private data class DragState(
        val unit: PlacedUnit,
        val preview: UnitVisual,
        val offset: Vector2D,
        val originalSnapshot: UnitSnapshot,
    )

    private data class UnitEditSession(
        var baselineSnapshot: UnitSnapshot,
        var baselinePosition: GridReferenceFields,
    ) {
        fun rebase(snapshot: UnitSnapshot, position: GridReferenceFields) {
            baselineSnapshot = snapshot
            baselinePosition = position
        }
    }

    private data class TacticalDialogKey(val unitId: MapPlacedObjectId)

    private data class TacticalDamageOutcome(val fightersDestroyed: Int = 0, val targetDestroyed: Boolean = false)

    private data class EcmPreview(
        val emitterId: MapPlacedObjectId,
        val targetId: MapPlacedObjectId,
        val manual: Boolean,
        val moduleKeys: Set<String>,
        val relativeBearingDegrees: Double,
        val shape: EcmShape,
    ) {
        fun activation(turnNumber: Int): EcmActivation = EcmActivation(
            turnNumber, emitterId, moduleKeys, relativeBearingDegrees, shape.rangeU, shape.widthDegrees, shape.intensity,
        )
    }

    private enum class EcmHandle { ROTATE, INTENSITY, WIDTH_START, WIDTH_END }

    private data class PendingIntercept(
        val attackerId: MapPlacedObjectId,
        val defenderId: MapPlacedObjectId,
        val incomingWeapons: List<TacticalWeaponStatus>,
        val incomingContributions: List<TacticalWeaponContribution>,
        val defensiveWeapons: List<TacticalWeaponStatus>,
        val defensiveContributions: List<TacticalWeaponContribution>,
        var defensiveFireResolved: Boolean = false,
        var interceptedDamage: Int = 0,
        var incomingResolved: Boolean = false,
    )

    private fun positionView(unit: PlacedUnit) {
        val worldPoint = coordinateSpace.toWorld(unit.mapCoordinate)
        unit.view.x = worldPoint.x - unit.worldWidth * 0.5
        unit.view.y = worldPoint.y - unit.worldHeight * 0.5
    }

    internal fun constrainMove(snapshot: UnitSnapshot, overrideMoveLimit: Boolean): UnitSnapshot =
        snapshot.copy(mapCoordinate = constrainMove(snapshot, snapshot.mapCoordinate, overrideMoveLimit))

    private fun constrainMove(unit: PlacedUnit, requested: MapCoordinate, overrideMoveLimit: Boolean = false): MapCoordinate =
        constrainMove(unit.snapshot(), requested, overrideMoveLimit)

    private fun constrainMove(snapshot: UnitSnapshot, requested: MapCoordinate, overrideMoveLimit: Boolean = false): MapCoordinate =
        MoveLimit.parse(snapshot.template.customProperties)
            ?.let { limit ->
                requested.takeIf { overrideMoveLimit }?.also { onTurnOriginReset(snapshot.id, it) }
                    ?: turnOriginProvider(snapshot.id)?.let { origin ->
                        MoveLimit.constrain(
                            origin,
                            requested,
                            MoveLimit.withWarpMultiplier(limit, snapshot.customProperties),
                            mapWidth,
                            mapHeight,
                            mapUnitsPerPixel,
                        )
                    }
            }
            ?: requested

    private fun syncEditDialog(unit: PlacedUnit) {
        val position = initialReferenceFields(unit)
        editSessions[unit.id]?.rebase(unit.snapshot(), position)
        dialogManager.updateTarget(
            unit.id,
            MapViewerDialogPositionSync(positionX = position.easting, positionY = position.northing),
        )
    }

    private fun syncEditDialogPosition(unit: PlacedUnit) {
        val position = initialReferenceFields(unit)
        dialogManager.updateTarget(
            unit.id,
            MapViewerDialogPositionSync(positionX = position.easting, positionY = position.northing),
        )
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
        facingColor: Color = UnitAffiliationUi.unaffiliatedColor(),
    ): UnitVisual {
        val container = parent.container()
        val facingLayer = container.container().also { it.zIndex = 0.0 }
        val icon = MapViewerImageViews.icon(container, bitmap).also { it.zIndex = 1.0 }
        facingDegrees?.let { degrees ->
            facingLayer.alpha = facingOverlayAlpha(facingActive)
            drawFacingIndicator(
                parent = facingLayer,
                width = bitmap.width.toDouble(),
                height = bitmap.height.toDouble(),
                facingDegrees = degrees,
                color = facingColor,
            )
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
        color: Color,
    ) {
        val center = Point(width * 0.5, height * 0.5)
        val outerRadius = bearingIndicatorOuterRadius(width, height)
        val innerRadius = outerRadius * 0.86
        val ringColor = toRgba(color)

        val radians = Math.toRadians(facingDegrees.toDouble())
        val arcRadius = (outerRadius + innerRadius) * 0.5
        val arcThickness = outerRadius - innerRadius
        parent.graphics {
            lineWidth = arcThickness
            stroke(ringColor) {
                arc(
                    center,
                    arcRadius,
                    Angle.fromRadians(radians - (PI * 2.0 / 3.0)),
                    Angle.fromRadians(radians - (PI / 3.0)),
                )
            }
            lineWidth = arcThickness * 0.5
            stroke(ringColor) {
                arc(
                    center,
                    arcRadius,
                    Angle.fromRadians(radians - PI),
                    Angle.fromRadians(radians - (PI * 2.0 / 3.0)),
                )
                arc(
                    center,
                    arcRadius,
                    Angle.fromRadians(radians - (PI / 3.0)),
                    Angle.fromRadians(radians),
                )
            }
        }

        val direction = Vector2D(sin(radians), -cos(radians))
        val perpendicular = Vector2D(cos(radians), sin(radians))
        val tip = Point(
            center.x + direction.x * outerRadius * 1.22,
            center.y + direction.y * outerRadius * 1.22,
        )
        val base = Point(
            center.x + direction.x * arcRadius,
            center.y + direction.y * arcRadius,
        )
        val halfWidth = minOf(width, height) * 0.16

        parent.graphics {
            fill(ringColor) {
                moveTo(tip.x, tip.y)
                lineTo(base.x + perpendicular.x * halfWidth, base.y + perpendicular.y * halfWidth)
                lineTo(base.x - perpendicular.x * halfWidth, base.y - perpendicular.y * halfWidth)
                close()
            }
        }
    }

    private fun drawBeamFireArcs(
        parent: Container,
        width: Double,
        height: Double,
        facingDegrees: Int,
        color: Color,
        localPixelsPerUnit: Double,
        weapons: List<OlympiadDawnShipWeapon>,
        originMapCoordinate: MapCoordinate,
        highlightTarget: MapCoordinate? = null,
    ) {
        val center = Point(width * 0.5, height * 0.5)
        val borderColor = toRgba(color)
        val rangeBandColor = toRgba(color, (color.alpha * BEAM_RANGE_BAND_ALPHA).toInt())

        highlightTarget?.let { target ->
            drawHighlightedFireArcSegment(parent, center, facingDegrees, originMapCoordinate, target, localPixelsPerUnit, color)
        }

        data class ArcRange(val arcIndex: Int, val band: Int)

        val beamWeapons = weapons.mapNotNull { weapon ->
            val beamClass = weapon.beamClass ?: weapon.magazineId?.let { 2 } ?: weapon.salvoRack.takeIf { it }?.let { 2 } ?: weapon.pointDefense.takeIf { it }?.let { 1 }
            ?: weapon.submunition.takeIf { it }?.let { 1 } ?: return@mapNotNull null
            val arcIndices = weapon.arcs.mapNotNull { BEAM_ARC_ORDER.indexOf(it).takeIf { index -> index >= 0 } }.distinct()
            arcIndices.takeIf { it.isNotEmpty() }?.let { beamClass to it }
        }
        val coverage = mutableMapOf<ArcRange, Int>()
        beamWeapons.forEach { (beamClass, arcIndices) ->
            arcIndices.forEach { arcIndex ->
                (1..beamClass).forEach { band ->
                    val key = ArcRange(arcIndex, band)
                    coverage[key] = (coverage[key] ?: 0) + 1
                }
            }
        }

        val maximumBand = beamWeapons.maxOfOrNull { it.first } ?: return
        (1..maximumBand).forEach { band ->
            val counts = BEAM_ARC_ORDER.indices.map { arcIndex -> coverage[ArcRange(arcIndex, band)] ?: 0 }
            val outerCounts = BEAM_ARC_ORDER.indices.map { arcIndex ->
                beamWeapons.count { (beamClass, arcIndices) -> beamClass == band && arcIndex in arcIndices }
            }
            val commonCount = counts.minOrNull() ?: 0
            val radius = band * OlympiadDawnBeamRules.RANGE_BAND_SIZE_U * localPixelsPerUnit
            val widthForCount = { count: Int, isOuter: Boolean ->
                if (count > 1) {
                    (if (isOuter) BEAM_BORDER_WIDTH else BEAM_RANGE_BAND_WIDTH) * 2.0
                } else {
                    if (isOuter) BEAM_BORDER_WIDTH else BEAM_RANGE_BAND_WIDTH
                }
            }

            val uniformBoundary = outerCounts.distinct().size <= 1
            if (commonCount > 0 && uniformBoundary) {
                drawCircle(
                    parent,
                    center,
                    radius,
                    if (outerCounts.firstOrNull()?.let { it > 0 } == true) borderColor else rangeBandColor,
                    widthForCount(commonCount, outerCounts.firstOrNull()?.let { it > 0 } == true),
                )
            }
            counts.forEachIndexed { arcIndex, count ->
                if (count == 0 || (count <= commonCount && uniformBoundary)) return@forEachIndexed
                val startDegrees = facingDegrees + arcIndex * BEAM_ARC_WIDTH_DEGREES - BEAM_ARC_WIDTH_DEGREES * 0.5
                drawSectorArc(
                    parent,
                    center,
                    radius,
                    startDegrees,
                    startDegrees + BEAM_ARC_WIDTH_DEGREES,
                    if (outerCounts[arcIndex] > 0) borderColor else rangeBandColor,
                    widthForCount(count, outerCounts[arcIndex] > 0),
                )
            }
        }

        beamWeapons.forEach { (beamClass, arcIndices) ->
            if (arcIndices.size < BEAM_ARC_ORDER.size) {
                val firstArc = arcIndices.first()
                val startDegrees = facingDegrees + firstArc * BEAM_ARC_WIDTH_DEGREES - BEAM_ARC_WIDTH_DEGREES * 0.5
                val endDegrees = startDegrees + arcIndices.size * BEAM_ARC_WIDTH_DEGREES
                val maximumRadius = beamClass * OlympiadDawnBeamRules.RANGE_BAND_SIZE_U * localPixelsPerUnit
                drawSectorRadius(parent, center, maximumRadius, startDegrees, borderColor, BEAM_BORDER_WIDTH)
                drawSectorRadius(parent, center, maximumRadius, endDegrees, borderColor, BEAM_BORDER_WIDTH)
            }
        }
    }

    private fun drawCircle(parent: Container, center: Point, radius: Double, color: RGBA, width: Double) {
        parent.graphics {
            lineWidth = width
            stroke(color) {
                sectorPoints(center, radius, 0.0, 360.0).forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
        }
    }

    private fun bearingIndicatorOuterRadius(width: Double, height: Double): Double = minOf(width, height) * 0.72

    private fun ecmInnerRadius(width: Double, height: Double): Double =
        bearingIndicatorOuterRadius(width, height) * BEARING_INDICATOR_THIN_ARC_CENTER_RATIO

    private fun drawEcmField(parent: Container, activation: EcmActivation, emitter: PlacedUnit, preview: Boolean = false) {
        val snapshot = emitter.snapshot()
        val center = coordinateSpace.toWorld(snapshot.mapCoordinate)
        val radius = activation.rangeU / mapUnitsPerPixel
        val innerRadius = ecmInnerRadius(emitter.worldWidth, emitter.worldHeight).coerceAtMost(radius)
        val bearing = activation.relativeBearingDegrees
        val start = bearing - activation.widthDegrees * 0.5
        val end = start + activation.widthDegrees
        val outerArc = sectorPoints(center, radius, start, end)
        val innerArc = sectorPoints(center, innerRadius, start, end).reversed()
        val color = UnitAffiliationUi.swatchColor(snapshot.template.affiliation)
        val viewport = viewportProvider()
        val viewportZoom = viewport.zoom.coerceAtLeast(MIN_VIEWPORT_ZOOM)
        val pitch = ECM_HATCH_PITCH_PX / viewportZoom
        val hatchSegments = ecmHatchSegments(
            center, innerRadius, radius, bearing, activation.widthDegrees, outerArc + innerArc, pitch,
            maxOf(center.x - radius, viewport.left - pitch), minOf(center.x + radius, viewport.right + pitch),
            maxOf(center.y - radius, viewport.top - pitch), minOf(center.y + radius, viewport.bottom + pitch),
        )
        parent.graphics(GraphicsRenderer.GPU) {
            lineWidth = pitch * 0.5
            stroke(toRgba(color, if (preview) 145 else 100)) {
                hatchSegments.forEach { (segmentStart, segmentEnd) ->
                    moveTo(segmentStart.x, segmentStart.y)
                    lineTo(segmentEnd.x, segmentEnd.y)
                }
            }
            lineWidth = ECM_OUTLINE_WIDTH_PX / viewportZoom
            stroke(toRgba(color, if (preview) 240 else 190)) {
                outerArc.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
                innerArc.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
                if (activation.widthDegrees < EcmRules.MAXIMUM_WIDTH_DEGREES) {
                    moveTo(outerArc.first().x, outerArc.first().y)
                    lineTo(innerArc.last().x, innerArc.last().y)
                    moveTo(outerArc.last().x, outerArc.last().y)
                    lineTo(innerArc.first().x, innerArc.first().y)
                }
            }
        }
    }

    private fun ecmHatchSegments(
        center: Point,
        innerRadius: Double,
        outerRadius: Double,
        bearingDegrees: Double,
        widthDegrees: Double,
        boundary: List<Point>,
        pitch: Double,
        minimumX: Double,
        maximumX: Double,
        minimumY: Double,
        maximumY: Double,
    ): List<Pair<Point, Point>> {
        if (minimumX >= maximumX || minimumY >= maximumY) return emptyList()
        val diagonalStep = pitch * sqrt(2.0)
        val firstBand = floor((minimumY - maximumX) / diagonalStep).toInt() - 1
        val lastBand = floor((maximumY - minimumX) / diagonalStep).toInt() + 1
        val lineMinimumX = minimumX - pitch
        val lineMaximumX = maximumX + pitch
        return (firstBand..lastBand).flatMap { index ->
            val intercept = index * diagonalStep
            val intersections = buildList {
                add(lineMinimumX)
                boundary.indices.forEach { boundaryIndex ->
                    val first = boundary[boundaryIndex]
                    val second = boundary[(boundaryIndex + 1) % boundary.size]
                    val firstOffset = first.y - first.x - intercept
                    val secondOffset = second.y - second.x - intercept
                    if (kotlin.math.abs(firstOffset) < ECM_GEOMETRY_EPSILON) add(first.x)
                    if (firstOffset * secondOffset < 0.0) {
                        val fraction = firstOffset / (firstOffset - secondOffset)
                        val intersectionX = first.x + (second.x - first.x) * fraction
                        if (intersectionX in lineMinimumX..lineMaximumX) add(intersectionX)
                    }
                }
                add(lineMaximumX)
            }
                .sorted()
                .fold(mutableListOf<Double>()) { unique, value ->
                    if (unique.lastOrNull()?.let { kotlin.math.abs(it - value) > ECM_GEOMETRY_EPSILON } != false) unique.add(value)
                    unique
                }
            intersections.zipWithNext().mapNotNull { (startX, endX) ->
                val midpointX = (startX + endX) * 0.5
                val midpoint = Point(midpointX, midpointX + intercept)
                midpoint.takeIf {
                    isInsideEcmSector(it, center, innerRadius, outerRadius, bearingDegrees, widthDegrees)
                }?.let { Point(startX, startX + intercept) to Point(endX, endX + intercept) }
            }
        }
    }

    private fun isInsideEcmSector(
        point: Point,
        center: Point,
        innerRadius: Double,
        outerRadius: Double,
        bearingDegrees: Double,
        widthDegrees: Double,
    ): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = hypot(dx, dy)
        if (distance < innerRadius - ECM_GEOMETRY_EPSILON || distance > outerRadius + ECM_GEOMETRY_EPSILON) return false
        if (widthDegrees >= EcmRules.MAXIMUM_WIDTH_DEGREES - ECM_GEOMETRY_EPSILON) return true
        val pointBearing = Math.toDegrees(kotlin.math.atan2(dx, -dy)).mod(360.0)
        val delta = ((pointBearing - bearingDegrees + 540.0).mod(360.0)) - 180.0
        return kotlin.math.abs(delta) <= widthDegrees * 0.5 + ECM_GEOMETRY_EPSILON
    }

    private fun drawEcmHandles(parent: Container, preview: EcmPreview, emitter: UnitSnapshot) {
        if (!preview.manual) return
        val color = toRgba(UnitAffiliationUi.swatchColor(emitter.template.affiliation), 230)
        val viewport = viewportProvider()
        ecmHandlePolygons(preview, emitter).values.forEach { polygon ->
            val worldPoints = polygon.map { point -> coordinateSpace.toWorld(coordinateSpace.screenToMap(point, viewport)) }
            parent.graphics {
                fill(color) {
                    moveTo(worldPoints.first().x, worldPoints.first().y)
                    worldPoints.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
            }
        }
    }

    private fun drawSectorArc(parent: Container, center: Point, radius: Double, startDegrees: Double, endDegrees: Double, color: RGBA, width: Double) {
        parent.graphics {
            lineWidth = width
            stroke(color) {
                sectorPoints(center, radius, startDegrees, endDegrees).forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
        }
    }

    private fun drawSectorRadius(parent: Container, center: Point, radius: Double, degrees: Double, color: RGBA, width: Double) {
        val end = sectorPoint(center, radius, degrees)
        parent.graphics {
            lineWidth = width
            stroke(color) {
                moveTo(center.x, center.y)
                lineTo(end.x, end.y)
            }
        }
    }

    private fun drawHighlightedFireArcSegment(
        parent: Container,
        center: Point,
        facingDegrees: Int,
        origin: MapCoordinate,
        target: MapCoordinate,
        localPixelsPerUnit: Double,
        color: Color,
    ) {
        val dx = (target.x - origin.x) * mapWidth
        val dy = (target.y - origin.y) * mapHeight
        val range = hypot(dx, dy) * mapUnitsPerPixel
        if (range <= 0.0) return
        val band = kotlin.math.floor(range / OlympiadDawnBeamRules.RANGE_BAND_SIZE_U).toInt() + 1
        val bearing = Math.toDegrees(kotlin.math.atan2(dx, -dy))
        val arcIndex = kotlin.math.floor(((bearing - facingDegrees).mod(360.0) + 30.0) / 60.0).toInt().mod(BEAM_ARC_ORDER.size)
        val start = facingDegrees + arcIndex * BEAM_ARC_WIDTH_DEGREES - BEAM_ARC_WIDTH_DEGREES * 0.5
        val outer = sectorPoints(center, band * OlympiadDawnBeamRules.RANGE_BAND_SIZE_U * localPixelsPerUnit, start, start + BEAM_ARC_WIDTH_DEGREES)
        val inner = sectorPoints(center, (band - 1) * OlympiadDawnBeamRules.RANGE_BAND_SIZE_U * localPixelsPerUnit, start, start + BEAM_ARC_WIDTH_DEGREES).reversed()
        parent.graphics {
            fill(toRgba(color, (color.alpha * 0.3).toInt())) {
                outer.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
                inner.forEach { point -> lineTo(point.x, point.y) }
                close()
            }
        }
    }

    private fun sectorPoints(center: Point, radius: Double, startDegrees: Double, endDegrees: Double): List<Point> {
        val segments = (kotlin.math.abs(endDegrees - startDegrees) / BEAM_ARC_SAMPLE_DEGREES).toInt().coerceAtLeast(1)
        return (0..segments).map { index ->
            sectorPoint(center, radius, startDegrees + (endDegrees - startDegrees) * index / segments)
        }
    }

    private fun sectorPoint(center: Point, radius: Double, degrees: Double): Point {
        val radians = Math.toRadians(degrees)
        return Point(center.x + sin(radians) * radius, center.y - cos(radians) * radius)
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

    private fun worldUnitSize(bitmap: korlibs.image.bitmap.Bitmap32, viewportZoom: Double = viewportProvider().zoom): WorldUnitSize {
        val worldWidth = unitIconSizePx / viewportZoom.coerceAtLeast(MIN_VIEWPORT_ZOOM)
        val worldScale = worldWidth / bitmap.width
        return WorldUnitSize(
            worldWidth = worldWidth,
            worldHeight = bitmap.height * worldScale,
            worldScale = worldScale,
            normalizedWidth = worldWidth / mapWidth,
            normalizedHeight = bitmap.height * worldScale / mapHeight,
        )
    }

    private data class WorldUnitSize(
        val worldWidth: Double,
        val worldHeight: Double,
        val worldScale: Double,
        val normalizedWidth: Double,
        val normalizedHeight: Double,
    )

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private companion object {
        const val DRAG_PREVIEW_Z_INDEX = 15_000.0
        private const val HIT_TOLERANCE = 8.0
        private const val ECM_HANDLE_TOLERANCE = 14.0
        private const val ECM_INTENSITY_HANDLE_LENGTH = 24.0
        private const val ECM_WIDTH_HANDLE_LENGTH = 24.0
        private const val ECM_HANDLE_THICKNESS = 7.0
        private const val ECM_DIRECTION_HANDLE_RADIUS = 13.0
        private const val ECM_HATCH_PITCH_PX = 16.0
        private const val ECM_OUTLINE_WIDTH_PX = 2.0
        private const val ECM_GEOMETRY_EPSILON = 0.000001
        private const val BEARING_INDICATOR_THIN_ARC_CENTER_RATIO = 0.93
        private const val DESTROYED_PARTIAL_ICON_ALPHA = 0.5
        private const val MIN_VIEWPORT_ZOOM = 0.0001
        private const val BEAM_ARC_WIDTH_DEGREES = 60.0
        private const val BEAM_ARC_SAMPLE_DEGREES = 5.0
        private const val BEAM_BORDER_WIDTH = 1.5
        private const val BEAM_RANGE_BAND_WIDTH = 1.0
        private const val BEAM_RANGE_BAND_ALPHA = 0.5
        private val BEAM_ARC_ORDER = listOf("F", "FS", "AS", "A", "AP", "FP")
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

    fun parseValid(input: String): UnitFacingParseResult? =
        parse(input).takeIf { it.isValid }

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
