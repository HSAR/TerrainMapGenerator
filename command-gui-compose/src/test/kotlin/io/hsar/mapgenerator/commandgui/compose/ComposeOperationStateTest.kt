package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapViewerLedgerReplay
import io.hsar.mapgenerator.commandgui.MapViewerReplayObject
import io.hsar.mapgenerator.commandgui.MapViewerReplayObjectKind
import io.hsar.mapgenerator.commandgui.MapViewport
import io.hsar.mapgenerator.commandgui.ViewerPoint
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class ComposeOperationStateTest {

    @Test
    fun `keeps tool selection object selection and ledger cursor in one immutable state`() {
        val objectId = UUID.fromString("00000000-0000-0000-0000-000000000001")

        val state = ComposeOperationState()
            .selectTool(ComposeMapTool.DRAW)
            .selectObject(objectId)
            .moveLedgerCursor(4)

        assertThat(state.activeTool, equalTo(ComposeMapTool.DRAW))
        assertThat(state.selectedObjectId, equalTo(objectId))
        assertThat(state.ledgerCursor, equalTo(4))
    }

    @Test
    fun `changing tools preserves the current selection and cursor`() {
        val objectId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val state = ComposeOperationState(selectedObjectId = objectId, ledgerCursor = 2)

        val updated = state.selectTool(ComposeMapTool.MEASURE)

        assertThat(updated.activeTool, equalTo(ComposeMapTool.MEASURE))
        assertThat(updated.selectedObjectId, equalTo(objectId))
        assertThat(updated.ledgerCursor, equalTo(2))
    }

    @Test
    fun `selects the topmost replay marker within its screen hit target`() {
        val first = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val second = UUID.fromString("00000000-0000-0000-0000-000000000004")
        val replay = MapViewerLedgerReplay.EMPTY.copy(
            objects = listOf(
                MapViewerReplayObject(first, MapViewerReplayObjectKind.UNIT, "First", x = 0.5, y = 0.5),
                MapViewerReplayObject(second, MapViewerReplayObjectKind.GRAPHIC, "Second", x = 0.5, y = 0.5),
            ),
        )

        val selected = replay.objectAtScreen(
            screenPoint = ViewerPoint(100.0, 100.0),
            viewport = MapViewport.centered(200.0, 200.0, 200, 200),
            mapOffsetX = 0.0,
            mapOffsetY = 0.0,
            mapWidth = 200,
            mapHeight = 200,
        )

        assertThat(selected?.id, equalTo(second))
    }
}
