package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.korge.interaction.EcmActivation
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class TacticalTurnMarkerTest {
    @Test
    fun `marker retains firing state by turn and leaves an unmarked turn fresh`() {
        val unit = MapPlacedObjectId(UUID(0, 1))
        val marked = TacticalTurnMarker.update(
            emptyMap(),
            turnNumber = 3,
            usage = TacticalFireUsage(setOf("beam"), 1),
            activation = EcmActivation(3, unit, setOf("ecm"), 90.0, 2.0, 60.0, 2),
        )

        assertThat(TacticalTurnMarker.usage(marked, 3), equalTo(TacticalFireUsage(setOf("beam"), 1)))
        assertThat(TacticalTurnMarker.usage(marked, 4), equalTo(null))
        assertThat(TacticalTurnMarker.activations(marked, unit).single().moduleKeys, equalTo(setOf("ecm")))
    }

    @Test
    fun `top level turn array is read using the requested turn number`() {
        val properties = mapOf(
            TacticalTurnMarker.PROPERTY_KEY to """[{"turn":7,"fireControlUsed":0,"firedWeapons":["-4zMD","1ft3L","ecm-IksVk"]}]""",
        )

        assertThat(
            TacticalTurnMarker.usage(properties, 7),
            equalTo(TacticalFireUsage(setOf("-4zMD", "1ft3L", "ecm-IksVk"), 0)),
        )
        assertThat(TacticalTurnMarker.usage(properties, 6), equalTo(null))
    }

    @Test
    fun `reconciling marked state removes an undone firing record`() {
        val unit = MapPlacedObjectId(UUID(0, 1))
        val key = TacticalFireKey(1, unit)
        val persistence = TacticalFirePersistence.fromJson(null)

        persistence.reconcileMarked(mapOf(key to TacticalFireUsage(setOf("beam"), 1)))
        persistence.reconcileMarked(emptyMap())

        assertThat(persistence.usage(key), equalTo(TacticalFireUsage()))
    }
}
