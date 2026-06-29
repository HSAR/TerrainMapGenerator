package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerSidebarTest {

    @Test
    fun `sidebar uses supplied initial unit templates`() {
        val sidebar = MapViewerSidebar(
            root = Container(),
            viewportWidth = 1200,
            viewportHeight = 800,
            onUnitDropped = { _, _ -> },
            dialogManager = MapViewerDialogManager(),
            initialUnitTemplates = listOf(
                MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000001")), "Unit 1", Bitmap32(8, 8) { _, _ -> RGBA(255, 0, 0, 255) }),
                MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000002")), "Unit 2", Bitmap32(8, 8) { _, _ -> RGBA(0, 0, 255, 255) }),
            ),
        )

        assertThat(sidebar.unitTemplateCount, equalTo(2))
    }
}
