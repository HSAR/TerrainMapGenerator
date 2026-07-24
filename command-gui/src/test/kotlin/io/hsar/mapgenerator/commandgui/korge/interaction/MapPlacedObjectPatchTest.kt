package io.hsar.mapgenerator.commandgui.korge.interaction

import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
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
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.2,
        )
        val after = before.copy(
            instanceName = "Alpha 2",
            destroyed = true,
            facingDegrees = 90,
            mapCoordinate = MapCoordinate(0.4, 0.5),
        )

        val patch = MapPlacedObjectPatch.diff(before, after)

        assertThat(MapPlacedObjectPatch.apply(before, patch), equalTo(after))
    }

    @Test
    fun `unit facing can be cleared by applying a nullable patch value`() {
        val before = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(
                UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                "Unit",
                Bitmap32(4, 4),
                affiliation = UnitAffiliation.Friend,
            ),
            instanceName = "Alpha 1",
            destroyed = false,
            facingDegrees = 270,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.2,
        )
        val after = before.copy(facingDegrees = null)

        val patch = MapPlacedObjectPatch.diff(before, after)

        assertThat(MapPlacedObjectPatch.apply(before, patch), equalTo(after))
    }

    @Test
    fun `unit affiliation diff can be applied to recreate updated snapshot`() {
        val before = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000013")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(
                UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                "Unit",
                Bitmap32(4, 4),
                affiliation = UnitAffiliation.Friend,
            ),
            instanceName = "Alpha 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.2,
        )
        val after = before.copy(template = before.template.copy(affiliation = UnitAffiliation.Hostile))

        val patch = MapPlacedObjectPatch.diff(before, after)

        assertThat(patch["affiliation"], equalTo(TextNode("Hostile")))
        assertThat(MapPlacedObjectPatch.apply(before, patch), equalTo(after))
    }

    @Test
    fun `unit custom property diff can be applied and reversed`() {
        val before = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000004")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4)),
            instanceName = "Alpha 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.2,
            customProperties = mapOf("callsign" to "A1", "obsolete" to "yes"),
        )
        val after = before.copy(customProperties = mapOf("callsign" to "A2", "morale" to "7"))

        val forward = MapPlacedObjectPatch.diff(before, after)
        val backward = MapPlacedObjectPatch.diff(after, before)

        assertThat(forward["callsign"], equalTo(TextNode("A2")))
        assertThat(forward["morale"], equalTo(TextNode("7")))
        assertThat(forward["obsolete"], equalTo(null))
        assertThat(MapPlacedObjectPatch.apply(before, forward), equalTo(after))
        assertThat(MapPlacedObjectPatch.apply(after, backward), equalTo(before))
    }

    @Test
    fun `unknown unit patch keys update custom properties without shadowing known fields`() {
        val before = UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000005")),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            template = MapViewerUnitTemplate(UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Unit", Bitmap32(4, 4)),
            instanceName = "Alpha 1",
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(0.2, 0.3),
            normalizedWidth = 0.1,
            normalizedHeight = 0.2,
            customProperties = mapOf("morale" to "steady"),
        )

        val after = MapPlacedObjectPatch.apply(
            before,
            mapOf(
                "instanceName" to TextNode("Alpha 2"),
                "morale" to IntNode(5),
                "obsolete" to null,
            ),
        ) as UnitSnapshot

        assertThat(after.instanceName, equalTo("Alpha 2"))
        assertThat(after.customProperties, equalTo(mapOf("morale" to "5")))
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

    @Test
    fun `drawing display name custom property can be applied and reversed`() {
        val before = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000006")),
            drawing = RectangleDrawing(MapDrawingPoint(0.1, 0.1), MapDrawingPoint(0.2, 0.2), Color.RED),
            customProperties = mapOf("displayName" to "Old name"),
        )
        val after = before.copy(customProperties = mapOf("displayName" to "New name"))

        val forward = MapPlacedObjectPatch.diff(before, after)
        val backward = MapPlacedObjectPatch.diff(after, before)

        assertThat(forward["displayName"], equalTo(TextNode("New name")))
        assertThat(MapPlacedObjectPatch.apply(before, forward), equalTo(after))
        assertThat(MapPlacedObjectPatch.apply(after, backward), equalTo(before))
    }

    @Test
    fun `drawing display name can be cleared by a custom property patch`() {
        val before = DrawingSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000007")),
            drawing = RectangleDrawing(MapDrawingPoint(0.1, 0.1), MapDrawingPoint(0.2, 0.2), Color.RED),
            customProperties = mapOf("displayName" to "Temporary"),
        )

        val after = MapPlacedObjectPatch.apply(before, mapOf("displayName" to null)) as DrawingSnapshot

        assertThat(after.customProperties, equalTo(emptyMap()))
    }

    @Test
    fun `graphic display name custom property can be applied and reversed`() {
        val templateId = GraphicTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000020"))
        val before = GraphicSnapshot(
            id = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000008")),
            graphicTemplateId = templateId,
            template = MapViewerGraphicTemplate(templateId, "Graphic", Bitmap32(4, 4)),
            instanceName = "Graphic",
            mapCoordinate = MapCoordinate(0.5, 0.5),
            normalizedWidth = 0.1,
            normalizedHeight = 0.1,
        )
        val after = before.copy(customProperties = mapOf("displayName" to "Objective"))

        val forward = MapPlacedObjectPatch.diff(before, after)
        val backward = MapPlacedObjectPatch.diff(after, before)

        assertThat(MapPlacedObjectPatch.apply(before, forward), equalTo(after))
        assertThat(MapPlacedObjectPatch.apply(after, backward), equalTo(before))
    }
}
