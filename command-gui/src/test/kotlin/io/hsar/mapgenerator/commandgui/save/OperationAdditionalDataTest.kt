package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogBounds
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class OperationAdditionalDataTest {
    @Test
    fun `ui state is additive and preserves unrelated additional data`() {
        val objectId = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val data = OperationAdditionalData.fromJson("""{"custom":"keep","sitrepContacts":[]}""".toByteArray())

        data.updateUiState(
            UiSessionState(
                selectedTool = MapViewerTool.DRAW,
                selectedObjectIds = listOf(objectId),
                dialogs = listOf(MapViewerPersistedDialog("note", "note-id", MapViewerDialogBounds(10.0, 20.0, 300.0, 400.0))),
            ),
        )

        val restored = OperationAdditionalData.fromJson(data.toJsonOrNull())

        assertThat(restored.uiState().selectedTool, equalTo(MapViewerTool.DRAW))
        assertThat(restored.uiState().selectedObjectIds, equalTo(listOf(objectId)))
        assertThat(restored.uiState().dialogs.single().bounds.width, equalTo(300.0))
        assertThat(String(data.toJsonOrNull()!!).contains("\"custom\" : \"keep\""), equalTo(true))
    }

    @Test
    fun `missing or malformed ui state uses safe defaults`() {
        val data = OperationAdditionalData.fromJson("""{"uiState":{"selectedTool":"UNKNOWN","dialogs":[{}]}}""".toByteArray())

        assertThat(data.uiState(), equalTo(UiSessionState()))
    }
}
