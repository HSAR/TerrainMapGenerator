package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class OlympiadDawnShipTest {
    @Test
    fun `parses magazines ordnance friendly names and fighter beam arcs`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"fireControl","id":"fc"},{"name":"magazine","id":"mag-1","capacity":3},{"name":"ecm","id":"ecm-1"}],"ordnance":[{"name":"salvoLauncher","id":"salvo-1","leftArc":"F","numArcs":2,"magazine":"mag-1"}],"fighters":[{"type":"standard","id":"fighters"}],"class":"Frigate","name":"Test"}""",
        )
        assertThat(definition.systems.map { it.name }, equalTo(listOf("Gunnery Fire Control System", "Magazine - 3", "Electronic Countermeasures")))
        assertThat(definition.systems[1].capacity, equalTo(3))
        assertThat(definition.weapons.single().name, equalTo("Salvo Missile Launcher"))
        assertThat(definition.weapons.single().magazineId, equalTo("mag-1"))
        assertThat(definition.fighters.single().weapons.single().arcs, equalTo(listOf("F", "FS", "AS", "A", "AP", "FP")))
    }

    @Test
    fun `binds bays and mine layers from ship JSON systems`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"bay","id":"kfNfH","capacity":16,"type":"cargo"},{"name":"bay","id":"cargo-bay","type":"cargo"},{"name":"bay","id":"sized-bay","capacity":8},{"name":"bay","id":"plain-bay"},{"name":"mineLayer","id":"F1jAt","capacity":3}],"class":"Minelayer","name":"Test"}""",
        )

        assertThat(definition.systems.map(OlympiadDawnShipSystem::name), equalTo(listOf("Cargo Bay - 16", "Cargo Bay", "Bay - 8", "Bay", "Mine Layer - 3")))
        assertThat(definition.systems.map(OlympiadDawnShipSystem::capacity), equalTo(listOf(null, null, null, null, 3)))
        assertThat(
            OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition)[OlympiadDawnShip.magazinePropertyKey("F1jAt")],
            equalTo("3"),
        )
    }

    @Test
    fun `parses a ship definition and preserves untracked source sections`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":24,"rows":4},"armour":[[5,0]],"systems":[{"name":"drive","thrust":2,"id":"drive-1"},{"name":"fireControl","id":"fc-1"},{"name":"hangar","id":"hangar-1"}],"weapons":[{"name":"beam","class":2,"leftArc":"FS","numArcs":2}],"fighters":[{"name":"fighters","type":"standard","id":"squadron-1","mods":[],"hangar":"hangar-1"}],"mass":55,"class":"Heavy Cruiser","name":"Vandenburg"}""",
        )

        assertThat(definition.name, equalTo("Vandenburg"))
        assertThat(definition.mass, equalTo(55))
        assertThat(definition.hullPoints, equalTo(24))
        assertThat(definition.armourPoints, equalTo(5))
        assertThat(definition.systems.map { it.name }, equalTo(listOf("Impulse Drive", "Impulse Drive", "Gunnery Fire Control System", "Hangar - Fighters [squadron-1]")))
        assertThat(definition.systems.last().fighterSquadronIds, equalTo(setOf("squadron-1")))
        assertThat(definition.weapons.single().name, equalTo("Class 2 Beam Turret"))
        assertThat(definition.weapons.single().arcs, equalTo(listOf("FS", "AS")))
        assertThat(definition.sourceJson.contains("weapons"), equalTo(true))

        val properties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition)
        assertThat(properties[OlympiadDawnShip.UNIT_CLASS_KEY], equalTo("CA"))
        assertThat(properties[OlympiadDawnShip.MASS_KEY], equalTo("55"))
        assertThat(OlympiadDawnShip.importedMoveLimit(definition), equalTo(1.0))
    }

    @Test
    fun `maps escort cruisers and cargo ship classes during import`() {
        val escortCruiser = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[],"class":"Escort Cruiser","name":"Escort"}""",
        )
        val cargoShip = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[],"mass":10,"class":"Armed Cargo Ship Freighter","name":"Cargo"}""",
        )

        assertThat(OlympiadDawnShip.importedTemplateProperties(emptyMap(), escortCruiser)[OlympiadDawnShip.UNIT_CLASS_KEY], equalTo("CA"))
        assertThat(OlympiadDawnShip.importedTemplateProperties(emptyMap(), cargoShip)[OlympiadDawnShip.UNIT_CLASS_KEY], equalTo("AK"))
    }

    @Test
    fun `accepts ships without armour`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[],"weapons":[],"class":"Scout","name":"Test"}""",
        )

        assertThat(definition.armourPoints, equalTo(null))
    }

    @Test
    fun `resolves beam firepower by six unit range bands`() {
        assertThat(OlympiadDawnBeamRules.firepowerAtRange(3, 0.0), equalTo(3))
        assertThat(OlympiadDawnBeamRules.firepowerAtRange(3, 6.0), equalTo(2))
        assertThat(OlympiadDawnBeamRules.firepowerAtRange(3, 12.0), equalTo(1))
        assertThat(OlympiadDawnBeamRules.firepowerAtRange(3, 18.0), equalTo(0))
        assertThat(OlympiadDawnBeamRules.maximumRangeU(3), equalTo(18.0))
        assertThat(
            OlympiadDawnShip.totalBeamFirepowerAtRange(
                listOf(
                    OlympiadDawnShipWeapon("beam-1", "Class 1 Beam Turret", beamClass = 1),
                    OlympiadDawnShipWeapon("beam-2", "Class 1 Beam Turret", beamClass = 1),
                    OlympiadDawnShipWeapon("beam-3", "Class 3 Beam Turret", beamClass = 3),
                ),
                3.0,
            ),
            equalTo(5),
        )
    }

    @Test
    fun `rejects duplicate system IDs`() {
        val result = runCatching {
            OlympiadDawnShip.parse(
                """{"hull":{"points":1,"rows":1},"armour":[[0,0]],"systems":[{"name":"drive","id":"a"},{"name":"ftl","id":"a"}],"class":"Scout","name":"Test"}""",
            )
        }

        assertThat(result.isFailure, equalTo(true))
    }

    @Test
    fun `rejects multiple fighter squadrons assigned to the same hangar`() {
        val result = runCatching {
            OlympiadDawnShip.parse(
                """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"hangar","id":"hangar-1"}],"class":"Carrier","name":"Test","fighters":[{"type":"standard","id":"fighters","hangar":"hangar-1"},{"type":"interceptor","id":"interceptors","hangar":"hangar-1"}]}""",
            )
        }

        assertThat(result.exceptionOrNull()?.message, equalTo("Only one fighter squadron may be assigned to each hangar"))
    }

    @Test
    fun `creates independent default state for a placed unit`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":2,"rows":1},"armour":[[1,0]],"systems":[{"name":"drive","id":"drive"}],"class":"Scout","name":"Test"}""",
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(java.util.UUID.randomUUID()),
            name = "Test",
            bitmap = korlibs.image.bitmap.Bitmap32(2, 2),
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )

        val properties = OlympiadDawnShip.instanceProperties(template)

        assertThat(properties.containsKey(OlympiadDawnShip.DAMAGE_KEY), equalTo(true))
        assertThat(OlympiadDawnShip.instanceDamage(properties, definition), equalTo(OlympiadDawnShipDamage()))
    }

    @Test
    fun `maps fighter types to their fixed squadrons and derives surviving weapon totals`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"hangar","id":"hangar-1"}],"class":"Carrier","name":"Test","fighters":[{"name":"fighters","type":"interceptor","id":"interceptors","mods":[],"hangar":"hangar-1"},{"name":"fighters","type":"torpedo","id":"bombers","mods":[]}]}""",
        )

        val damage = OlympiadDawnShipDamage(remainingFighters = mapOf("interceptors" to 1, "bombers" to 0))

        assertThat(definition.fighters.map { it.name to it.count }, equalTo(listOf("Interceptor" to 6, "Torpedo Fighter" to 6)))
        assertThat(definition.systems.single().name, equalTo("Hangar - Interceptors [interceptors]"))
        assertThat(
            OlympiadDawnShip.fighterWeaponTotals(definition, damage).map { Triple(it.name, it.arcs, it.count) },
            equalTo(
                listOf(
                    Triple("Light Interceptor Beam", listOf("F", "FS", "AS", "A", "AP", "FP"), 1),
                    Triple("Light Torpedo Launcher", listOf("F", "FS", "AS", "A", "AP", "FP"), 0)
                )
            ),
        )
    }

    @Test
    fun `uses fighter IDs for persisted strength and clamps it to the fixed squadron size`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[],"class":"Carrier","name":"Test","fighters":[{"name":"fighters","type":"assault","id":"assault-1","mods":[]}]}""",
        )

        val damage = OlympiadDawnShip.decodeDamage("""{"remainingFighters":{"assault-1":5,"unknown":0}}""", definition)

        assertThat(definition.fighters.single().count, equalTo(3))
        assertThat(OlympiadDawnShip.remainingFighters(definition, damage, 0), equalTo(3))
        assertThat(damage.remainingFighters, equalTo(mapOf("assault-1" to 3)))
        assertThat(
            OlympiadDawnShip.decodeDamage(OlympiadDawnShip.encodeDamage(OlympiadDawnShipDamage(remainingFighters = mapOf("assault-1" to 0))), definition).remainingFighters,
            equalTo(mapOf("assault-1" to 0)),
        )
    }

    @Test
    fun `resolves fighter links against exactly one carrier squadron`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"hangar","id":"hangar-1"}],"class":"Carrier","name":"Carrier","fighters":[{"name":"fighters","type":"standard","id":"squadron-1","mods":[],"hangar":"hangar-1"}]}""",
        )
        val carrierTemplate = MapViewerUnitTemplate(
            id = UnitTemplateId(java.util.UUID.randomUUID()),
            name = "Carrier",
            bitmap = korlibs.image.bitmap.Bitmap32(2, 2),
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )
        val carrierId = MapPlacedObjectId(java.util.UUID.randomUUID())
        val carrierProperties = OlympiadDawnShip.instanceProperties(carrierTemplate)
        val squadronId = OlympiadDawnShip.definition(carrierProperties)!!.fighters.single().id
        val carrier = UnitSnapshot(
            id = carrierId,
            unitTemplateId = carrierTemplate.id,
            template = carrierTemplate,
            instanceName = "Carrier",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.0, 0.0),
            normalizedWidth = 1.0,
            normalizedHeight = 1.0,
            customProperties = carrierProperties,
        )
        val fighterTemplate = MapViewerUnitTemplate(
            id = UnitTemplateId(java.util.UUID.randomUUID()),
            name = "Fighter",
            bitmap = korlibs.image.bitmap.Bitmap32(2, 2),
            customProperties = mapOf("unitClass" to "F"),
        )
        val fighter = UnitSnapshot(
            id = MapPlacedObjectId(java.util.UUID.randomUUID()),
            unitTemplateId = fighterTemplate.id,
            template = fighterTemplate,
            instanceName = "Fighter 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.0, 0.0),
            normalizedWidth = 1.0,
            normalizedHeight = 1.0,
            customProperties = mapOf(OlympiadDawnShip.FIGHTER_UNIT_ID_KEY to squadronId),
        )
        val unrelated = fighter.copy(
            id = MapPlacedObjectId(java.util.UUID.randomUUID()),
            customProperties = mapOf("unitClass" to "DD", OlympiadDawnShip.FIGHTER_UNIT_ID_KEY to squadronId),
        )
        val statuses = OlympiadDawnShip.fighterLaunchStatuses(carrier, listOf(carrier, fighter, unrelated))
        val duplicateCarrier = carrier.copy(id = MapPlacedObjectId(java.util.UUID.randomUUID()))
        val squadronStatus = OlympiadDawnShip.squadronStatus(fighter, listOf(carrier, fighter))!!
        val damagedCarrier = carrier.copy(
            customProperties = OlympiadDawnShip.updatedSquadronProperties(carrier, squadronStatus.squadronId, 3)!!,
        )

        assertThat(statuses[squadronId]?.destroyed, equalTo(false))
        assertThat(OlympiadDawnShip.fighterLaunchStatuses(carrier, listOf(carrier)), equalTo(emptyMap()))
        assertThat(squadronStatus.remaining, equalTo(6))
        assertThat(squadronStatus.weapons.single().name, equalTo("Light Multirole Beam"))
        assertThat(OlympiadDawnShip.squadronStatus(fighter, listOf(damagedCarrier, fighter))?.remaining, equalTo(3))
        assertThat(OlympiadDawnShip.hasUniqueFighterLink(fighter, listOf(carrier, fighter)), equalTo(true))
        assertThat(OlympiadDawnShip.hasUniqueFighterLink(fighter, listOf(fighter)), equalTo(false))
        assertThat(OlympiadDawnShip.hasUniqueFighterLink(fighter, listOf(carrier, duplicateCarrier, fighter)), equalTo(false))
        assertThat(
            OlympiadDawnShip.validatedFighterProperties(fighter, listOf(carrier, duplicateCarrier, fighter)),
            equalTo(emptyMap()),
        )
        assertThat(
            OlympiadDawnShip.fighterLaunchStatuses(carrier, listOf(carrier, fighter.copy(destroyed = true)))[squadronId]?.destroyed,
            equalTo(true),
        )
    }

    @Test
    fun `writes four character fighter IDs into placed ship definitions`() {
        val definition = OlympiadDawnShip.parse(
            """{"hull":{"points":1,"rows":1},"armour":[],"systems":[{"name":"hangar","id":"hangar-1"}],"class":"Carrier","name":"Carrier","fighters":[{"type":"standard","id":"squadron-1","hangar":"hangar-1"}]}""",
        )
        val template = MapViewerUnitTemplate(
            id = UnitTemplateId(java.util.UUID.randomUUID()),
            name = "Carrier",
            bitmap = korlibs.image.bitmap.Bitmap32(2, 2),
            customProperties = OlympiadDawnShip.importedTemplateProperties(emptyMap(), definition),
        )
        val firstProperties = OlympiadDawnShip.instanceProperties(template)
        val secondProperties = OlympiadDawnShip.instanceProperties(template)
        val firstFighterId = OlympiadDawnShip.definition(firstProperties)!!.fighters.single().id
        val secondFighterId = OlympiadDawnShip.definition(secondProperties)!!.fighters.single().id

        assertThat(firstFighterId.matches(Regex("[A-NP-Z0-9_]{4}")), equalTo(true))
        assertThat(secondFighterId.matches(Regex("[A-NP-Z0-9_]{4}")), equalTo(true))
        assertThat(firstProperties.keys.any { it.startsWith("olympiadDawn.fighterLink.") }, equalTo(false))
    }
}
