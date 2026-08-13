package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.operation.*

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
        val forward = ship(2, "Forward", 0.5, 0.495, unitClass = "FF")
        val starboard = ship(3, "Starboard", 0.504, 0.499)

        val report = resolve(source, listOf(starboard, forward))

        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Forward", "Starboard")))
        assertThat(report.targets.map(TacticalTarget::shipClass), equalTo(listOf("FF", "Frigate")))
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

    @Test
    fun `class one beams can target fighters after fire control is spent`() {
        val source = ship(
            id = 1,
            name = "Source",
            x = 0.5,
            y = 0.5,
            affiliation = UnitAffiliation.Friend,
            weapons = """[{"id":"beam","name":"beam","class":1,"leftArc":"F","numArcs":1}]""",
        )
        val fighterProperties = mapOf("unitClass" to "F")
        val fighter = ship(2, "Fighter", 0.5, 0.495, affiliation = UnitAffiliation.Hostile).copy(
            template = template(2, fighterProperties, UnitAffiliation.Hostile),
            customProperties = fighterProperties,
        )

        val report = TacticalTargetingResolver.resolve(
            source,
            KorgeUnitQueryAdapter(listOf(source, fighter), mapWidth = 1000, mapHeight = 1000, mapUnitsPerPixel = 1.0),
            fireControlUsed = 1,
        )

        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Fighter")))
        assertThat(report.targets.single().shipClass, equalTo("F"))
        assertThat(report.targets.single().contributions.single().weaponKey, equalTo("beam"))
    }

    @Test
    fun `unfaced fighter squadrons can target ships and fighters within one unit`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Carrier","class":"Carrier","hull":{"points":1,"rows":1},"armour":[],"systems":[],"fighters":[{"type":"standard","id":"squadron"}]}""",
        )
        val carrier = ship(1, "Carrier", 0.5, 0.5, affiliation = UnitAffiliation.Friend, definition = definition)
        val sourceProperties = mapOf("unitClass" to "F", OlympiadDawnShip.FIGHTER_UNIT_ID_KEY to "squadron")
        val source = UnitSnapshot(
            objectId(2),
            UnitTemplateId(uuid(102)),
            template(2, sourceProperties, UnitAffiliation.Friend),
            "Fighters",
            false,
            null,
            MapCoordinate(0.5, 0.5),
            0.01,
            0.01,
            sourceProperties,
        )
        val hostileShip = ship(3, "Hostile Ship", 0.5, 0.499, affiliation = UnitAffiliation.Hostile)
        val fighterProperties = mapOf("unitClass" to "F")
        val hostileFighter = UnitSnapshot(
            objectId(4),
            UnitTemplateId(uuid(104)),
            template(4, fighterProperties, UnitAffiliation.Hostile),
            "Hostile Fighters",
            false,
            null,
            MapCoordinate(0.5, 0.4995),
            0.01,
            0.01,
            fighterProperties,
        )

        val report = resolve(source, listOf(carrier, source, hostileShip, hostileFighter))

        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Hostile Fighters", "Hostile Ship")))
    }

    @Test
    fun `torpedo fighter squadrons contribute one torpedo per remaining fighter`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"Carrier","class":"Carrier","hull":{"points":1,"rows":1},"armour":[],"systems":[],"fighters":[{"type":"torpedo","id":"squadron"}]}""",
        )
        val carrier = ship(1, "Carrier", 0.5, 0.5, affiliation = UnitAffiliation.Friend, definition = definition).copy(
            customProperties = mapOf(
                OlympiadDawnShip.DEFINITION_KEY to definition.sourceJson,
                OlympiadDawnShip.DAMAGE_KEY to OlympiadDawnShip.encodeDamage(OlympiadDawnShipDamage(remainingFighters = mapOf("squadron" to 3))),
            ),
        )
        val sourceProperties = mapOf("unitClass" to "F", OlympiadDawnShip.FIGHTER_UNIT_ID_KEY to "squadron")
        val source = UnitSnapshot(
            objectId(2), UnitTemplateId(uuid(102)), template(2, sourceProperties, UnitAffiliation.Friend), "Torpedo Fighters", false, null,
            MapCoordinate(0.5, 0.5), 0.01, 0.01, sourceProperties,
        )
        val target = ship(3, "Hostile Ship", 0.5, 0.499, affiliation = UnitAffiliation.Hostile)

        val report = resolve(source, listOf(carrier, source, target))

        assertThat(report.weapons.single().quantity, equalTo(3))
        assertThat(report.targets.single().firepower, equalTo(3))
    }

    @Test
    fun `operational ECM modules provide automatic and manual targeting solutions`() {
        val definition = OlympiadDawnShip.parse(
            """{"name":"ECM Ship","class":"Frigate","hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"ecm","id":"ecm-1"},{"name":"ecm","id":"ecm-2"}]}""",
        )
        val source = ship(1, "Source", 0.5, 0.5, affiliation = UnitAffiliation.Friend, definition = definition)
        val target = ship(2, "Target", 0.5, 0.49, affiliation = UnitAffiliation.Hostile)

        val report = resolve(source, listOf(source, target))

        assertThat(report.weapons.map(TacticalWeaponStatus::key), equalTo(listOf("ecm-ecm-1", "ecm-ecm-2")))
        assertThat(report.weapons.all(TacticalWeaponStatus::ecm), equalTo(true))
        assertThat(report.targets.map(TacticalTarget::targetName), equalTo(listOf("Target", "Manual Targeting")))
        assertThat(report.targets.first().contributions.map(TacticalWeaponContribution::weaponKey), equalTo(listOf("ecm-ecm-1", "ecm-ecm-2")))
        assertThat(report.targets.last().manualEcm, equalTo(true))
    }

    private fun resolve(source: UnitSnapshot, candidates: List<UnitSnapshot>) =
        TacticalTargetingResolver.resolve(source, KorgeUnitQueryAdapter(candidates, mapWidth = 1000, mapHeight = 1000, mapUnitsPerPixel = 1.0))

    private fun ship(
        id: Int,
        name: String,
        x: Double,
        y: Double,
        facing: Int? = 0,
        affiliation: UnitAffiliation? = UnitAffiliation.Hostile,
        weapons: String = """[{"id":"beam","name":"beam","class":3,"leftArc":"F","numArcs":6}]""",
        definition: OlympiadDawnShipDefinition = definition(weapons),
        unitClass: String? = null,
    ): UnitSnapshot {
        val properties = mapOf(OlympiadDawnShip.DEFINITION_KEY to definition.sourceJson) +
                unitClass?.let { mapOf("unitClass" to it) }.orEmpty()
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
