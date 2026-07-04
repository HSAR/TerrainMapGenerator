package io.hsar.mapgenerator.grid

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MgrsCoordTest {

    @Test
    fun `parsing right pads significant digits to metres`() {
        assertThat(MgrsCoord.parse("8")?.metres, equalTo(80_000.0))
        assertThat(MgrsCoord.parse("80")?.metres, equalTo(80_000.0))
        assertThat(MgrsCoord.parse("800")?.metres, equalTo(80_000.0))
        assertThat(MgrsCoord.parse("8000")?.metres, equalTo(80_000.0))
        assertThat(MgrsCoord.parse("80000")?.metres, equalTo(80_000.0))
    }

    @Test
    fun `parsing preserves leading zero precision`() {
        assertThat(MgrsCoord.parse("0840")?.metres, equalTo(8_400.0))
    }

    @Test
    fun `format trims trailing zeroes while preserving leading precision`() {
        assertThat(MgrsCoord.parse("0840")?.format(), equalTo("084"))
        assertThat(MgrsCoord.parse("-0840")?.format(), equalTo("-084"))
        assertThat(MgrsCoord.fromMetres(39_000).format(), equalTo("39"))
        assertThat(MgrsCoord.fromMetres(0).format(), equalTo("0"))
    }

    @Test
    fun `invalid values do not parse`() {
        assertThat(MgrsCoord.parse(""), equalTo(null))
        assertThat(MgrsCoord.parse("12.3"), equalTo(null))
        assertThat(MgrsCoord.parse("123456"), equalTo(null))
        assertThat(MgrsCoord.parse("ABC"), equalTo(null))
    }

    @Test
    fun `equivalent significant coordinates compare equal`() {
        assertThat(MgrsCoord.parse("8"), equalTo(MgrsCoord.parse("800")))
    }
}
