package io.hsar.mapgenerator.commandgui.korge.interaction

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class UnitEditPositionResolverTest {

    @Test
    fun `unchanged position fields preserve the current coordinate exactly`() {
        val current = MapCoordinate(0.123456789, 0.987654321)

        val resolved = UnitEditPositionResolver.resolve(
            currentCoordinate = current,
            initialPositionX = "0123",
            initialPositionY = "0456",
            newPositionX = "0123",
            newPositionY = "0456",
            parse = { _, _ -> error("parse should not be called when fields are unchanged") },
        )

        assertThat(resolved, equalTo(current))
    }

    @Test
    fun `changed position fields are reparsed`() {
        val current = MapCoordinate(0.0, 0.0)
        val updated = MapCoordinate(0.4, 0.6)

        val resolved = UnitEditPositionResolver.resolve(
            currentCoordinate = current,
            initialPositionX = "0123",
            initialPositionY = "0456",
            newPositionX = "0124",
            newPositionY = "0456",
            parse = { _, _ -> updated },
        )

        assertThat(resolved, equalTo(updated))
    }

    @Test
    fun `blank facing input clears facing`() {
        assertThat(UnitFacingResolver.parse(" ").degrees, equalTo(null))
        assertThat(UnitFacingResolver.parse(" ").isValid, equalTo(true))
    }

    @Test
    fun `facing input normalizes full circle bearings`() {
        assertThat(UnitFacingResolver.parse("0").degrees, equalTo(0))
        assertThat(UnitFacingResolver.parse("90").degrees, equalTo(90))
        assertThat(UnitFacingResolver.parse("359").degrees, equalTo(359))
        assertThat(UnitFacingResolver.parse("360").degrees, equalTo(0))
    }

    @Test
    fun `non numeric facing input is invalid`() {
        assertThat(UnitFacingResolver.parse("north").isValid, equalTo(false))
    }
}
