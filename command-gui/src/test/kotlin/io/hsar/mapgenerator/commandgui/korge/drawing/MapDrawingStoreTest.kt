package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.commandgui.korge.interaction.DrawingSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapDrawingStoreTest {

    @Test
    fun `stores committed drawings separately from the active preview`() {
        val store = MapDrawingStore()
        val preview = RectangleDrawing(
            start = MapDrawingPoint(10.0, 20.0),
            end = MapDrawingPoint(30.0, 40.0),
            color = Palette.Colours.RED,
        )

        store.preview(preview)

        assertThat(store.state.preview, equalTo(preview))
        assertThat(store.state.drawings, equalTo(emptyList()))

        store.upsert(DrawingSnapshot(MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")), preview))

        assertThat(store.state.preview, equalTo(null))
        assertThat(store.state.drawings.map { it.drawing }, equalTo(listOf(preview)))
    }

    @Test
    fun `clearing preview leaves committed drawings in place`() {
        val store = MapDrawingStore()
        val drawing = FreeformDrawing(
            points = listOf(MapDrawingPoint(1.0, 2.0), MapDrawingPoint(3.0, 4.0)),
            color = Palette.Colours.RED,
        )

        store.upsert(DrawingSnapshot(MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")), drawing))
        store.preview(OvalDrawing(MapDrawingPoint(6.0, 7.0), 1.0, 1.0, Palette.Colours.RED))
        store.clearPreview()

        assertThat(store.state.preview, equalTo(null))
        assertThat(store.state.drawings.map { it.drawing }, equalTo(listOf(drawing)))
    }
}
