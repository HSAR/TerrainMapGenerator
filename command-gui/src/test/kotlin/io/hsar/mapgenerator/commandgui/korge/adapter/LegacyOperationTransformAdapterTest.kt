package io.hsar.mapgenerator.commandgui.korge.adapter

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.sameInstance
import org.junit.jupiter.api.Test
import java.util.*

class LegacyOperationTransformAdapterTest {
    @Test
    fun `object projection preserves identity bitmap and custom properties`() {
        val templateId = UnitTemplateId(UUID(0L, 1L))
        val bitmap = Bitmap32(4, 4)
        val snapshot = UnitSnapshot(
            id = MapPlacedObjectId(UUID(0L, 2L)),
            unitTemplateId = templateId,
            template = MapViewerUnitTemplate(templateId, "Unit", bitmap, customProperties = mapOf("template" to "value")),
            instanceName = "Projected unit",
            destroyed = false,
            facingDegrees = 30,
            mapCoordinate = MapCoordinate(.4, .6),
            normalizedWidth = .1,
            normalizedHeight = .1,
            customProperties = mapOf("object" to "value"),
        )

        val content = listOf(snapshot).toOperationContent()
        val restored = content.objects.single().toLegacyObject(content) as UnitSnapshot

        assertThat(restored.id, equalTo(snapshot.id))
        assertThat(restored.customProperties, equalTo(snapshot.customProperties))
        assertThat(restored.template.customProperties, equalTo(snapshot.template.customProperties))
        assertThat(restored.template.bitmap, sameInstance(bitmap))
    }
}
