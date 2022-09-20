package io.hsar.mapgenerator.model

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
        val objectUnderTest = Deck(
            id = "00",
            compartments = listOf(
                Compartment("shouldn't appear", HANGAR, emptyList()),
                Compartment("shouldn't appear", CARGO, emptyList()),
                Compartment("shouldn't appear", CORRIDOR, emptyList()),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("00 (Hangar)"))
    }

    @Test
    fun `name is correct when multiple primary compartments are present on the deck`() {
        val objectUnderTest = Deck(
            id = "01",
            compartments = listOf(
                Compartment("shouldn't appear", BRIDGE, emptyList()),
                Compartment("shouldn't appear", HANGAR, emptyList()),
                Compartment("shouldn't appear", ENGINE, emptyList()),
                Compartment("shouldn't appear", ENGINE, emptyList()),
                Compartment("shouldn't appear", CARGO, emptyList()),
                Compartment("shouldn't appear", CORRIDOR, emptyList()),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("01 (Bridge/Engineering/Hangar)"))
    }

    @Test
    fun `name is correct when a single secondary compartment is present on the deck`() {
        val objectUnderTest = Deck(
            id = "00",
            compartments = listOf(
                Compartment("shouldn't appear", CARGO, emptyList()),
                Compartment("shouldn't appear", CORRIDOR, emptyList()),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("00 (Cargo)"))
    }

    @Test
    fun `name is correct when multiple secondary compartments are present on the deck`() {
        val objectUnderTest = Deck(
            id = "01",
            compartments = listOf(
                Compartment("shouldn't appear", CARGO, emptyList()),
                Compartment("shouldn't appear", CARGO, emptyList()),
                Compartment("shouldn't appear", CREW_QUARTERS, emptyList()),
                Compartment("shouldn't appear", CORRIDOR, emptyList()),
            )
        )

        val result = objectUnderTest.name

        assertThat(result, equalTo("01 (Cargo/Crew Quarters)"))
    }
}