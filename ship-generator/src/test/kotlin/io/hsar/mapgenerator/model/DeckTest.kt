package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.model.CompartmentType.BRIDGE
import io.hsar.mapgenerator.model.CompartmentType.CARGO
import io.hsar.mapgenerator.model.CompartmentType.CORRIDOR
import io.hsar.mapgenerator.model.CompartmentType.CREW_QUARTERS
import io.hsar.mapgenerator.model.CompartmentType.ENGINE
import io.hsar.mapgenerator.model.CompartmentType.HANGAR
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class DeckTest {

    @Test
    fun `name is correct when a single primary compartment is present on the deck`() {
        val objectUnderTest = createTestDeck(
            id = "00",
            compartmentSlices = listOf(
                createCompartmentSlice(HANGAR),
                createCompartmentSlice(CARGO),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("00 (Hangar)"))
    }

    @Test
    fun `name is correct when multiple primary compartments are present on the deck`() {
        val objectUnderTest = createTestDeck(
            id = "01",
            compartmentSlices = listOf(
                createCompartmentSlice(BRIDGE),
                createCompartmentSlice(HANGAR),
                createCompartmentSlice(ENGINE),
                createCompartmentSlice(ENGINE),
                createCompartmentSlice(CARGO),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("01 (Bridge/Engineering/Hangar)"))
    }

    @Test
    fun `name is correct when a single secondary compartment is present on the deck`() {
        val objectUnderTest = createTestDeck(
            id = "00",
            compartmentSlices = listOf(
                createCompartmentSlice(CARGO),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("00 (Cargo)"))
    }

    @Test
    fun `name is correct when multiple secondary compartments are present on the deck`() {
        val objectUnderTest = createTestDeck(
            id = "01",
            compartmentSlices = listOf(
                createCompartmentSlice(CARGO),
                createCompartmentSlice(CARGO),
                createCompartmentSlice(CREW_QUARTERS),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("01 (Cargo/Crew Quarters)"))
    }

    @Test
    fun `valid placement returns false correctly`() {
        val testDeck = Deck(
            "test deck",
            Rectangle(Point.ORIGIN, Point(2.0, 2.0)),
            listOf(
                CompartmentSlice("engine", "engine", ENGINE, Rectangle(Point.ORIGIN, Point(0.99, 0.99)).shape)
            )
        )
        val testRectangle = Rectangle(Point.ORIGIN, Point(0.8, 1.3))

        val result = testDeck.validPlacement(testRectangle)
        assertThat(result, equalTo(false))
    }

    private fun createTestDeck(id: String, compartmentSlices: List<CompartmentSlice>) =
        Deck(id, Rectangle(Point.ORIGIN, Point.ORIGIN), compartmentSlices)

    private fun createCompartmentSlice(compartmentType: CompartmentType) =
        CompartmentSlice("shouldn't appear", "shouldn't appear", compartmentType, emptyList())
}