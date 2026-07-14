package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class TacticalTargetingResolverTest {
    @Test
    fun `ranks targets by available firepower and reports each contributing mount`() {
        val source = ship(
            id = 1,
            name = "Source",
            x = 0.5,
            y = 0.5,
            facing = 0,
            affiliation = UnitAffiliation.Neutral,
            weapons = """[{"id":"wide","name":"beam","class":3,"leftArc":"F","numArcs":2},{"id":"forward","name":"beam","class":2,"leftArc":"F","numArcs":1}]""",
        )
        val forward = ship(2, "Forward", 0.5, 0.495)
        val starboard = ship(3, "Starboard", 0.504, 0.499)

        val report = resolve(source, listOf(starboard, forward))

        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Forward", "Starboard")))
        assertThat(report.targets.map(TacticalTarget::firepower), equalTo(listOf(5, 3)))
        assertThat(report.targets.first().contributions.map(TacticalWeaponContribution::weaponKey), equalTo(listOf("wide", "forward")))
        assertThat(report.targets.first().rangeU, closeTo(5.0, 0.0001))
    }

    @Test
    fun `uses six firing sectors including wraparound and range band boundaries`() {
        val source = ship(
            id = 1,
            name = "Source",
            x = 0.5,
            y = 0.5,
            facing = 0,
            affiliation = UnitAffiliation.Neutral,
            weapons = """[{"id":"aft-port","name":"beam","class":2,"leftArc":"A","numArcs":3}]""",
        )
        val aft = ship(2, "Aft", 0.5, 0.506)
        val portForward = ship(3, "Port Forward", 0.497, 0.497)
        val outOfArc = ship(4, "Starboard", 0.503, 0.5)

        val report = resolve(source, listOf(aft, portForward, outOfArc))

        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Port Forward", "Aft")))
        assertThat(report.targets.map { it.arcs.single() }, equalTo(listOf("FP", "A")))
        assertThat(report.targets.first().firepower, equalTo(2))
        assertThat(report.targets.last().firepower, equalTo(1))
    }

    @Test
    fun `excludes destroyed mounts and ineligible candidates while ignoring affiliation`() {
        val definition =
            OlympiadDawnShip.parse("""{"name":"Ship","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"fireControl","id":"fc-1"}],"weapons":[{"id":"beam-1","name":"beam","class":2,"leftArc":"F","numArcs":1},{"id":"beam-2","name":"beam","class":2,"leftArc":"F","numArcs":1},{"id":"pds","name":"pds"}]}""")
        val source = ship(1, "Source", 0.5, 0.5, affiliation = UnitAffiliation.Neutral, definition = definition).copy(
            customProperties = mapOf(
                OlympiadDawnShip.DEFINITION_KEY to definition.sourceJson,
                OlympiadDawnShip.DAMAGE_KEY to OlympiadDawnShip.encodeDamage(OlympiadDawnShipDamage(destroyedWeapons = setOf("beam-1"))),
            ),
        )
        val hostile = ship(2, "Hostile", 0.5, 0.495, affiliation = UnitAffiliation.Hostile)
        val friendly = ship(3, "Friendly", 0.5, 0.496, affiliation = UnitAffiliation.Friend)
        val destroyed = ship(4, "Destroyed", 0.5, 0.497).copy(destroyed = true)
        val nonShip = ship(5, "Non Ship", 0.5, 0.498).copy(customProperties = emptyMap(), template = template(5, emptyMap()))

        val report = resolve(source, listOf(source, hostile, friendly, destroyed, nonShip))

        assertThat(report.weapons.map(TacticalWeaponStatus::key), equalTo(listOf("beam-1", "beam-2", "pds")))
        assertThat(report.weapons.map(TacticalWeaponStatus::destroyed), equalTo(listOf(true, false, false)))
        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Friendly", "Hostile")))
        assertThat(report.targets.map(TacticalTarget::firepower), equalTo(listOf(2, 2)))
    }

    @Test
    fun `uses deterministic range name and id tie breakers`() {
        val source = ship(1, "Source", 0.5, 0.5, affiliation = UnitAffiliation.Neutral)
        val farther = ship(2, "Alpha", 0.5, 0.496)
        val zulu = ship(3, "Zulu", 0.5, 0.497)
        val alphaHighId = ship(5, "Alpha", 0.5, 0.497)
        val alphaLowId = ship(4, "Alpha", 0.5, 0.497)

        val report = resolve(source, listOf(zulu, alphaHighId, farther, alphaLowId))

        assertThat(report.targets.map { it.targetId }, equalTo(listOf(alphaLowId.id, alphaHighId.id, zulu.id, farther.id)))
    }

    private fun resolve(source: UnitSnapshot, candidates: List<UnitSnapshot>) =
        TacticalTargetingResolver.resolve(source, candidates, mapWidth = 1000, mapHeight = 1000, mapUnitsPerPixel = 1.0)

    private fun ship(
        id: Int,
        name: String,
        x: Double,
        y: Double,
        facing: Int? = 0,
        affiliation: UnitAffiliation? = UnitAffiliation.Hostile,
        weapons: String = """[{"id":"beam","name":"beam","class":3,"leftArc":"F","numArcs":6}]""",
        definition: OlympiadDawnShipDefinition = definition(weapons),
    ): UnitSnapshot {
        val properties = mapOf(OlympiadDawnShip.DEFINITION_KEY to definition.sourceJson)
        return UnitSnapshot(
            id = objectId(id),
            unitTemplateId = UnitTemplateId(uuid(id + 100)),
            template = template(id, properties, affiliation),
            instanceName = name,
            destroyed = false,
            facingDegrees = facing,
            mapCoordinate = MapCoordinate(x, y),
            normalizedWidth = 0.01,
            normalizedHeight = 0.01,
            customProperties = properties,
        )
    }

    private fun template(id: Int, properties: Map<String, String>, affiliation: UnitAffiliation? = UnitAffiliation.Neutral) =
        MapViewerUnitTemplate(
            id = UnitTemplateId(uuid(id + 100)),
            name = "Type $id",
            bitmap = Bitmap32(1, 1),
            affiliation = affiliation,
            customProperties = properties,
        )

    private fun definition(weapons: String) = OlympiadDawnShip.parse(
        """{"name":"Ship","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"fireControl","id":"fc-1"}],"weapons":$weapons}""",
    )

    private fun objectId(id: Int) = MapPlacedObjectId(uuid(id))

    private fun uuid(id: Int): UUID = UUID(0, id.toLong())
}
