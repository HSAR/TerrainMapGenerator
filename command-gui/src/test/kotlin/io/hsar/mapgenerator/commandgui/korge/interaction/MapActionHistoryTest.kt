package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.ViewerGeometry
import io.hsar.mapgenerator.commandgui.korge.drawing.MapDrawingStore
import korlibs.korge.view.Container
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class MapActionHistoryTest {

    @Test
    fun `record enables undo and clears redo`() {
        val history = MapActionHistory()

        history.record(TestMapAction("first"))
        history.record(TestMapAction("second"))

        assertThat(history.canUndo, equalTo(true))
        assertThat(history.canRedo, equalTo(false))
    }

    @Test
    fun `recording after undo clears redo`() {
        val history = MapActionHistory()
        val root = Container()
        val controller = MapObjectController(
            unitManager = MapPlacedUnitManager(
                root = root,
                unitLayer = Container(),
                geometry = ViewerGeometry(100, 100, 0, 0),
                mapMetresPerPixel = 20.0,
                mapWidth = 100,
                mapHeight = 100,
                viewportProvider = { CameraViewport(50.0, 50.0, 1.0, 100, 100, 100, 100) },
                contextMenuManager = MapPlacedObjectContextMenuManager(root),
            ),
            drawingStore = MapDrawingStore(),
        )

        history.record(TestMapAction("first"))
        history.undo(controller)
        assertThat(history.canRedo, equalTo(true))
        history.record(TestMapAction("second"))

        assertThat(history.canRedo, equalTo(false))
    }

    private data class TestMapAction(
        val name: String,
    ) : MapAction {
        override fun undo(controller: MapObjectController) = Unit

        override fun redo(controller: MapObjectController) = Unit
    }
}
