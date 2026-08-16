package io.hsar.mapgenerator.commandgui.operation

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class OperationFireArcOverlayTest {
    @Test
    fun `single forward class two beam produces exact shared primitives`() {
        val primitives = OperationFireArcOverlayBuilder.build(
            facingDegrees = 90,
            weapons = listOf(OperationFireArcWeaponDefinition(arcs = listOf("F"), beamClass = 2)),
        )

        assertThat(primitives, equalTo(listOf(
            OperationFireArcPrimitive.SectorArc(6.0, 60.0, 120.0, 1.0, false),
            OperationFireArcPrimitive.SectorArc(12.0, 60.0, 120.0, 1.5, true),
            OperationFireArcPrimitive.Radial(12.0, 60.0, 1.5),
            OperationFireArcPrimitive.Radial(12.0, 120.0, 1.5),
        )))
    }

    @Test
    fun `all arc weapon produces circles without radial boundaries`() {
        val primitives = OperationFireArcOverlayBuilder.build(
            0,
            listOf(OperationFireArcWeaponDefinition(listOf("F", "FS", "AS", "A", "AP", "FP"), beamClass = 1)),
        )

        assertThat(primitives, equalTo(listOf(OperationFireArcPrimitive.Circle(6.0, 1.5, true))))
    }
}
