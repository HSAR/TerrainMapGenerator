package io.hsar.mapgenerator.commandgui.korge.drawing

import io.hsar.mapgenerator.image.Palette
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

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

        store.commit(preview)

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

        store.commit(drawing)
        store.preview(CircleDrawing(MapDrawingPoint(5.0, 6.0), 0.25, Palette.Colours.RED))
        store.clearPreview()

        assertThat(store.state.preview, equalTo(null))
        assertThat(store.state.drawings.map { it.drawing }, equalTo(listOf(drawing)))
    }
}
