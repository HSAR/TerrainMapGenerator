package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.LineDrawing
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import io.hsar.mapgenerator.commandgui.nextTestUuid
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MapSelectionControllerTest {
    @Test
    fun `box selection ignores objects rejected by the selectable predicate`() {
        val objectController = objectController()
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000061")),
            name = "Destroyed",
            bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        objectController.createObject(
            UnitSnapshot(
                id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000062")),
                unitTemplateId = template.id,
                template = template,
                instanceName = "Destroyed",
                destroyed = true,
                facingDegrees = null,
                mapCoordinate = MapCoordinate(0.5, 0.5),
                normalizedWidth = 0.1,
                normalizedHeight = 0.1,
            ),
        )
        val selectionController = MapSelectionController(
            coordinateSpace = MapCoordinateSpace(GEOMETRY, mapWidth = 1000, mapHeight = 1000),
            viewportProvider = ::viewport,
            objectController = objectController,
            contextMenuManager = ContextMenuManager(Container()),
            objectAt = { null },
            isSelectableObject = { snapshot -> snapshot !is UnitSnapshot || !snapshot.destroyed },
            onStateChanged = {},
            onLedgerEntry = {},
        )

        selectionController.beginLeftDrag(Vector2D(100.0, 100.0))
        selectionController.update(Vector2D(900.0, 900.0), leftHeld = false)

        assertThat(selectionController.selectedIds(), equalTo(emptySet()))
    }

    @Test
    fun `shift click toggles objects in the current selection`() {
        val objectController = objectController()
        val first = unitSnapshot("00000000-0000-0000-0000-000000000081", destroyed = false, x = 0.2, y = 0.2)
        val second = unitSnapshot("00000000-0000-0000-0000-000000000082", destroyed = false, x = 0.4, y = 0.4)
        objectController.createObject(first)
        objectController.createObject(second)
        val selectionController = selectionController(
            objectController = objectController,
            objectAt = { point ->
                if (point.x < 150.0) {
                    MapPlacedObjectSelection(MapPlacedObjectSelectionKind.UNIT, first.id, 0.0)
                } else {
                    MapPlacedObjectSelection(MapPlacedObjectSelectionKind.UNIT, second.id, 0.0)
                }
            },
        )

        selectionController.selectOnly(first.id)
        selectionController.beginLeftDrag(Vector2D(100.0, 100.0), shiftHeld = true)
        assertThat(selectionController.selectedIds(), equalTo(emptySet()))

        selectionController.beginLeftDrag(Vector2D(200.0, 100.0), shiftHeld = true)
        assertThat(selectionController.selectedIds(), equalTo(setOf(second.id)))

        selectionController.beginLeftDrag(Vector2D(100.0, 100.0), shiftHeld = true)
        assertThat(selectionController.selectedIds(), equalTo(setOf(second.id, first.id)))
    }

    @Test
    fun `shift drag bypasses the unit move constraint`() {
        val objectController = objectController()
        val unit = unitSnapshot("00000000-0000-0000-0000-000000000091", destroyed = false)
        objectController.createObject(unit)
        var receivedShiftOverride = false
        val selectionController = MapSelectionController(
            coordinateSpace = MapCoordinateSpace(GEOMETRY, mapWidth = 1000, mapHeight = 1000),
            viewportProvider = ::viewport,
            objectController = objectController,
            contextMenuManager = ContextMenuManager(Container()),
            objectAt = { MapPlacedObjectSelection(MapPlacedObjectSelectionKind.UNIT, unit.id, 0.0) },
            onStateChanged = {},
            onLedgerEntry = {},
            constrainUnitMove = { snapshot, shiftHeld ->
                receivedShiftOverride = shiftHeld
                snapshot
            },
        )

        selectionController.beginLeftDrag(Vector2D(500.0, 500.0))
        selectionController.update(Vector2D(900.0, 900.0), leftHeld = true, shiftHeld = true)

        assertThat(receivedShiftOverride, equalTo(true))
    }

    @Test
    fun `shift drag inverts selection status for every object in the box`() {
        val objectController = objectController()
        val first = unitSnapshot("00000000-0000-0000-0000-000000000083", destroyed = false, x = 0.2, y = 0.2)
        val second = unitSnapshot("00000000-0000-0000-0000-000000000084", destroyed = false, x = 0.4, y = 0.4)
        val third = unitSnapshot("00000000-0000-0000-0000-000000000085", destroyed = false, x = 0.6, y = 0.6)
        objectController.createObject(first)
        objectController.createObject(second)
        objectController.createObject(third)
        val selectionController = selectionController(
            objectController = objectController,
            objectAt = { point ->
                if (point.x < 150.0 && point.y < 150.0) {
                    MapPlacedObjectSelection(MapPlacedObjectSelectionKind.UNIT, first.id, 0.0)
                } else if (point.x > 600.0 && point.y < 150.0) {
                    MapPlacedObjectSelection(MapPlacedObjectSelectionKind.UNIT, third.id, 0.0)
                } else {
                    null
                }
            },
        )

        selectionController.selectOnly(first.id)
        selectionController.beginLeftDrag(Vector2D(650.0, 100.0), shiftHeld = true)
        selectionController.beginLeftDrag(Vector2D(200.0, 200.0), shiftHeld = true)
        selectionController.update(Vector2D(700.0, 700.0), leftHeld = false)

        assertThat(selectionController.selectedIds(), equalTo(setOf(second.id)))
    }

    @Test
    fun `resize handle drag updates selected drawing before object move`() {
        val objectController = objectController()
        val drawing = drawingSnapshot("00000000-0000-0000-0000-000000000071")
        objectController.createObject(drawing)
        val storedDrawing = objectController.snapshot(drawing.id) as DrawingSnapshot
        val selectionController = selectionController(
            objectController = objectController,
            objectAt = { MapPlacedObjectSelection(MapPlacedObjectSelectionKind.DRAWING, drawing.id, 0.0) },
        )
        selectionController.selectOnly(drawing.id)

        selectionController.beginLeftDrag(resizeHandleCenterFor(listOf(storedDrawing)))
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = true)

        val resized = objectController.snapshot(drawing.id) as DrawingSnapshot
        assertThat(resized.drawing != drawing.drawing, equalTo(true))
    }

    @Test
    fun `shift resize propagates to rectangles and locks them to squares`() {
        val objectController = objectController()
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000145")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.2, 0.2),
                end = MapDrawingPoint(0.4, 0.4),
                color = Color.RED,
            ),
        )
        objectController.createObject(drawing)
        val selectionController = selectionController(
            objectController = objectController,
            objectAt = { MapPlacedObjectSelection(MapPlacedObjectSelectionKind.DRAWING, drawing.id, 0.0) },
        )
        selectionController.selectOnly(drawing.id)

        selectionController.beginLeftDrag(resizeHandleCenterFor(listOf(objectController.snapshot(drawing.id) as DrawingSnapshot)))
        selectionController.update(Vector2D(600.0, 100.0), leftHeld = true, shiftHeld = true)

        val resized = objectController.snapshot(drawing.id) as DrawingSnapshot
        val rectangle = resized.drawing as RectangleDrawing
        assertThat(abs(rectangle.end.x - rectangle.start.x), closeTo(abs(rectangle.end.y - rectangle.start.y), 0.0001))
    }

    @Test
    fun `resize release commits an object edit`() {
        val objectController = objectController()
        val ledgerEntries = mutableListOf<LedgerEntry>()
        val drawing = drawingSnapshot("00000000-0000-0000-0000-000000000072")
        objectController.createObject(drawing)
        val selectionController = selectionController(
            objectController = objectController,
            onLedgerEntry = ledgerEntries::add,
        )
        selectionController.selectOnly(drawing.id)

        selectionController.beginLeftDrag(resizeHandleCenterFor(listOf(objectController.snapshot(drawing.id) as DrawingSnapshot)))
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = true)
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = false)

        assertThat(ledgerEntries.size, equalTo(1))
        assertThat(ledgerEntries.single() is EditObjectAction, equalTo(true))
    }

    @Test
    fun `multi drawing resize release commits a batch object edit`() {
        val objectController = objectController()
        val ledgerEntries = mutableListOf<LedgerEntry>()
        val first = drawingSnapshot("00000000-0000-0000-0000-000000000077")
        val second = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000078")),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.5, 0.5),
                end = MapDrawingPoint(0.6, 0.6),
                color = Color.RED,
            ),
        )
        objectController.createObject(first)
        objectController.createObject(second)
        val selectionController = selectionController(
            objectController = objectController,
            onLedgerEntry = ledgerEntries::add,
        )
        selectionController.beginLeftDrag(Vector2D(0.0, 0.0))
        selectionController.update(Vector2D(700.0, 700.0), leftHeld = false)

        selectionController.beginLeftDrag(resizeHandleCenterFor(listOf(first, second)))
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = true)
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = false)

        val batch = ledgerEntries.single() as BatchLedgerEntry
        assertThat(batch.entries.size, equalTo(2))
        assertThat(batch.entries.all { it is EditObjectAction }, equalTo(true))
    }

    @Test
    fun `cancel restores original resize snapshots`() {
        val objectController = objectController()
        val drawing = drawingSnapshot("00000000-0000-0000-0000-000000000073")
        objectController.createObject(drawing)
        val selectionController = selectionController(objectController = objectController)
        selectionController.selectOnly(drawing.id)

        selectionController.beginLeftDrag(resizeHandleCenterFor(listOf(objectController.snapshot(drawing.id) as DrawingSnapshot)))
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = true)
        selectionController.cancelTransientState()

        assertThat(objectController.snapshot(drawing.id), equalTo(drawing))
    }

    @Test
    fun `unit only selections do not expose resize handles`() {
        val objectController = objectController()
        val unit = unitSnapshot("00000000-0000-0000-0000-000000000074", destroyed = false)
        objectController.createObject(unit)
        val selectionController = selectionController(objectController = objectController)
        selectionController.selectOnly(unit.id)

        assertThat(selectionController.hasResizeHandleAt(Vector2D(447.0, 445.0)), equalTo(false))
    }

    @Test
    fun `mixed selection resize leaves unit unchanged and resizes drawing`() {
        val objectController = objectController()
        val unit = unitSnapshot("00000000-0000-0000-0000-000000000075", destroyed = false)
        val drawing = drawingSnapshot("00000000-0000-0000-0000-000000000076")
        objectController.createObject(unit)
        objectController.createObject(drawing)
        val storedUnit = objectController.snapshot(unit.id)
        val selectionController = selectionController(objectController = objectController)
        selectionController.beginLeftDrag(Vector2D(0.0, 0.0))
        selectionController.update(Vector2D(600.0, 600.0), leftHeld = false)

        selectionController.beginLeftDrag(
            resizeHandleCenterFor(
                listOf(
                    objectController.snapshot(unit.id) as UnitSnapshot,
                    objectController.snapshot(drawing.id) as DrawingSnapshot
                )
            )
        )
        selectionController.update(Vector2D(100.0, 100.0), leftHeld = true)

        val resizedDrawing = objectController.snapshot(drawing.id) as DrawingSnapshot
        assertThat(objectController.snapshot(unit.id), equalTo(storedUnit))
        assertThat((resizedDrawing.drawing as LineDrawing).start == (drawing.drawing as LineDrawing).start, equalTo(false))
    }

    @Test
    fun `resizing one graphic resizes every placed instance of its template around its centre`() {
        val graphicManager = graphicManager()
        val objectController = objectController(graphicManager)
        val templateId = GraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000130"))
        val otherTemplateId = GraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000131"))
        val template = MapViewerGraphicTemplate(templateId, "Graphic", Bitmap32(100, 100))
        val first = graphicSnapshot("00000000-0000-0000-0000-000000000132", templateId, template, 0.3, 0.3, 0.1)
        val second = graphicSnapshot("00000000-0000-0000-0000-000000000133", templateId, template, 0.7, 0.7, 0.2)
        val unrelated = graphicSnapshot(
            "00000000-0000-0000-0000-000000000134",
            otherTemplateId,
            MapViewerGraphicTemplate(otherTemplateId, "Other", Bitmap32(100, 100)),
            0.5,
            0.5,
            0.15,
        )
        listOf(first, second, unrelated).forEach(objectController::createObject)
        val resizedTemplates = mutableListOf<Triple<GraphicTemplateId, Double, Double>>()
        val selectionController = selectionController(
            objectController = objectController,
            onGraphicTemplateResized = { id, width, height ->
                resizedTemplates += Triple(id, width, height)
                null
            },
        )
        selectionController.selectOnly(first.id)

        selectionController.beginLeftDrag(resizeHandleCenterFor(listOf(first)))
        selectionController.update(Vector2D(500.0, 100.0), leftHeld = true)

        val resizedFirst = objectController.snapshot(first.id) as GraphicSnapshot
        val resizedSecond = objectController.snapshot(second.id) as GraphicSnapshot
        assertThat(resizedFirst.normalizedWidth, equalTo(resizedSecond.normalizedWidth))
        assertThat(resizedFirst.normalizedHeight, equalTo(resizedSecond.normalizedHeight))
        assertThat(resizedFirst.mapCoordinate, equalTo(first.mapCoordinate))
        assertThat(resizedSecond.mapCoordinate, equalTo(second.mapCoordinate))
        assertThat(objectController.snapshot(unrelated.id), equalTo(unrelated))

        selectionController.update(Vector2D(500.0, 100.0), leftHeld = false)

        assertThat(
            resizedTemplates,
            equalTo(listOf(Triple(templateId, resizedFirst.normalizedWidth, resizedFirst.normalizedHeight))),
        )
    }

    @Test
    fun `new graphic inherits its template size`() {
        val objectController = objectController(graphicManager())
        val templateId = GraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000135"))
        val template = GraphicTemplateSnapshot(
            id = templateId,
            template = MapViewerGraphicTemplate(templateId, "Graphic", Bitmap32(100, 50))
                .withNormalizedSize(width = 0.08, height = 0.04),
            index = 0,
        )

        val created = objectController.createGraphic(template, MapCoordinate(0.5, 0.5))

        assertThat(created.normalizedWidth, equalTo(0.08))
        assertThat(created.normalizedHeight, equalTo(0.04))
    }

    @Test
    fun `single unit pivot rotation updates facing and keeps position fixed`() {
        val objectController = objectController()
        val unit = unitSnapshot("00000000-0000-0000-0000-000000000087", destroyed = false, x = 0.5, y = 0.5)
        objectController.createObject(unit)
        val storedUnit = objectController.snapshot(unit.id) as UnitSnapshot
        val selectionController = selectionController(objectController = objectController)
        selectionController.selectOnly(unit.id)

        val handle = pivotHandleForSelection(listOf(storedUnit), MapCoordinateSpace(GEOMETRY, 1000, 1000), viewport()) ?: error("missing handle")
        val pivot = MapSelectionPivot.pivotCenter(listOf(storedUnit), MapCoordinateSpace(GEOMETRY, 1000, 1000))
        val startAngle = MapSelectionPivot.bearingAngle(
            Vector2D(pivot.x * 1000.0, pivot.y * 1000.0),
            handle.center,
        )
        val target = pointFromBearingAngle(
            center = Vector2D(pivot.x * 1000.0, pivot.y * 1000.0),
            radius = handle.radius,
            bearingRadians = startAngle + (PI / 2.0),
        )

        selectionController.beginLeftDrag(handle.center)
        selectionController.update(target, leftHeld = true)
        selectionController.update(target, leftHeld = false)

        val rotated = objectController.snapshot(unit.id) as UnitSnapshot
        assertThat(rotated.facingDegrees, equalTo(90))
        assertThat(rotated.mapCoordinate.x, closeTo(storedUnit.mapCoordinate.x, 0.0001))
        assertThat(rotated.mapCoordinate.y, closeTo(storedUnit.mapCoordinate.y, 0.0001))
    }

    @Test
    fun `multi unit pivot rotation updates positions around centroid and bearings`() {
        val objectController = objectController()
        val north = unitSnapshot("00000000-0000-0000-0000-000000000088", destroyed = false, x = 0.5, y = 0.4, facing = 10)
        val south = unitSnapshot("00000000-0000-0000-0000-000000000089", destroyed = false, x = 0.5, y = 0.6)
        objectController.createObject(north)
        objectController.createObject(south)
        val storedNorth = objectController.snapshot(north.id) as UnitSnapshot
        val storedSouth = objectController.snapshot(south.id) as UnitSnapshot
        val selectionController = selectionController(objectController = objectController)
        selectionController.beginLeftDrag(Vector2D(0.0, 0.0))
        selectionController.update(Vector2D(700.0, 700.0), leftHeld = false)

        val selected = listOf(storedNorth, storedSouth)
        val handle = pivotHandleForSelection(selected, MapCoordinateSpace(GEOMETRY, 1000, 1000), viewport()) ?: error("missing handle")
        val pivot = MapSelectionPivot.pivotCenter(selected, MapCoordinateSpace(GEOMETRY, 1000, 1000))
        val startAngle = MapSelectionPivot.bearingAngle(
            Vector2D(pivot.x * 1000.0, pivot.y * 1000.0),
            handle.center,
        )
        val target = pointFromBearingAngle(
            center = Vector2D(pivot.x * 1000.0, pivot.y * 1000.0),
            radius = handle.radius,
            bearingRadians = startAngle + (PI / 2.0),
        )

        selectionController.beginLeftDrag(handle.center)
        selectionController.update(target, leftHeld = true)
        selectionController.update(target, leftHeld = false)

        val rotatedNorth = objectController.snapshot(north.id) as UnitSnapshot
        val rotatedSouth = objectController.snapshot(south.id) as UnitSnapshot
        assertThat(rotatedNorth.facingDegrees, equalTo(100))
        assertThat(rotatedNorth.mapCoordinate.x, closeTo(0.6, 0.0001))
        assertThat(rotatedNorth.mapCoordinate.y, closeTo(0.5, 0.0001))
        assertThat(rotatedSouth.facingDegrees, equalTo(90))
        assertThat(rotatedSouth.mapCoordinate.x, closeTo(0.4, 0.0001))
        assertThat(rotatedSouth.mapCoordinate.y, closeTo(0.5, 0.0001))
    }

    @Test
    fun `shift rotate snaps to the nearest quarter turn`() {
        val objectController = objectController()
        val drawing = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000086")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.4, 0.45),
                end = MapDrawingPoint(0.6, 0.55),
                color = Color.RED,
                rotationDegrees = 10.0,
            ),
        )
        objectController.createObject(drawing)
        val selectionController = selectionController(objectController = objectController)
        selectionController.selectOnly(drawing.id)

        val handle = pivotHandleForSelection(listOf(drawing), MapCoordinateSpace(GEOMETRY, 1000, 1000), viewport()) ?: error("missing handle")
        val pivot = MapSelectionPivot.pivotCenter(listOf(drawing), MapCoordinateSpace(GEOMETRY, 1000, 1000))
        val startAngle = MapSelectionPivot.bearingAngle(
            Vector2D(pivot.x * 1000.0, pivot.y * 1000.0),
            handle.center,
        )
        val target = pointFromBearingAngle(
            center = Vector2D(pivot.x * 1000.0, pivot.y * 1000.0),
            radius = handle.radius,
            bearingRadians = startAngle + (PI / 4.0),
        )

        selectionController.beginLeftDrag(handle.center, shiftHeld = true)
        selectionController.update(target, leftHeld = true, shiftHeld = true)

        val rotated = objectController.snapshot(drawing.id) as DrawingSnapshot
        val snappedDegrees = (rotated.drawing as RectangleDrawing).rotationDegrees
        assertThat(snappedDegrees, equalTo(45.0))
    }

    private fun selectionController(
        objectController: MapObjectController,
        objectAt: (Vector2D) -> MapPlacedObjectSelection? = { null },
        onLedgerEntry: (LedgerEntry) -> Unit = {},
        onGraphicTemplateResized: (GraphicTemplateId, Double, Double) -> LedgerEntry? = { _, _, _ -> null },
    ) = MapSelectionController(
        coordinateSpace = MapCoordinateSpace(GEOMETRY, mapWidth = 1000, mapHeight = 1000),
        viewportProvider = ::viewport,
        objectController = objectController,
        contextMenuManager = ContextMenuManager(Container()),
        objectAt = objectAt,
        onStateChanged = {},
        onLedgerEntry = onLedgerEntry,
        onGraphicTemplateResized = onGraphicTemplateResized,
    )

    private fun objectController(graphicManager: MapPlacedGraphicManager? = null): MapObjectController =
        MapObjectController(
            unitManager = MapPlacedUnitManager(
                root = Container(),
                unitLayer = Container(),
                geometry = GEOMETRY,
                mapUnitsPerPixel = 1.0,
                mapWidth = 1000,
                mapHeight = 1000,
                viewportProvider = ::viewport,
                contextMenuManager = ContextMenuManager(Container()),
                deferredActionRunner = ContextMenuDeferredActionRunner(Container()),
                dialogManager = MapViewerDialogManager(),
            ),
            graphicManager = graphicManager,
            drawingStore = MapDrawingStore(),
            uuidGenerator = ::nextTestUuid,
        )

    private fun graphicManager() =
        MapPlacedGraphicManager(
            root = Container(),
            graphicLayer = Container(),
            geometry = GEOMETRY,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportProvider = ::viewport,
            contextMenuManager = ContextMenuManager(Container()),
        )

    private fun graphicSnapshot(
        id: String,
        templateId: GraphicTemplateId,
        template: MapViewerGraphicTemplate,
        x: Double,
        y: Double,
        size: Double,
    ) = GraphicSnapshot(
        id = MapPlacedObjectId(UUID.fromString(id)),
        graphicTemplateId = templateId,
        template = template,
        instanceName = template.name,
        mapCoordinate = MapCoordinate(x, y),
        normalizedWidth = size,
        normalizedHeight = size,
    )

    private fun drawingSnapshot(id: String) =
        DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            drawing = LineDrawing(
                start = MapDrawingPoint(0.2, 0.2),
                end = MapDrawingPoint(0.4, 0.4),
                color = Color.RED,
            ),
        )

    private fun unitSnapshot(id: String, destroyed: Boolean, x: Double = 0.5, y: Double = 0.5, facing: Int? = null) =
        UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000061")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000061")),
                name = "Unit",
                bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
            ),
            instanceName = "Unit",
            destroyed = destroyed,
            facingDegrees = facing,
            mapCoordinate = MapCoordinate(x, y),
            normalizedWidth = 0.1,
            normalizedHeight = 0.1,
        )

    private fun assertPoint(point: MapDrawingPoint, x: Double, y: Double) {
        assertThat(point.x, closeTo(x, 0.0001))
        assertThat(point.y, closeTo(y, 0.0001))
    }

    private fun resizeHandleCenterFor(snapshots: List<MapPlacedObjectSnapshot>): Vector2D {
        val coordinateSpace = MapCoordinateSpace(GEOMETRY, mapWidth = 1000, mapHeight = 1000)
        val frame = when (snapshots.size) {
            0 -> error("no snapshots")
            1 -> snapshots.single().selectionFrame(coordinateSpace, viewport(), SELECTION_FRAME_SCREEN_PADDING)
            else -> snapshots.groupSelectionFrame(coordinateSpace, viewport()) ?: error("missing group frame")
        }
        val bracket = frame.cornerBrackets(SELECTION_BRACKET_LENGTH_PX, SELECTION_BRACKET_THICKNESS_PX)
            .first { it.corner == ScreenSelectionCorner.TOP_RIGHT }
        val minX = bracket.points.minOf { it.x }
        val maxX = bracket.points.maxOf { it.x }
        val minY = bracket.points.minOf { it.y }
        val maxY = bracket.points.maxOf { it.y }
        for (yStep in 0..8) {
            val y = minY + ((maxY - minY) * yStep / 8.0)
            for (xStep in 0..8) {
                val x = minX + ((maxX - minX) * xStep / 8.0)
                val candidate = Vector2D(x, y)
                if (bracket.contains(candidate)) {
                    return candidate
                }
            }
        }
        return Vector2D(
            x = bracket.points.map { it.x }.average(),
            y = bracket.points.map { it.y }.average(),
        )
    }

    private fun pointFromBearingAngle(center: Vector2D, radius: Double, bearingRadians: Double): Vector2D =
        Vector2D(
            x = center.x + sin(bearingRadians) * radius,
            y = center.y - cos(bearingRadians) * radius,
        )

    private companion object {
        val GEOMETRY = ViewerGeometry(
            backgroundWidth = 1000,
            backgroundHeight = 1000,
            mapOffsetX = 0,
            mapOffsetY = 0,
        )

        fun viewport(): CameraViewport =
            CameraViewport.centeredOnMap(
                geometry = GEOMETRY,
                mapWidth = 1000,
                mapHeight = 1000,
                viewportWidth = 1000,
                viewportHeight = 1000,
                zoom = 1.0,
            )
    }
}
