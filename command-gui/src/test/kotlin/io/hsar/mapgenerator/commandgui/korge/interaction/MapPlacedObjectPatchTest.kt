package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingPoint
import io.hsar.mapgenerator.commandgui.korge.drawing.RectangleDrawing
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.*

class MapPlacedObjectPatchTest {

    @Test
    fun `unit diff can be applied to recreate updated snapshot`() {
        val before = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4)),
            instanceName = "Alpha 1",
            destroyed = false,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.2,
        )
        val after = before.copy(
            instanceName = "Alpha 2",
            destroyed = true,
            mapCoordinate = MapCoordinate(0.4, 0.5),
        )

        val patch = MapPlacedObjectPatch.diff(before, after)

        assertThat(MapPlacedObjectPatch.apply(before, patch), equalTo(after))
    }

    @Test
    fun `drawing diff can be applied to recreate updated snapshot`() {
        val before = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.1, 0.1),
                end = MapDrawingPoint(0.2, 0.2),
                color = Color.RED,
            ),
        )
        val after = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            drawing = RectangleDrawing(
                start = MapDrawingPoint(0.3, 0.3),
                end = MapDrawingPoint(0.5, 0.6),
                color = Color.BLUE,
            ),
        )

        val patch = MapPlacedObjectPatch.diff(before, after)

        assertThat(MapPlacedObjectPatch.apply(before, patch), equalTo(after))
    }
}
