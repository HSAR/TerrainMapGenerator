package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapViewerSidebarTest {

    @Test
    fun `sidebar uses supplied initial unit templates`() {
        val sidebar = MapViewerSidebar(
            root = Container(),
            viewportWidth = 1200,
            viewportHeight = 800,
            onUnitDropped = { _, _ -> },
            initialUnitTemplates = listOf(
                MapViewerUnitTemplate("Unit 1", Bitmap32(8, 8) { _, _ -> RGBA(255, 0, 0, 255) }),
                MapViewerUnitTemplate("Unit 2", Bitmap32(8, 8) { _, _ -> RGBA(0, 0, 255, 255) }),
            ),
        )

        assertThat(sidebar.unitTemplateCount, equalTo(2))
    }
}
