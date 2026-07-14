package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class TacticalCombatResolverTest {
    @Test
    fun `standard beams honour shield levels`() {
        val weapon = weapon(beamClass = 1)

        assertThat(resolve(ship(shields = 0), weapon, listOf(4, 5, 6)).damage, equalTo(4))
        assertThat(resolve(ship(shields = 1), weapon, listOf(4, 5, 6)).damage, equalTo(3))
        assertThat(resolve(ship(shields = 2), weapon, listOf(4, 5, 6)).damage, equalTo(2))
    }

    @Test
    fun `submunitions explode and ignore shields`() {
        val result = resolve(ship(shields = 2), weapon(submunition = true), listOf(6, 6, 4))

        assertThat(result.diceRolled, equalTo(3))
        assertThat(result.damage, equalTo(5))
        assertThat(result.dieResults.map(TacticalDieResult::damage), equalTo(listOf(2, 2, 1)))
    }

    @Test
    fun `torpedoes and salvoes use their dedicated damage rules`() {
        assertThat(resolve(ship(), weapon(torpedo = true), listOf(4)).damage, equalTo(4))
        assertThat(resolve(ship(), weapon(torpedo = true), listOf(3)).damage, equalTo(0))
        assertThat(resolve(ship(), weapon(salvo = true), listOf(6)).damage, equalTo(6))
    }

    @Test
    fun `interception removes incoming weapons before their attack resolves`() {
        val salvo = weapon(salvo = true).copy(key = "salvo")
        val beam = weapon().copy(key = "beam")

        val surviving = TacticalCombatResolver.removeInterceptedAttackers(
            listOf(salvo, beam),
            listOf(
                TacticalWeaponContribution(salvo.key, salvo.name, "F", 6),
                TacticalWeaponContribution(beam.key, beam.name, "F", 3),
            ),
            intercepted = 2,
        )

        assertThat(surviving.map(TacticalWeaponContribution::weaponKey), equalTo(listOf("salvo", "beam")))
        assertThat(surviving.map(TacticalWeaponContribution::firepower), equalTo(listOf(4, 3)))
    }

    @Test
    fun `interceptor modifiers and multiple PDS hits apply per die`() {
        val interceptor = weapon(beamClass = 1).copy(interceptor = true)
        val pds = weapon().copy(pointDefense = true)

        assertThat(resolve(ship(), interceptor, listOf(6)).damage, equalTo(1))
        assertThat(resolve(fighter(), interceptor, listOf(4)).damage, equalTo(2))
        assertThat(resolve(fighter(), pds, listOf(4, 4, 4)).damage, equalTo(3))
    }

    @Test
    fun `ship damage fills armour before hull in index order`() {
        val definition = definition(shields = 0, armour = 2, hull = 3)

        val damage = TacticalCombatResolver.applyShipDamage(definition, OlympiadDawnShipDamage(), 4)

        assertThat(damage.destroyedArmour, equalTo(setOf(0, 1)))
        assertThat(damage.destroyedHull, equalTo(setOf(0, 1)))
    }

    @Test
    fun `fighter damage removes one fighter per hit and caps at survivors`() {
        assertThat(TacticalCombatResolver.remainingFightersAfterDamage(6, 3), equalTo(3))
        assertThat(TacticalCombatResolver.remainingFightersAfterDamage(2, 5), equalTo(0))
    }

    private fun resolve(target: UnitSnapshot, weapon: TacticalWeaponStatus, dice: List<Int>): TacticalFireResolution {
        val rolls = dice.iterator()
        return TacticalCombatResolver.resolve(
            target,
            listOf(weapon),
            listOf(
                TacticalWeaponContribution(
                    weapon.key, weapon.name, "F", when {
                        weapon.submunition -> 1
                        weapon.salvo || weapon.salvoRack -> 6
                        else -> 3
                    }
                )
            )
        ) { rolls.next() }
    }

    private fun weapon(
        beamClass: Int? = null,
        submunition: Boolean = false,
        torpedo: Boolean = false,
        salvo: Boolean = false,
    ) = TacticalWeaponStatus("weapon", "Weapon", beamClass, salvo = salvo, submunition = submunition, torpedo = torpedo, arcs = listOf("F"), destroyed = false, fired = false)

    private fun ship(shields: Int = 0): UnitSnapshot {
        val definition = definition(shields, armour = 0, hull = 3)
        val properties = mapOf(OlympiadDawnShip.DEFINITION_KEY to definition.sourceJson)
        return UnitSnapshot(
            MapPlacedObjectId(UUID(0, 1)), UnitTemplateId(UUID(0, 2)),
            MapViewerUnitTemplate(UnitTemplateId(UUID(0, 2)), "Ship", Bitmap32(1, 1), customProperties = properties),
            "Ship", false, 0, MapCoordinate(0.0, 0.0), 0.01, 0.01, properties,
        )
    }

    private fun fighter(): UnitSnapshot = ship().let { ship ->
        val properties = mapOf("unitClass" to "F")
        ship.copy(template = ship.template.copy(customProperties = properties), customProperties = properties)
    }

    private fun definition(shields: Int, armour: Int, hull: Int): OlympiadDawnShipDefinition = OlympiadDawnShip.parse(
        """{"name":"Ship","class":"Frigate","hull":{"points":$hull,"rows":1},"armour":${if (armour == 0) "[]" else "[[$armour]]"},"systems":[${(1..shields).joinToString { "{\"name\":\"screen\",\"id\":\"shield-$it\"}" }}]}""",
    )
}
