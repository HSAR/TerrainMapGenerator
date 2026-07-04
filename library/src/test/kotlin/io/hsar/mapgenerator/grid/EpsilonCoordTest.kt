package io.hsar.mapgenerator.grid

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class EpsilonCoordTest {
    @Test
    fun `parses integer unit coordinates`() {
        assertThat(EpsilonCoord.parse("123")?.units, equalTo(123))
        assertThat(EpsilonCoord.parse("-45")?.units, equalTo(-45))
    }

    @Test
    fun `parses sub unit coordinates`() {
        assertThat(EpsilonCoord.parse("12.75")?.preciseUnits, equalTo(12.75))
        assertThat(EpsilonCoord.fromUnits(12.75).format(), equalTo("12.75"))
    }

    @Test
    fun `rejects non numeric coordinates`() {
        assertThat(EpsilonCoord.parse(""), equalTo(null))
        assertThat(EpsilonCoord.parse("ABC"), equalTo(null))
    }

    @Test
    fun `reference system parses epsilon starts as epsilon coords`() {
        assertThat(ReferenceSystem.EPSILON.parseCoord("1200"), equalTo(EpsilonCoord.fromUnits(1200)))
    }

    @Test
    fun `epsilon y designators advance through single and double letters`() {
        assertThat(EpsilonGridDesignators.yDesignator(0), equalTo("A"))
        assertThat(EpsilonGridDesignators.yDesignator(25), equalTo("Z"))
        assertThat(EpsilonGridDesignators.yDesignator(26), equalTo("AA"))
        assertThat(EpsilonGridDesignators.yDesignator(27), equalTo("AB"))
        assertThat(EpsilonGridDesignators.yDesignator(701), equalTo("ZZ"))
    }

    @Test
    fun `epsilon x designators are one to one hundred`() {
        assertThat(EpsilonGridDesignators.xDesignator(0), equalTo("1"))
        assertThat(EpsilonGridDesignators.xDesignator(99), equalTo("100"))
        assertThat(EpsilonGridDesignators.xIndex("0"), equalTo(null))
        assertThat(EpsilonGridDesignators.xIndex("101"), equalTo(null))
    }

    @Test
    fun `reference system parses and formats epsilon axis starts as designators`() {
        assertThat(ReferenceSystem.EPSILON.parseCoord("4", CoordAxis.X, ReferenceSystem.EPSILON.gridScale), equalTo(EpsilonCoord.fromUnits(60)))
        assertThat(ReferenceSystem.EPSILON.parseCoord("CD", CoordAxis.Y, ReferenceSystem.EPSILON.gridScale), equalTo(EpsilonCoord.fromUnits(1_620)))
        assertThat(ReferenceSystem.EPSILON.formatCoord(EpsilonCoord.fromUnits(60), CoordAxis.X, ReferenceSystem.EPSILON.gridScale), equalTo("4"))
        assertThat(ReferenceSystem.EPSILON.formatCoord(EpsilonCoord.fromUnits(1_640), CoordAxis.Y, ReferenceSystem.EPSILON.gridScale), equalTo("CE"))
    }
}
