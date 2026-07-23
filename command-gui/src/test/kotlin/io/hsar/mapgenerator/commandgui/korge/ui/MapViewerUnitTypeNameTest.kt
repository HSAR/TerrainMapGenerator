package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerUnitTypeNameTest {
    @Test
    fun `live updates preserve spaces while confirmation trims surrounding whitespace`() {
        val draft = "Orbital  Defense "

        assertThat(MapViewerUnitTypeName.forLiveUpdate(draft), equalTo(draft))
        assertThat(MapViewerUnitTypeName.forConfirmation(draft), equalTo("Orbital  Defense"))
    }
}
