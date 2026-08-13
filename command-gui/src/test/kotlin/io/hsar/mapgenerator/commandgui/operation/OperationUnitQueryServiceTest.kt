package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.UUID

class OperationUnitQueryServiceTest {
    @Test
    fun `converts normalized coordinates using the map scale`() {
        val first = unit(1, "First", 0.1, 0.1)
        val second = unit(2, "Second", 0.16, 0.14)
        val service = service(listOf(first, second), 100, 200, 0.5)

        assertThat(service.distanceBetween(first, second), closeTo(5.0, 0.0001))
    }

    @Test
    fun `radius queries include the centre and exact boundary across grid cells`() {
        val centre = unit(1, "Centre", 0.0, 0.0)
        val boundary = unit(2, "Boundary", 0.006, 0.0)
        val outside = unit(3, "Outside", 0.0061, 0.0)
        val service = service(listOf(centre, boundary, outside))

        val matches = service.unitsWithin(centre, 6.0)

        assertThat(matches.map(OperationUnitQueryMatch::unit).map(OperationUnit::id), equalTo(listOf(centre.id, boundary.id)))
        assertThat(matches.last().distanceUnits, closeTo(6.0, 0.0001))
        assertThat(service.countWithin(centre.coordinate, 6.0), equalTo(matches.size))
    }

    @Test
    fun `radius results are ordered by range name and identifier`() {
        val centre = unit(1, "Centre", 0.0, 0.0)
        val zulu = unit(4, "Zulu", 0.003, 0.0)
        val alphaHighId = unit(3, "Alpha", -0.003, 0.0)
        val alphaLowId = unit(2, "Alpha", 0.0, 0.003)

        assertThat(
            service(listOf(zulu, alphaHighId, centre, alphaLowId)).unitsWithin(centre, 3.0).map { it.unit.id },
            equalTo(listOf(centre.id, alphaLowId.id, alphaHighId.id, zulu.id)),
        )
    }

    @Test
    fun `matches expose only operational ship systems`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Ship","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"drive","id":"drive"},{"name":"fireControl","id":"fc"}]}""",
        )
        val damage = OlympiadDawnShip.encodeDamage(OlympiadDawnShipDamage(destroyedSystems = setOf("fc")))
        val ship = unit(1, "Ship", 0.0, 0.0, mapOf(OlympiadDawnShip.DEFINITION_KEY to definition.sourceJson, OlympiadDawnShip.DAMAGE_KEY to damage))

        assertThat(service(listOf(ship)).unitsWithin(ship, 0.0).single().operationalSystems.map(OlympiadDawnShipSystem::id), equalTo(listOf("drive")))
    }

    private fun service(units: List<OperationUnit>, width: Int = 1_000, height: Int = 1_000, unitsPerPixel: Double = 1.0) =
        OperationUnitQueryService(
            OperationContent(
                objects = units,
                unitTemplates = units.map { unit ->
                    OperationUnitTemplate(unit.unitTemplateId, unit.instanceName, BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), unit.id.value.leastSignificantBits.toInt())
                },
            ),
            width,
            height,
            unitsPerPixel,
        )

    private fun unit(id: Int, name: String, x: Double, y: Double, properties: Map<String, String> = emptyMap()) = OperationUnit(
        id = OperationObjectId(UUID(0, id.toLong())),
        unitTemplateId = OperationUnitTemplateId(UUID(0, (id + 100).toLong())),
        instanceName = name,
        destroyed = false,
        facingDegrees = null,
        coordinate = MapCoordinate(x, y),
        normalizedWidth = 0.01,
        normalizedHeight = 0.01,
        customProperties = properties,
    )
}
