package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.container
import org.hamcrest.MatcherAssert.assertThat
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

    private fun Container.images(): List<Image> =
        children.flatMap { child ->
            listOfNotNull(child as? Image) + ((child as? Container)?.images().orEmpty())
        }
}
