package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.persistence.TacticalFireKey
import io.hsar.mapgenerator.commandgui.korge.persistence.TacticalFireUsage
import io.hsar.mapgenerator.commandgui.korge.persistence.TacticalTurnMarker
import io.hsar.mapgenerator.commandgui.korge.ui.DestroyedUnitsDisplay
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogManager
import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.container
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*
import java.util.Collections.emptyMap

class MapPlacedUnitManagerTest {
    @Test
    fun `consuming an ECM solution persists module use and activates the fitted field`() {
        val activations = mutableListOf<EcmActivation>()
        val usage = mutableMapOf<TacticalFireKey, TacticalFireUsage>()
        val root = Container()
        val manager = MapPlacedUnitManager(
            root = root,
            unitLayer = root.container(),
            geometry = CENTERED_GEOMETRY,
            mapUnitsPerPixel = 1.0,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportProvider = {
                CameraViewport.centeredOnMap(CENTERED_GEOMETRY, 1000, 1000, 1000, 1000, zoom = 1.0)
            },
            contextMenuManager = ContextMenuManager(root),
            deferredActionRunner = ContextMenuDeferredActionRunner(root),
            dialogManager = MapViewerDialogManager(),
            tacticalFireUsageProvider = { usage[it] ?: TacticalFireUsage() },
            onTacticalFireUsageChanged = { key, value -> usage[key] = value },
            ecmActivationsProvider = { turn -> activations.filter { it.turnNumber == turn } },
            onEcmActivation = activations::add,
        )
        val definition = OlympiadDawnShip.parse(
            """{"name":"ECM Ship","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"ecm","id":"ecm-1"}]}""",
        )
        val sourceTemplate = MapViewerUnitTemplate(
            UnitTemplateId(UUID(0, 70)), "Source", Bitmap32(4, 4), affiliation = UnitAffiliation.Friend,
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )
        val targetDefinition = OlympiadDawnShip.parse(
            """{"name":"Target","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[]}""",
        )
        val targetTemplate = MapViewerUnitTemplate(
            UnitTemplateId(UUID(0, 71)), "Target", Bitmap32(4, 4), affiliation = UnitAffiliation.Hostile,
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), targetDefinition),
        )
        val sourceId = MapPlacedObjectId(UUID(0, 72))
        val targetId = MapPlacedObjectId(UUID(0, 73))
        manager.upsert(manager.buildSnapshot(sourceId, sourceTemplate.id, sourceTemplate, "Source", MapCoordinate(0.5, 0.5), facingDegrees = 0))
        manager.upsert(manager.buildSnapshot(targetId, targetTemplate.id, targetTemplate, "Target", MapCoordinate(0.5, 0.498), facingDegrees = 0))

        assertThat(manager.consumeTacticalSolution(sourceId, targetId), equalTo(true))

        assertThat(activations.single().moduleKeys, equalTo(setOf("ecm-ecm-1")))
        assertThat(activations.single().intensity, equalTo(3))
        assertThat(usage.getValue(TacticalFireKey(1, sourceId)).firedWeapons, equalTo(setOf("ecm-ecm-1")))
        assertThat(
            TacticalTurnMarker.usage(manager.snapshot(sourceId)!!.customProperties, 1),
            equalTo(TacticalFireUsage(setOf("ecm-ecm-1"), 0)),
        )
    }

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
    fun `replacing a unit template supplies ship properties without replacing instance damage`() {
        val manager = unitManager(unitLayer = Container(), destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val templateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000013"))
        val unitId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000014"))
        val initialTemplate = MapViewerUnitTemplate(
            id = templateId,
            name = "Unit",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
            affiliation = UnitAffiliation.Friend,
        )
        val damage = "{\"destroyedHull\":[1]}"

        manager.upsert(
            manager.buildSnapshot(
                id = unitId,
                unitTemplateId = templateId,
                template = initialTemplate,
                instanceName = "Unit",
                mapCoordinate = MapCoordinate(0.5, 0.5),
                customProperties = mapOf(OlympiadDawnShip.DAMAGE_KEY to damage),
            ),
        )
        manager.replaceUnitTemplate(
            UnitTemplateSnapshot(
                id = templateId,
                template = initialTemplate.copy(
                    affiliation = UnitAffiliation.Hostile,
                    customProperties = mapOf(OlympiadDawnShip.DEFINITION_KEY to "ship-definition"),
                ),
                index = 0,
            ),
        )

        val updated = manager.snapshot(unitId)!!
        assertThat(updated.template.affiliation, equalTo(UnitAffiliation.Hostile))
        assertThat(updated.template.customProperties[OlympiadDawnShip.DEFINITION_KEY], equalTo("ship-definition"))
        assertThat(updated.customProperties[OlympiadDawnShip.DAMAGE_KEY], equalTo(damage))
    }

    @Test
    fun `restored units receive missing damage state from their current imported type`() {
        val manager = unitManager(unitLayer = Container(), destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val templateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000019"))
        val originalTemplate = MapViewerUnitTemplate(
            id = templateId,
            name = "Ship",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Heavy Cruiser","hull":{"points":1,"rows":1},"armour":[[1]],"systems":[]}""",
        )
        val importedTemplate = originalTemplate.copy(
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )
        val unitId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000020"))

        manager.restoreAll(
            listOf(
                manager.buildSnapshot(unitId, templateId, originalTemplate, "Ship", MapCoordinate(0.5, 0.5)),
            ),
        )
        manager.replaceUnitTemplate(UnitTemplateSnapshot(templateId, importedTemplate, 0))

        assertThat(manager.snapshot(unitId)?.customProperties?.containsKey(OlympiadDawnShip.DAMAGE_KEY), equalTo(true))
    }

    @Test
    fun `changing a selected template affiliation refreshes filtered fire arcs`() {
        val manager = unitManager(unitLayer = Container(), destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val selectedTemplate = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000015")),
            name = "Selected",
            bitmap = Bitmap32(4, 4) { _, _ -> RGBA(255, 0, 0, 255) },
            affiliation = UnitAffiliation.Friend,
        )
        val otherTemplate = selectedTemplate.copy(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000016")),
            name = "Other",
        )
        val selectedId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000017"))

        manager.upsert(manager.buildSnapshot(selectedId, selectedTemplate.id, selectedTemplate, "Selected", MapCoordinate(0.4, 0.5), facingDegrees = 0))
        manager.upsert(
            manager.buildSnapshot(
                MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000018")),
                otherTemplate.id,
                otherTemplate,
                "Other",
                MapCoordinate(0.6, 0.5),
                facingDegrees = 0
            )
        )
        manager.updateFireArcDisplayMode(FireArcDisplayMode.FriendliesOfSelected)
        manager.updateSelectedUnits(setOf(selectedId))
        manager.resetVisualUpdateStats()

        manager.replaceUnitTemplate(UnitTemplateSnapshot(selectedTemplate.id, selectedTemplate.copy(affiliation = UnitAffiliation.Hostile), 0))

        assertThat(manager.visualUpdateStats().facingGeometryRebuilds, equalTo(3))
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
    fun `placed unit icon width uses fixed screen pixels at current zoom`() {
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

        assertThat(snapshot.normalizedWidth, closeTo(0.032, 0.0000001))
        assertThat(snapshot.normalizedHeight, closeTo(0.064, 0.0000001))
    }

    @Test
    fun `upsert recalculates unit bounds from fixed screen icon size`() {
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

        assertThat(manager.snapshot(unitId)?.normalizedWidth, closeTo(0.032, 0.0000001))
        assertThat(manager.snapshot(unitId)?.normalizedHeight, closeTo(0.064, 0.0000001))
    }

    @Test
    fun `unit icon world bounds shrink as zoom increases to keep screen size stable`() {
        val root = Container()
        val unitLayer = root.container()
        var viewport = CameraViewport.centeredOnMap(
            geometry = CENTERED_GEOMETRY,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportWidth = 1000,
            viewportHeight = 1000,
            zoom = 1.0,
        )
        val manager = MapPlacedUnitManager(
            root = root,
            unitLayer = unitLayer,
            geometry = CENTERED_GEOMETRY,
            mapUnitsPerPixel = 1.0,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportProvider = { viewport },
            contextMenuManager = ContextMenuManager(root),
            deferredActionRunner = ContextMenuDeferredActionRunner(root),
            dialogManager = MapViewerDialogManager(),
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000061")),
            name = "Unit",
            bitmap = Bitmap32(16, 16) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val unitId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000062"))
        manager.upsert(
            manager.buildSnapshot(
                id = unitId,
                unitTemplateId = template.id,
                template = template,
                instanceName = "Unit",
                mapCoordinate = MapCoordinate(0.5, 0.5),
            ),
        )

        val zoomOneWidth = manager.snapshot(unitId)!!.normalizedWidth
        viewport = viewport.copy(zoom = 2.0)
        manager.refreshIconDisplay(force = true)

        assertThat(zoomOneWidth * 1000.0 * 1.0, closeTo(32.0, 0.0000001))
        assertThat(manager.snapshot(unitId)!!.normalizedWidth * 1000.0 * 2.0, closeTo(32.0, 0.0000001))
    }

    @Test
    fun `updating unit icon size changes existing unit display bounds`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val unit = destroyedUnitSnapshot().copy(destroyed = false)

        manager.upsert(unit)
        manager.updateUnitIconSize(48.0)

        assertThat(manager.snapshot(unit.id)?.normalizedWidth, closeTo(0.048, 0.0000001))
    }

    @Test
    fun `full destroyed display leaves destroyed units visible and selectable`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)

        manager.upsert(destroyedUnitSnapshot())

        val image = unitLayer.images().single()
        assertThat(image.alpha, equalTo(1.0))
        assertThat(image.parent?.visible, equalTo(true))
        assertThat(manager.selectionAt(CENTER_SCREEN_POINT)?.kind, equalTo(MapPlacedObjectSelectionKind.UNIT))
        assertThat(manager.hasDragCandidate(CENTER_SCREEN_POINT), equalTo(true))
    }

    @Test
    fun `partial destroyed display fades only the icon and blocks movement while allowing context menus`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Partial)

        manager.upsert(destroyedUnitSnapshot())

        val image = unitLayer.images().single()
        assertThat(image.alpha, equalTo(0.5))
        assertThat(image.parent?.visible, equalTo(true))
        assertThat(manager.selectionAt(CENTER_SCREEN_POINT), equalTo(null))
        assertThat(manager.hasDragCandidate(CENTER_SCREEN_POINT), equalTo(false))
        assertThat(manager.openContextMenuAt(CENTER_SCREEN_POINT), equalTo(true))
    }

    @Test
    fun `hide destroyed display hides destroyed units and blocks all unit interaction`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Hide)

        manager.upsert(destroyedUnitSnapshot())

        val image = unitLayer.images().single()
        assertThat(image.parent?.visible, equalTo(false))
        assertThat(manager.selectionAt(CENTER_SCREEN_POINT), equalTo(null))
        assertThat(manager.hasDragCandidate(CENTER_SCREEN_POINT), equalTo(false))
        assertThat(manager.openContextMenuAt(CENTER_SCREEN_POINT), equalTo(false))
    }

    @Test
    fun `changing destroyed display updates existing unit visuals`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)

        manager.upsert(destroyedUnitSnapshot())
        manager.updateDestroyedUnitsDisplay(DestroyedUnitsDisplay.Partial)

        val image = unitLayer.images().single()
        assertThat(image.alpha, equalTo(0.5))
        assertThat(image.parent?.visible, equalTo(true))

        manager.updateDestroyedUnitsDisplay(DestroyedUnitsDisplay.Hide)

        assertThat(image.parent?.visible, equalTo(false))
    }

    @Test
    fun `facing previews reuse the unit visual`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000061")),
            name = "Facing",
            bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val snapshot = manager.buildSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000062")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Facing",
            mapCoordinate = MapCoordinate(0.5, 0.5),
            facingDegrees = 0,
        )

        manager.upsert(snapshot)
        manager.updateSelectedUnits(setOf(snapshot.id))
        manager.resetVisualUpdateStats()

        manager.preview(snapshot.copy(facingDegrees = 90))
        manager.preview(snapshot.copy(facingDegrees = 180))
        manager.updateSelectedUnits(setOf(snapshot.id))

        assertThat(manager.visualUpdateStats(), equalTo(UnitVisualUpdateStats(0, 2)))
        assertThat(unitLayer.images().size, equalTo(1))
    }

    @Test
    fun `unit ship data changes refresh facing geometry without rebuilding the icon`() {
        val manager = unitManager(unitLayer = Container(), destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000065")),
            name = "Ship",
            bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val snapshot = manager.buildSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000066")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Ship",
            mapCoordinate = MapCoordinate(0.5, 0.5),
            facingDegrees = 0,
        )

        manager.upsert(snapshot)
        manager.resetVisualUpdateStats()
        manager.preview(snapshot.copy(customProperties = mapOf(OlympiadDawnShip.DEFINITION_KEY to "ship-definition")))

        assertThat(manager.visualUpdateStats(), equalTo(UnitVisualUpdateStats(0, 1)))
    }

    @Test
    fun `template changes invalidate cached unit visuals`() {
        val unitLayer = Container()
        val manager = unitManager(unitLayer = unitLayer, destroyedUnitsDisplay = DestroyedUnitsDisplay.Full)
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000063")),
            name = "Template",
            bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        val snapshot = manager.buildSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000064")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Template",
            mapCoordinate = MapCoordinate(0.5, 0.5),
            facingDegrees = 0,
        )

        manager.upsert(snapshot)
        manager.resetVisualUpdateStats()
        manager.upsert(snapshot.copy(template = template.copy(affiliation = UnitAffiliation.Hostile)))

        assertThat(manager.visualUpdateStats().fullVisualRebuilds, equalTo(1))
        assertThat(manager.visualUpdateStats().facingGeometryRebuilds, equalTo(1))
    }

    private fun Container.images(): List<Image> =
        children.flatMap { child ->
            listOfNotNull(child as? Image) + ((child as? Container)?.images().orEmpty())
        }

    private fun unitManager(
        unitLayer: Container,
        destroyedUnitsDisplay: DestroyedUnitsDisplay,
    ): MapPlacedUnitManager =
        MapPlacedUnitManager(
            root = Container(),
            unitLayer = unitLayer,
            geometry = CENTERED_GEOMETRY,
            mapUnitsPerPixel = 1.0,
            mapWidth = 1000,
            mapHeight = 1000,
            viewportProvider = {
                CameraViewport.centeredOnMap(
                    geometry = CENTERED_GEOMETRY,
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
            initialDestroyedUnitsDisplay = destroyedUnitsDisplay,
        )

    private fun destroyedUnitSnapshot(): UnitSnapshot {
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000051")),
            name = "Destroyed",
            bitmap = Bitmap32(64, 64) { _, _ -> RGBA(255, 0, 0, 255) },
        )
        return UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000052")),
            unitTemplateId = template.id,
            template = template,
            instanceName = "Destroyed",
            destroyed = true,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.5, 0.5),
            normalizedWidth = 0.1,
            normalizedHeight = 0.1,
        )
    }

    private companion object {
        val CENTER_SCREEN_POINT = Vector2D(500.0, 500.0)
        val CENTERED_GEOMETRY = ViewerGeometry(
            backgroundWidth = 1000,
            backgroundHeight = 1000,
            mapOffsetX = 0,
            mapOffsetY = 0,
        )
    }
}
