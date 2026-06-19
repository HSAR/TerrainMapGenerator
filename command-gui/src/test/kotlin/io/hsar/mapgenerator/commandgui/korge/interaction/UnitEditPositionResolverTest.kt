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
}
