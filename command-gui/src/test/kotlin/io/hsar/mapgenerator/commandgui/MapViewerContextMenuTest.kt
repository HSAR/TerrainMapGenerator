package io.hsar.mapgenerator.commandgui

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class MapViewerContextMenuTest {

    @Test
    fun `context menu actions follow the replay object kind`() {
        val unit = MapViewerReplayObject(UUID.randomUUID(), MapViewerReplayObjectKind.UNIT, "Unit")
        val graphic = MapViewerReplayObject(UUID.randomUUID(), MapViewerReplayObjectKind.GRAPHIC, "Graphic")
        val drawing = MapViewerReplayObject(UUID.randomUUID(), MapViewerReplayObjectKind.DRAWING, "Drawing")

        assertThat(unit.contextMenuItems().map(MapViewerContextMenuItem::label), equalTo(listOf("Edit Unit", "Delete Unit", "SITREP")))
        assertThat(graphic.contextMenuItems().map(MapViewerContextMenuItem::label), equalTo(listOf("Edit Graphic", "Delete Graphic")))
        assertThat(drawing.contextMenuItems().map(MapViewerContextMenuItem::label), equalTo(listOf("Delete Drawing")))
    }
}
