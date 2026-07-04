package io.hsar.mapgenerator.grid

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ReferenceSystemTest {
    @Test
    fun `unit icon widths follow reference system units`() {
        assertThat(ReferenceSystem.MGRS.defaultUnitIconWidthUnits, equalTo(100.0))
        assertThat(ReferenceSystem.EPSILON.defaultUnitIconWidthUnits, equalTo(2.0))
    }
}
