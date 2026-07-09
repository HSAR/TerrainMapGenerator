package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class NavrepDetectionTest {

    @Test
    fun `navrep contact detection uses the target class range and distance`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000001",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val nearbyFF = unit(
            id = "00000000-0000-0000-0000-000000000002",
            instanceName = "Escort",
            unitClass = "FF",
            x = 0.505,
            y = 0.5,
        )
        val farCL = unit(
            id = "00000000-0000-0000-0000-000000000003",
            instanceName = "Cruiser",
            unitClass = "CL",
            x = 0.513,
            y = 0.5,
        )

        val contacts = NavrepDetection.visibleContacts(
            observer = observer,
            candidates = listOf(observer, nearbyFF, farCL),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        )

        assertThat(contacts.map { it.unit.id }, equalTo(listOf(nearbyFF.id)))
        assertThat(contacts.single().distanceUnits, closeTo(5.0, 0.0001))
        assertThat(contacts.single().detectionRangeUnits, equalTo(6.0))
    }

    @Test
    fun `navrep contact detection respects sensor modifiers from custom properties`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000010",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val activeSensorsContact = unit(
            id = "00000000-0000-0000-0000-000000000011",
            instanceName = "Runner",
            unitClass = "CL",
            x = 0.52,
            y = 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val silentContact = unit(
            id = "00000000-0000-0000-0000-000000000012",
            instanceName = "Sneak",
            unitClass = "CA",
            x = 0.52,
            y = 0.5,
            customProperties = mapOf("silentRunning" to "true"),
        )

        val contacts = NavrepDetection.visibleContacts(
            observer = observer,
            candidates = listOf(observer, activeSensorsContact, silentContact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        )

        assertThat(contacts.map { it.unit.id }, equalTo(listOf(activeSensorsContact.id)))
        assertThat(contacts.single().detectionRangeUnits, equalTo(24.0))
    }

    @Test
    fun `navrep labels use the unit class when present`() {
        val snapshot = unit(
            id = "00000000-0000-0000-0000-000000000020",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )

        assertThat(snapshot.isNavrepUnit(), equalTo(true))
        assertThat(snapshot.navrepMenuLabel(), equalTo("FF 1 [FF]"))
    }

    @Test
    fun `navrep labels fall back to the template class when the unit has no class`() {
        val snapshot = unit(
            id = "00000000-0000-0000-0000-000000000021",
            instanceName = "FF 2",
            unitClass = null,
            x = 0.5,
            y = 0.5,
            templateUnitClass = "FF",
        )

        assertThat(snapshot.isNavrepUnit(), equalTo(true))
        assertThat(snapshot.navrepMenuLabel(), equalTo("FF 2 [FF]"))
    }

    @Test
    fun `navrep report includes visible and hidden checks`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000030",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val report = NavrepDetection.visibilityReport(
            observer = observer,
            candidates = listOf(
                observer,
                unit(
                    id = "00000000-0000-0000-0000-000000000031",
                    instanceName = "Escort",
                    unitClass = "FF",
                    x = 0.505,
                    y = 0.5,
                ),
                unit(
                    id = "00000000-0000-0000-0000-000000000032",
                    instanceName = "Cruiser",
                    unitClass = "CL",
                    x = 0.513,
                    y = 0.5,
                ),
            ),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            gridLayer = epsilonGridLayer(),
        )

        assertThat(report.startsWith("Position "), equalTo(true))
        assertThat(report.contains("FF Escort has position"), equalTo(true))
        assertThat(report.contains("[HIDDEN]"), equalTo(true))
        assertThat(report.contains("- CL Cruiser detection range is"), equalTo(true))
    }

    private fun unit(
        id: String,
        instanceName: String,
        unitClass: String?,
        x: Double,
        y: Double,
        customProperties: Map<String, String> = emptyMap(),
        templateUnitClass: String? = unitClass,
    ): UnitSnapshot =
        UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-0000000000ab")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-0000000000ab")),
                name = instanceName,
                bitmap = Bitmap32(4, 4),
                customProperties = templateUnitClass?.let { mapOf("unitClass" to it) } ?: emptyMap(),
            ),
            instanceName = instanceName,
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(x = x, y = y),
            normalizedWidth = 0.01,
            normalizedHeight = 0.01,
            customProperties = customProperties + (unitClass?.let { mapOf("unitClass" to it) } ?: emptyMap()),
        )

    private fun epsilonGridLayer(): GridLayer =
        GridLayer(
            mapId = "EPSILON",
            referenceSystem = ReferenceSystem.EPSILON,
            gridScale = ReferenceSystem.EPSILON.gridScale,
            unitsPerPixel = 1.0,
            detailLevel = 2,
            pixelsPerLine = 10.0,
            verticalLineNumberStart = EpsilonCoord.fromUnits(0),
            horizontalLineNumberStart = EpsilonCoord.fromUnits(1_000),
            verticalLineCount = 10,
            horizontalLineCount = 10,
        )
}
