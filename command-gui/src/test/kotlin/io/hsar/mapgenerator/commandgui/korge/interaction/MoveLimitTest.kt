package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.nextTestUuid
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MoveLimitTest {
    @Test
    fun `parses finite non-negative move limits and removes invalid values`() {
        assertThat(MoveLimit.parse(mapOf("moveLimit" to " 2.5 ")), equalTo(2.5))
        assertThat(MoveLimit.parse(mapOf("moveLimit" to "0")), equalTo(0.0))
        assertThat(MoveLimit.parse(emptyMap()), equalTo(null))
        assertThat(MoveLimit.sanitize(mapOf("moveLimit" to "NaN", "other" to "value")), equalTo(mapOf("other" to "value")))
        assertThat(MoveLimit.sanitize(mapOf("moveLimit" to "Infinity")), equalTo(emptyMap()))
        assertThat(MoveLimit.sanitize(mapOf("moveLimit" to "-1")), equalTo(emptyMap()))
    }

    @Test
    fun `applies warp movement multipliers without changing the base limit`() {
        assertThat(MoveLimit.withWarpMultiplier(2.5, emptyMap()), equalTo(2.5))
        assertThat(MoveLimit.withWarpMultiplier(2.5, mapOf("activeWarp" to "true")), equalTo(25.0))
        assertThat(MoveLimit.withWarpMultiplier(2.5, mapOf("activeWarp" to "true", "maximumWarp" to "true")), equalTo(50.0))
        assertThat(MoveLimit.withWarpMultiplier(2.5, mapOf("maximumWarp" to "true")), equalTo(50.0))
    }

    @Test
    fun `projects an over-limit request onto the move limit radius`() {
        val constrained = MoveLimit.constrain(
            origin = MapCoordinate(0.5, 0.5),
            requested = MapCoordinate(0.8, 1.3),
            limit = 50.0,
            mapWidth = 100,
            mapHeight = 100,
            mapUnitsPerPixel = 1.0,
        )

        assertThat(constrained.x, closeTo(0.67556172, 0.0000001))
        assertThat(constrained.y, closeTo(0.96816459, 0.0000001))
    }

    @Test
    fun `uses the latest turn boundary or current-turn creation as the origin`() {
        val history = MapActionHistory()
        val existingId = MapPlacedObjectId(nextTestUuid())
        val createdId = MapPlacedObjectId(nextTestUuid())
        val template = MapViewerUnitTemplate(id = UnitTemplateId(nextTestUuid()), name = "Unit", bitmap = Bitmap32(1, 1))
        val existing = unit(existingId, template, MapCoordinate(0.1, 0.2))
        val created = unit(createdId, template, MapCoordinate(0.6, 0.7))

        history.record(CreateObjectAction(existing))
        history.record(AddTurnLedgerEntry("Turn 1"))
        history.record(EditObjectAction(existingId, emptyMap(), emptyMap()))
        history.record(CreateObjectAction(created))

        assertThat(history.turnOriginFor(existingId), equalTo(MapCoordinate(0.1, 0.2)))
        assertThat(history.turnOriginFor(createdId), equalTo(MapCoordinate(0.6, 0.7)))
    }

    @Test
    fun `shift override resets the movement origin until the next turn`() {
        val history = MapActionHistory()
        val unitId = MapPlacedObjectId(nextTestUuid())
        val template = MapViewerUnitTemplate(id = UnitTemplateId(nextTestUuid()), name = "Unit", bitmap = Bitmap32(1, 1))
        history.record(CreateObjectAction(unit(unitId, template, MapCoordinate(0.1, 0.2))))
        history.record(AddTurnLedgerEntry("Turn 1"))

        history.resetTurnOrigin(unitId, MapCoordinate(0.8, 0.9))

        assertThat(history.turnOriginFor(unitId), equalTo(MapCoordinate(0.8, 0.9)))
        history.record(AddTurnLedgerEntry("Turn 2"))
        assertThat(history.turnOriginFor(unitId), equalTo(MapCoordinate(0.1, 0.2)))
    }

    @Test
    fun `move-limit override retains the requested destination`() {
        val origin = MapCoordinate(0.5, 0.5)
        val requested = MapCoordinate(0.9, 0.9)
        val overridden = MoveLimit.constrain(
            origin = origin,
            requested = requested,
            limit = 1.0,
            mapWidth = 100,
            mapHeight = 100,
            mapUnitsPerPixel = 1.0,
            overrideLimit = true,
        )

        assertThat(overridden, equalTo(requested))
    }

    private fun unit(id: MapPlacedObjectId, template: MapViewerUnitTemplate, coordinate: MapCoordinate) =
        UnitSnapshot(
            id = id,
            unitTemplateId = template.id,
            template = template,
            instanceName = "Unit",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = coordinate,
            normalizedWidth = 0.01,
            normalizedHeight = 0.01,
        )
}
