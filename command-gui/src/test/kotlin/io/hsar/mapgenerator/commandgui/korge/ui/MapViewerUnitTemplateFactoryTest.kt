package io.hsar.mapgenerator.commandgui.korge.ui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test

class MapViewerUnitTemplateFactoryTest {

    @Test
    fun `defaults provide the two starter units`() {
        val templates = MapViewerUnitTemplateFactory.defaults()

        assertThat(templates.map { it.name }, contains("Unit 1", "Unit 2"))
    }
}
