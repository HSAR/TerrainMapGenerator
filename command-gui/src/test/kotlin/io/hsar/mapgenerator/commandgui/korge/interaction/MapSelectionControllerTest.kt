package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapSelectionControllerTest {
    @Test
    fun `box selection ignores objects rejected by the selectable predicate`() {
        val unitManager = MapPlacedUnitManager(
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
        )
        val objectController = MapObjectController(
            unitManager = unitManager,
            drawingStore = MapDrawingStore(),
        )
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
