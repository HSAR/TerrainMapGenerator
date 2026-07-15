package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class EcmRulesTest {
    @Test
    fun `capability pools modules and halves for intensity`() {
        assertThat(EcmRules.capability(2, 1), closeTo(240.0, 0.0001))
        assertThat(EcmRules.capability(2, 3), closeTo(60.0, 0.0001))
        assertThat(EcmRules.manual(1, 60.0, 1)?.rangeU, closeTo(2.0, 0.0001))
        assertThat(EcmRules.manual(1, 180.0, 2), equalTo(null))
    }

    @Test
    fun `automatic targeting chooses highest intensity that retains ten degrees`() {
        assertThat(EcmRules.automatic(1, 2.0), equalTo(EcmShape(2.0, 15.0, 3)))
        assertThat(EcmRules.automatic(1, 12.0), equalTo(EcmShape(12.0, 10.0, 1)))
        assertThat(EcmRules.automatic(1, 12.1), equalTo(null))
    }

    @Test
    fun `manual range dragging snaps to valid intensity levels without dropping below one unit`() {
        assertThat(EcmRules.closestManualIntensity(1, 60.0, 1.4), equalTo(EcmShape(1.0, 60.0, 2)))
        assertThat(EcmRules.closestManualIntensity(1, 60.0, 0.2), equalTo(EcmShape(1.0, 60.0, 2)))
        assertThat(EcmRules.closestManualIntensity(2, 60.0, 1.4), equalTo(EcmShape(1.0, 60.0, 3)))
    }

    @Test
    fun `sector containment remains on global bearing when emitter turns`() {
        val emitter = unit(MapCoordinate(0.5, 0.5), facing = 350)
        val query = UnitQueryService(listOf(emitter), 100, 100, 1.0)
        val activation = EcmActivation(1, emitter.id, setOf("ecm"), 0.0, 10.0, 12.0, 1)
        val turnedEmitter = emitter.copy(facingDegrees = 90)

        assertThat(EcmRules.contains(activation, emitter, MapCoordinate(0.5, 0.4), query), equalTo(true))
        assertThat(EcmRules.contains(activation, turnedEmitter, MapCoordinate(0.5, 0.4), query), equalTo(true))
        assertThat(EcmRules.contains(activation, emitter, MapCoordinate(0.6, 0.5), query), equalTo(false))
    }

    @Test
    fun `manual width changes snap range to quarter units without changing intensity`() {
        val narrow = EcmRules.resizedManual(1, 35.0, 1)!!
        val wide = EcmRules.resizedManual(1, 70.0, 1)!!

        assertThat(narrow.rangeU, equalTo(3.5))
        assertThat(narrow.widthDegrees, closeTo(120.0 / 3.5, 0.0001))
        assertThat(narrow.intensity, equalTo(1))
        assertThat(wide.rangeU, equalTo(1.75))
        assertThat(wide.widthDegrees, closeTo(120.0 / 1.75, 0.0001))
        assertThat(wide.intensity, equalTo(1))
        assertThat(EcmRules.resizedManual(1, 150.0, 2), equalTo(EcmShape(1.0, 60.0, 2)))
    }

    @Test
    fun `manual ECM arc follows global bearing relative to the emitter`() {
        assertThat(TacticalTargetingResolver.arcForBearing(0.0, 0), equalTo("F"))
        assertThat(TacticalTargetingResolver.arcForBearing(60.0, 0), equalTo("FS"))
        assertThat(TacticalTargetingResolver.arcForBearing(359.0, 0), equalTo("F"))
        assertThat(TacticalTargetingResolver.arcForBearing(0.0, 60), equalTo("FP"))
    }

    private fun unit(position: MapCoordinate, facing: Int) = UnitSnapshot(
        MapPlacedObjectId(UUID(0, 1)), UnitTemplateId(UUID(0, 2)),
        MapViewerUnitTemplate(UnitTemplateId(UUID(0, 2)), "Ship", Bitmap32(1, 1)),
        "Ship", false, facing, position, 0.01, 0.01, emptyMap(),
    )
}
