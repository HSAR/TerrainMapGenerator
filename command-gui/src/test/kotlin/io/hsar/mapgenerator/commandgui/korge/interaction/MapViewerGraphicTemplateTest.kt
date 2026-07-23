package io.hsar.mapgenerator.commandgui.korge.interaction

import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerGraphicTemplateTest {
    @Test
    fun `normalized size is stored on the template for future instances`() {
        val id = GraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000140"))
        val original = MapViewerGraphicTemplate(id, "Graphic", Bitmap32(100, 50))

        val resized = original.withNormalizedSize(width = 0.08, height = 0.04)

        assertThat(resized.normalizedSize(), equalTo(GraphicTemplateSize(0.08, 0.04)))
        assertThat(original.normalizedSize(), equalTo(null))
    }
}
