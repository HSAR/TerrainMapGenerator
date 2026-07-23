package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.korge.interaction.EcmActivation
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerTool
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogBounds
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class OperationAdditionalDataTest {
    @Test
    fun `ECM activations round trip and clear before the next turn`() {
        val unit = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
        val data = OperationAdditionalData.fromJson(null)
        val activation = EcmActivation(3, unit, setOf("ecm-1"), 30.0, 6.0, 20.0, 1)
        data.ecm.add(activation)

        val restored = OperationAdditionalData.fromJson(data.toJsonOrNull())

        assertThat(restored.ecm.forTurn(3), equalTo(listOf(activation)))
        restored.ecm.clearBefore(4)
        assertThat(restored.ecm.forTurn(3), equalTo(emptyList()))
    }

    @Test
    fun `tactical fire usage is included in the additional-data save payload`() {
        val unit = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
        val data = OperationAdditionalData.fromJson(null)
        val key = TacticalFireKey(4, unit)
        data.tacticalFire.replace(key, TacticalFireUsage(setOf("beam-1"), 1))

        val restored = OperationAdditionalData.fromJson(data.toJsonOrNull())

        assertThat(restored.tacticalFire.usage(key), equalTo(TacticalFireUsage(setOf("beam-1"), 1)))
    }

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

    @Test
    fun `unit edit dialog state round trips through additional data`() {
        val dialog = MapViewerPersistedDialog(
            type = "unit-edit",
            targetId = "00000000-0000-0000-0000-000000000002",
            bounds = MapViewerDialogBounds(10.0, 20.0, 300.0, 400.0),
            draft = OperationSaveJson.objectMapper.readTree("""{"name":"Alpha","positionX":"12","positionY":"34"}"""),
        )
        val data = OperationAdditionalData.fromJson(null)

        data.updateUiState(UiSessionState(dialogs = listOf(dialog)))

        val restored = OperationAdditionalData.fromJson(data.toJsonOrNull()).uiState().dialogs.single()

        assertThat(restored.type, equalTo("unit-edit"))
        assertThat(restored.targetId, equalTo(dialog.targetId))
        assertThat(restored.draft?.path("name")?.textValue(), equalTo("Alpha"))
    }

    @Test
    fun `dialog window state round trips additively and missing state defaults safely`() {
        val dialog = MapViewerPersistedDialog(
            type = "note",
            targetId = "note-id",
            bounds = MapViewerDialogBounds(10.0, 20.0, 300.0, 400.0),
            minimized = true,
            pinned = true,
            pinAnchor = Vector2D(123.0, 456.0),
        )
        val data = OperationAdditionalData.fromJson(null)

        data.updateUiState(UiSessionState(dialogs = listOf(dialog)))

        val restored = OperationAdditionalData.fromJson(data.toJsonOrNull()).uiState().dialogs.single()
        val legacy = MapViewerPersistedDialog.fromJson(
            OperationSaveJson.objectMapper.readTree(
                """{"type":"note","bounds":{"left":1,"top":2,"width":300,"height":400}}""",
            ),
        )

        assertThat(restored.minimized, equalTo(true))
        assertThat(restored.pinned, equalTo(true))
        assertThat(restored.pinAnchor, equalTo(Vector2D(123.0, 456.0)))
        assertThat(legacy?.minimized, equalTo(false))
        assertThat(legacy?.pinned, equalTo(false))
        assertThat(legacy?.pinAnchor, equalTo(null))
    }
}
