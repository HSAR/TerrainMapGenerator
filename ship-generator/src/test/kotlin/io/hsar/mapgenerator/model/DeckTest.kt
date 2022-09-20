package io.hsar.mapgenerator.model

import io.hsar.mapgenerator.model.CompartmentType.BRIDGE_ADVANCED
import io.hsar.mapgenerator.model.CompartmentType.CARGO_SMALL
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
        val objectUnderTest = Deck(
            id = "00",
            compartmentSlices = listOf(
                createCompartmentSlice(HANGAR),
                createCompartmentSlice(CARGO_SMALL),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("00 (Hangar)"))
    }

    @Test
    fun `name is correct when multiple primary compartments are present on the deck`() {
        val objectUnderTest = Deck(
            id = "01",
            compartmentSlices = listOf(
                createCompartmentSlice(BRIDGE_ADVANCED),
                createCompartmentSlice(HANGAR),
                createCompartmentSlice(ENGINE),
                createCompartmentSlice(ENGINE),
                createCompartmentSlice(CARGO_SMALL),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("01 (Bridge/Engineering/Hangar)"))
    }

    @Test
    fun `name is correct when a single secondary compartment is present on the deck`() {
        val objectUnderTest = Deck(
            id = "00",
            compartmentSlices = listOf(
                createCompartmentSlice(CARGO_SMALL),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("00 (Cargo)"))
    }

    @Test
    fun `name is correct when multiple secondary compartments are present on the deck`() {
        val objectUnderTest = Deck(
            id = "01",
            compartmentSlices = listOf(
                createCompartmentSlice(CARGO_SMALL),
                createCompartmentSlice(CARGO_SMALL),
                createCompartmentSlice(CREW_QUARTERS),
                createCompartmentSlice(CORRIDOR),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("01 (Cargo/Crew Quarters)"))
    }

    private fun createCompartmentSlice(compartmentType: CompartmentType) =
        CompartmentSlice("shouldn't appear", "shouldn't appear", compartmentType, emptyList())
}