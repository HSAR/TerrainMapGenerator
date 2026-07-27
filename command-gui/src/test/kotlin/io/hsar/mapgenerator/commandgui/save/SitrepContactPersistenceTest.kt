package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepContactState
import io.hsar.mapgenerator.commandgui.korge.interaction.SitrepLostContact
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class SitrepContactPersistenceTest {

    @Test
    fun `contact records preserve unrelated data and sort by turn then observer`() {
        val observerLater = id("00000000-0000-0000-0000-000000000002")
        val observerEarlier = id("00000000-0000-0000-0000-000000000001")
        val contact = id("00000000-0000-0000-0000-000000000003")
        val persistence = SitrepContactPersistence.fromJson("""{"custom":"keep"}""".toByteArray())

        persistence.update(
            SitrepContactKey(2, observerLater),
            SitrepContactState(sensorContactIds = setOf(contact), sierraContactNumbers = mapOf(contact to 2)),
        )
        persistence.update(
            SitrepContactKey(1, observerEarlier),
            SitrepContactState(sensorContactIds = setOf(contact), identifiedIds = setOf(contact), sierraContactNumbers = mapOf(contact to 1)),
        )

        val json = persistence.toJson().toString(Charsets.UTF_8)

        assertThat(json.contains("\"custom\" : \"keep\""), equalTo(true))
        assertThat(json.indexOf("\"turn\" : 1") < json.indexOf("\"turn\" : 2"), equalTo(true))
        val restored = SitrepContactPersistence.fromJson(json.toByteArray())
        assertThat(
            restored.stateFor(SitrepContactKey(1, observerEarlier)),
            equalTo(
                SitrepContactState(
                    setOf(contact),
                    setOf(contact),
                    mapOf(contact to 1),
                    setOf(1),
                ),
            ),
        )
        assertThat(
            restored.stateFor(SitrepContactKey(2, observerLater)).sierraContactNumbers[contact],
            equalTo(2),
        )
    }

    @Test
    fun `empty state removes a contact record`() {
        val observer = id("00000000-0000-0000-0000-000000000010")
        val contact = id("00000000-0000-0000-0000-000000000011")
        val persistence = SitrepContactPersistence.fromJson(null)
        val key = SitrepContactKey(1, observer)
        persistence.update(key, SitrepContactState(sensorContactIds = setOf(contact)))
        persistence.update(key, SitrepContactState())

        assertThat(persistence.toJsonOrNull(), equalTo(null))
    }

    @Test
    fun `historical Sierra numbers remain persisted after active contacts are removed`() {
        val observer = id("00000000-0000-0000-0000-000000000012")
        val contact = id("00000000-0000-0000-0000-000000000013")
        val persistence = SitrepContactPersistence.fromJson(null)
        val key = SitrepContactKey(1, observer)

        persistence.update(
            key,
            SitrepContactState(
                sensorContactIds = setOf(contact),
                sierraContactNumbers = mapOf(contact to 4),
                usedSierraContactNumbers = setOf(4),
            ),
        )
        persistence.update(
            key,
            SitrepContactState(usedSierraContactNumbers = setOf(4)),
        )

        val restored = SitrepContactPersistence.fromJson(persistence.toJson())
        assertThat(
            restored.stateFor(key),
            equalTo(SitrepContactState(usedSierraContactNumbers = setOf(4))),
        )
    }

    @Test
    fun `known Sierra mapping remains available when contact is hidden in the current turn`() {
        val observer = id("00000000-0000-0000-0000-000000000014")
        val contact = id("00000000-0000-0000-0000-000000000015")
        val key = SitrepContactKey(1, observer)
        val persistence = SitrepContactPersistence.fromJson(null)

        persistence.update(
            key,
            SitrepContactState(
                sensorContactIds = setOf(contact),
                sierraContactNumbers = mapOf(contact to 8),
                usedSierraContactNumbers = setOf(8),
            ),
        )
        persistence.update(
            key,
            SitrepContactState(
                sierraContactNumbers = mapOf(contact to 8),
                usedSierraContactNumbers = setOf(8),
            ),
        )

        val restored = SitrepContactPersistence.fromJson(persistence.toJson())
        assertThat(restored.stateFor(key).sensorContactIds, equalTo(emptySet()))
        assertThat(
            restored.stateFor(key).sierraContactNumbers,
            equalTo(mapOf(contact to 8)),
        )
    }

    @Test
    fun `copying a turn carries observer contact state into the new turn`() {
        val observer = id("00000000-0000-0000-0000-000000000020")
        val contact = id("00000000-0000-0000-0000-000000000021")
        val persistence = SitrepContactPersistence.fromJson(null)
        val state = SitrepContactState(
            sensorContactIds = setOf(contact),
            identifiedIds = setOf(contact),
            sierraContactNumbers = mapOf(contact to 7),
        )
        persistence.update(SitrepContactKey(1, observer), state)

        persistence.copyTurnState(1, 2)

        assertThat(
            persistence.stateFor(SitrepContactKey(2, observer)),
            equalTo(state.copy(usedSierraContactNumbers = setOf(7))),
        )
    }

    @Test
    fun `lost contact routing metadata survives persistence`() {
        val observer = id("00000000-0000-0000-0000-000000000030")
        val contact = id("00000000-0000-0000-0000-000000000031")
        val key = SitrepContactKey(1, observer)
        val lost = SitrepLostContact(
            contactId = contact,
            sierraNumber = 3,
            lastSeenPosition = MapCoordinate(0.4, 0.5),
            lastSeenRangeUnits = 4.0,
            wasIdentified = true,
            wasFriendly = true,
            lostDueToEnemyAction = true,
        )
        val persistence = SitrepContactPersistence.fromJson(null)

        persistence.update(key, SitrepContactState(lostContacts = listOf(lost)))

        assertThat(
            SitrepContactPersistence.fromJson(persistence.toJson()).stateFor(key).lostContacts,
            equalTo(listOf(lost)),
        )
    }

    private fun id(value: String): MapPlacedObjectId =
        MapPlacedObjectId(UUID.fromString(value))
}
