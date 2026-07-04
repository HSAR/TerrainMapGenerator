package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapPlacedUnitManagerTest {
    @Test
    fun `replacing a unit template refreshes the live unit affiliation`() {
        val root = Container()
        val unitLayer = root.container()
        val manager = MapPlacedUnitManager(
            root = root,
            unitLayer = unitLayer,
            geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
            mapUnitsPerPixel = 1.0,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportProvider = {
                CameraViewport.centeredOnMap(
                    geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
                    mapWidth = 1000,
                    mapHeight = 1000,
                    viewportWidth = 1000,
                    viewportHeight = 1000,
                    zoom = 1.0,
                )
            },
            contextMenuManager = ContextMenuManager(root),
            deferredActionRunner = ContextMenuDeferredActionRunner(root),
            dialogManager = MapViewerDialogManager(),
        )
        val templateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
        val unitId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000012"))
        val initialTemplate = MapViewerUnitTemplate(
            id = templateId,
            name = "Unit",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
            affiliation = UnitAffiliation.Friend,
        )
        val snapshot = manager.buildSnapshot(
            id = unitId,
            unitTemplateId = templateId,
            template = initialTemplate,
            instanceName = "Unit",
            mapCoordinate = MapCoordinate(0.5, 0.5),
        )

        manager.upsert(snapshot)
        manager.replaceUnitTemplate(
            UnitTemplateSnapshot(
                id = templateId,
                template = initialTemplate.copy(affiliation = UnitAffiliation.Hostile),
                index = 0,
            ),
        )

        assertThat(manager.snapshot(unitId)?.template?.affiliation, equalTo(UnitAffiliation.Hostile))
    }

    @Test
    fun `placed unit icons enable smoothing on the map`() {
        val root = Container()
        val unitLayer = root.container()
        val manager = MapPlacedUnitManager(
            root = root,
            unitLayer = unitLayer,
            geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
            mapUnitsPerPixel = 1.0,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportProvider = {
                CameraViewport.centeredOnMap(
                    geometry = ViewerGeometry(backgroundWidth = 1660, backgroundHeight = 1660, mapOffsetX = 330, mapOffsetY = 330),
                    mapWidth = 1000,
                    mapHeight = 1000,
                    viewportWidth = 1000,
                    viewportHeight = 1000,
                    zoom = 1.0,
                )
            },
            contextMenuManager = ContextMenuManager(root),
            deferredActionRunner = ContextMenuDeferredActionRunner(root),
            dialogManager = MapViewerDialogManager(),
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000021")),
            name = "Unit",
            bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
        )

        manager.upsert(
            manager.buildSnapshot(
                id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000022")),
                unitTemplateId = template.id,
                template = template,
                instanceName = "Unit",
                mapCoordinate = MapCoordinate(0.0, 0.0),
            ),
        )

        assertThat(unitLayer.images().single().smoothing, equalTo(true))
    }

    @Test
    fun `placed unit icon width follows epsilon reference units`() {
        val manager = MapPlacedUnitManager(
            root = Container(),
            unitLayer = Container(),
            geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
            mapUnitsPerPixel = 0.5,
            mapWidth = 1000,
            mapHeight = 1000,
            gridLayer = GridLayer(
                mapId = "EPSILON",
                referenceSystem = ReferenceSystem.EPSILON,
                gridScale = ReferenceSystem.EPSILON.gridScale,
                unitsPerPixel = 0.5,
                detailLevel = 2,
                pixelsPerLine = 40.0,
                verticalLineNumberStart = EpsilonCoord.fromUnits(0),
                horizontalLineNumberStart = EpsilonCoord.fromUnits(0),
                verticalLineCount = 10,
                horizontalLineCount = 10,
            ),
            viewportProvider = {
                CameraViewport.centeredOnMap(
                    geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
                    mapWidth = 1000,
                    mapHeight = 1000,
                    viewportWidth = 1000,
                    viewportHeight = 1000,
                    zoom = 1.0,
                )
            },
            contextMenuManager = ContextMenuManager(Container()),
            deferredActionRunner = ContextMenuDeferredActionRunner(Container()),
            dialogManager = MapViewerDialogManager(),
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000031")),
            name = "Unit",
            bitmap = Bitmap32(10, 20) { _, _ -> RGBA(255, 0, 0, 255) },
        )

        val snapshot = manager.buildSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000032")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Unit",
            mapCoordinate = MapCoordinate(0.5, 0.5),
        )

        assertThat(snapshot.normalizedWidth, closeTo(0.004, 0.0000001))
        assertThat(snapshot.normalizedHeight, closeTo(0.008, 0.0000001))
    }

    @Test
    fun `upsert recalculates unit bounds from epsilon icon size`() {
        val root = Container()
        val unitLayer = root.container()
        val manager = MapPlacedUnitManager(
            root = root,
            unitLayer = unitLayer,
            geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
            mapUnitsPerPixel = 0.5,
            mapWidth = 1000,
            mapHeight = 1000,
            gridLayer = GridLayer(
                mapId = "EPSILON",
                referenceSystem = ReferenceSystem.EPSILON,
                gridScale = ReferenceSystem.EPSILON.gridScale,
                unitsPerPixel = 0.5,
                detailLevel = 2,
                pixelsPerLine = 40.0,
                verticalLineNumberStart = EpsilonCoord.fromUnits(0),
                horizontalLineNumberStart = EpsilonCoord.fromUnits(0),
                verticalLineCount = 10,
                horizontalLineCount = 10,
            ),
            viewportProvider = {
                CameraViewport.centeredOnMap(
                    geometry = ViewerGeometry(backgroundWidth = 1000, backgroundHeight = 1000, mapOffsetX = 0, mapOffsetY = 0),
                    mapWidth = 1000,
                    mapHeight = 1000,
                    viewportWidth = 1000,
                    viewportHeight = 1000,
                    zoom = 1.0,
                )
            },
            contextMenuManager = ContextMenuManager(root),
            deferredActionRunner = ContextMenuDeferredActionRunner(root),
            dialogManager = MapViewerDialogManager(),
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000041")),
            name = "Unit",
            bitmap = Bitmap32(10, 20) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val unitId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000042"))

        manager.upsert(
            UnitSnapshot(
                id = unitId,
                unitTemplateId = template.id,
                template = template,
                instanceName = "Unit",
                destroyed = false,
                facingDegrees = null,
                mapCoordinate = MapCoordinate(0.5, 0.5),
                normalizedWidth = 0.25,
                normalizedHeight = 0.25,
            ),
        )

        assertThat(manager.snapshot(unitId)?.normalizedWidth, closeTo(0.004, 0.0000001))
        assertThat(manager.snapshot(unitId)?.normalizedHeight, closeTo(0.008, 0.0000001))
    }

    private fun Container.images(): List<Image> =
        children.flatMap { child ->
            listOfNotNull(child as? Image) + ((child as? Container)?.images().orEmpty())
        }
}
