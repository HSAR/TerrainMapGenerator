package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class TacticalFirePersistenceTest {
    @Test
    fun `stores fired weapons and fire control usage by turn and unit`() {
        val unit = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val persistence = TacticalFirePersistence.fromJson(null)
        val key = TacticalFireKey(7, unit)
        persistence.replace(key, TacticalFireUsage(setOf("beam-1", "beam-2"), 1))

        val restored = TacticalFirePersistence.fromJson(persistence.toJson())

        assertThat(restored.usage(key), equalTo(TacticalFireUsage(setOf("beam-1", "beam-2"), 1)))
        assertThat(restored.usage(TacticalFireKey(8, unit)), equalTo(TacticalFireUsage()))
    }
}
