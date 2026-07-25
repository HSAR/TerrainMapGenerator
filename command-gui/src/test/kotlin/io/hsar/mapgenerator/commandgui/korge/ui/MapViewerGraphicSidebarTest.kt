package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerGraphicTemplate
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerGraphicSidebarTest {

    @Test
    fun `read-only sidebar rejects user template mutations`() {
        val sidebar = MapViewerGraphicSidebar(
            root = Container(),
            dialogManager = MapViewerDialogManager(),
            contextMenuManager = ContextMenuManager(Container()),
            onGraphicDropped = { _, _ -> },
            onTemplateCreated = {},
            onTemplateDeleted = {},
            onTemplateEdited = { _, _ -> },
            canEdit = { false },
        )
        val templateId = GraphicTemplateId(UUID.randomUUID())
        val template = GraphicTemplateSnapshot(
            id = templateId,
            template = MapViewerGraphicTemplate(templateId, "Graphic", Bitmap32(4, 4)),
            index = 0,
        )

        sidebar.addTemplate(template)

        assertThat(sidebar.template(template.id), equalTo(null))
    }
}
