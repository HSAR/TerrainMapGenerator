package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.persistence.LegacyOperationAdditionalData
import io.hsar.mapgenerator.commandgui.korge.persistence.TacticalFireKey
import io.hsar.mapgenerator.commandgui.korge.persistence.TacticalFireUsage
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerDialogBounds
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerPersistedDialog
import io.hsar.mapgenerator.commandgui.operation.OperationEditorBounds
import io.hsar.mapgenerator.commandgui.operation.OperationEcmActivation
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorTarget
import io.hsar.mapgenerator.commandgui.operation.OperationFloatingEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationImageDraft
import io.hsar.mapgenerator.commandgui.operation.OperationPropertyDraft
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationNoteDraft
import io.hsar.mapgenerator.commandgui.operation.OperationNoteId
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState
import io.hsar.mapgenerator.commandgui.operation.editorState
import korlibs.io.lang.toByteArray
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*
import java.util.Collections.emptyList

class OperationAdditionalDataTest {
    @Test
    fun `ECM activations round trip and clear before the next turn`() {
        val unit = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
        val data = LegacyOperationAdditionalData.fromJson(null)
        val activation = OperationEcmActivation(3, unit, setOf("ecm-1"), 30.0, 6.0, 20.0, 1)
        data.ecm.add(activation)

        val restored = LegacyOperationAdditionalData.fromJson(data.toJsonOrNull())

        assertThat(restored.ecm.forTurn(3), equalTo(listOf(activation)))
        restored.ecm.clearBefore(4)
        assertThat(restored.ecm.forTurn(3), equalTo(emptyList()))
    }

    @Test
    fun `tactical fire usage is included in the additional-data save payload`() {
        val unit = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
        val data = LegacyOperationAdditionalData.fromJson(null)
        val key = TacticalFireKey(4, unit)
        data.tacticalFire.replace(key, TacticalFireUsage(setOf("beam-1"), 1))

        val restored = LegacyOperationAdditionalData.fromJson(data.toJsonOrNull())

        assertThat(restored.tacticalFire.usage(key), equalTo(TacticalFireUsage(setOf("beam-1"), 1)))
    }

    @Test
    fun `ui state is additive and preserves unrelated additional data`() {
        val objectId = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
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
            pinAnchor = ViewerPoint(123.0, 456.0),
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
        assertThat(restored.pinAnchor, equalTo(ViewerPoint(123.0, 456.0)))
        assertThat(legacy?.minimized, equalTo(false))
        assertThat(legacy?.pinned, equalTo(false))
        assertThat(legacy?.pinAnchor, equalTo(null))
    }

    @Test
    fun `version two workspace state round trips while legacy dialogs remain present`() {
        // Arrange
        val dialog = MapViewerPersistedDialog(
            type = "notes",
            bounds = MapViewerDialogBounds(10.0, 20.0, 300.0, 400.0),
        )
        val data = OperationAdditionalData.fromJson("""{"custom":"keep"}""".toByteArray())
        val state = UiSessionState(
            dialogs = listOf(dialog),
            workspaceMode = MapViewerWorkspaceMode.DOCKED,
            dockPanels = listOf(
                DockPanelSessionState(
                    region = "right",
                    size = 320.0,
                    tabIds = listOf("notes", "unit-edit:1"),
                    activeTabId = "notes",
                    collapsed = false,
                ),
            ),
            enabledLayerIds = setOf("GRID", "map"),
            openEditorIds = listOf("notes", "unit-edit:1"),
            drafts = mapOf("notes" to "working text"),
        )

        // Act
        data.updateUiState(state)
        val json = data.toJsonOrNull()!!
        val restored = OperationAdditionalData.fromJson(json).uiState()

        // Assert
        assertThat(restored, equalTo(state))
        assertThat(OperationSaveJson.objectMapper.readTree(json).path("uiState").path("schemaVersion").intValue(), equalTo(6))
        assertThat(restored.dialogs.single(), equalTo(dialog))
        assertThat(String(json).contains("\"custom\" : \"keep\""), equalTo(true))
    }

    @Test
    fun `unversioned workspace state defaults to floating compatibility mode`() {
        // Arrange / Act
        val restored = OperationAdditionalData.fromJson(
            """{"uiState":{"selectedTool":"SELECT","selectedObjectIds":[],"dialogs":[]}}""".toByteArray(),
        ).uiState()

        // Assert
        assertThat(restored.workspaceMode, equalTo(MapViewerWorkspaceMode.FLOATING))
    }

    @Test
    fun `operation without workspace metadata starts with notes in the right dock`() {
        // Arrange / Act
        val state = OperationAdditionalData.fromJson(null).operationUiState()

        // Assert
        assertThat(state.workspace.mode, equalTo(MapViewerWorkspaceMode.DOCKED))
        assertThat(state.workspace.editors.single().id, equalTo(OperationWorkspaceState.DEFAULT_NOTES_ID))
        assertThat(state.workspace.dockRegions.single().region.name, equalTo("RIGHT"))
    }

    @Test
    fun `version three typed workspace and legacy unit dialog round trip together`() {
        // Arrange
        val unit = OperationUnit(
            OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000022")),
            OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000023")),
            "Alpha", false, 45, MapCoordinate(0.2, 0.3), 0.03, 0.03,
        )
        val editor = unit.editorState(OperationEditorBounds(12.0, 34.0, 520.0, 560.0)).let { value ->
            value.copy(
                draft = (value.draft as OperationEditorDraft.Unit).copy(name = "Working Alpha"),
                floating = value.floating.copy(minimized = true, pinned = true, mapAnchor = ViewerPoint(40.0, 50.0)),
            )
        }
        val data = OperationAdditionalData.fromJson("""{"uiState":{"customUi":"keep"},"custom":"keep"}""".toByteArray())

        // Act
        data.updateOperationUiState(
            io.hsar.mapgenerator.commandgui.operation.OperationUiState(
                workspace = OperationWorkspaceState.default().open(editor).copy(mode = MapViewerWorkspaceMode.FLOATING),
            ),
        )
        val json = data.toJsonOrNull()!!
        val restored = OperationAdditionalData.fromJson(json)

        // Assert
        val restoredEditor = restored.operationUiState().workspace.editor(editor.id)!!
        assertThat((restoredEditor.draft as OperationEditorDraft.Unit).name, equalTo("Working Alpha"))
        assertThat(restoredEditor.floating.mapAnchor, equalTo(ViewerPoint(40.0, 50.0)))
        val unitDialog = restored.uiState().dialogs.single { it.type == "unit-edit" }
        assertThat(unitDialog.type, equalTo("unit-edit"))
        assertThat(unitDialog.draft!!.path("name").textValue(), equalTo("Working Alpha"))
        assertThat(OperationSaveJson.objectMapper.readTree(json).path("uiState").path("customUi").textValue(), equalTo("keep"))
    }

    @Test
    fun `version four preserves an unfinished unit template draft with embedded image`() {
        val image = OperationImageDraft.fromBytes("scout.svg", "<svg/>".toByteArray())
        val editor = OperationEditorState(
            OperationEditorId("unit-template:new"), OperationEditorKind.UNIT_TEMPLATE,
            OperationEditorTarget.NewUnitTemplate,
            OperationEditorDraft.UnitTemplate(OperationUnitTemplateDraft("Scout", replacementImage = image, properties = listOf(OperationPropertyDraft("Class", "F")))),
            OperationFloatingEditorState(OperationEditorBounds(10.0, 20.0, 520.0, 600.0)),
        )
        val data = OperationAdditionalData.fromJson(null)

        data.updateOperationUiState(io.hsar.mapgenerator.commandgui.operation.OperationUiState(workspace = OperationWorkspaceState.default().open(editor)))
        val restored = OperationAdditionalData.fromJson(data.toJsonOrNull()).operationUiState().workspace.editor(editor.id)!!

        assertThat(restored, equalTo(editor))
    }

    @Test
    fun `version five preserves an unfinished graphic template draft with dimensions and embedded image`() {
        val image = OperationImageDraft.fromBytes("arrow.svg", "<svg/>".toByteArray())
        val editor = OperationEditorState(
            OperationEditorId("graphic-template:new"), OperationEditorKind.GRAPHIC_TEMPLATE,
            OperationEditorTarget.NewGraphicTemplate,
            OperationEditorDraft.GraphicTemplate(OperationGraphicTemplateDraft("Arrow", "3.2", "1.6", image, listOf(OperationPropertyDraft("role", "axis")))),
            OperationFloatingEditorState(OperationEditorBounds(10.0, 20.0, 520.0, 620.0)),
        )
        val data = OperationAdditionalData.fromJson("""{"custom":"keep"}""".toByteArray())

        data.updateOperationUiState(io.hsar.mapgenerator.commandgui.operation.OperationUiState(workspace = OperationWorkspaceState.default().open(editor)))
        val bytes = data.toJsonOrNull()!!
        val restored = OperationAdditionalData.fromJson(bytes).operationUiState().workspace.editor(editor.id)!!

        assertThat(restored, equalTo(editor))
        assertThat(OperationSaveJson.objectMapper.readTree(bytes).path("uiState").path("schemaVersion").intValue(), equalTo(6))
        assertThat(OperationSaveJson.objectMapper.readTree(bytes).path("custom").textValue(), equalTo("keep"))
    }

    @Test
    fun `legacy unit edit migrates to a typed functional editor`() {
        // Arrange
        val json =
            """{"uiState":{"dialogs":[{"type":"unit-edit","targetId":"00000000-0000-0000-0000-000000000024","bounds":{"left":10,"top":20,"width":520,"height":560},"draft":{"name":"Legacy","positionX":"0.1","positionY":"0.2","facing":"90","destroyed":false}}]}}"""

        // Act
        val state = OperationAdditionalData.fromJson(json.toByteArray()).operationUiState()

        // Assert
        val editor = state.workspace.editors.single()
        assertThat((editor.draft as OperationEditorDraft.Unit).name, equalTo("Legacy"))
        assertThat(editor.kind.value, equalTo("unit"))
    }

    @Test
    fun `version six preserves an unfinished note draft and ordered missing references`() {
        val noteId = OperationNoteId(UUID.fromString("00000000-0000-0000-0000-000000000031"))
        val first = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000032"))
        val missing = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000033"))
        val editor = OperationEditorState(
            OperationEditorId("note:new:${noteId.value}"), OperationEditorKind.NOTE,
            OperationEditorTarget.NewNote(noteId),
            OperationEditorDraft.Note(OperationNoteDraft(noteId, "Orders", "Hold\r\nPosition", listOf(first, missing))),
            OperationFloatingEditorState(OperationEditorBounds(20.0, 30.0, 560.0, 640.0), minimized = true),
        )
        val data = OperationAdditionalData.fromJson(null)

        data.updateOperationUiState(io.hsar.mapgenerator.commandgui.operation.OperationUiState(workspace = OperationWorkspaceState.default().open(editor)))
        val bytes = data.toJsonOrNull()!!
        val restored = OperationAdditionalData.fromJson(bytes).operationUiState().workspace.editor(editor.id)

        assertThat(restored, equalTo(editor))
        assertThat(OperationSaveJson.objectMapper.readTree(bytes).path("uiState").path("schemaVersion").intValue(), equalTo(6))
        assertThat(OperationAdditionalData.fromJson(bytes).uiState().dialogs.none { it.type == "note" && it.targetId == noteId.value.toString() }, equalTo(true))
    }

    @Test
    fun `legacy note dialogs migrate to typed editors and retain unsupported draft fields`() {
        val noteId = "00000000-0000-0000-0000-000000000034"
        val objectId = "00000000-0000-0000-0000-000000000035"
        val json = """{"uiState":{"workspaceMode":"FLOATING","dialogs":[
            {"type":"notes","bounds":{"left":1,"top":2,"width":320,"height":400}},
            {"type":"note","targetId":"$noteId","bounds":{"left":10,"top":20,"width":560,"height":640},"draft":{"title":"Legacy","body":"Draft","referencesExpanded":true,"references":["$objectId"]}}
        ]}}"""
        val data = OperationAdditionalData.fromJson(json.toByteArray())

        val state = data.operationUiState()
        val editor = state.workspace.editor(OperationEditorId("note:$noteId"))!!
        data.updateOperationUiState(state)
        val projected = data.uiState().dialogs.single { it.type == "note" }

        assertThat((editor.draft as OperationEditorDraft.Note).value.referencedObjectIds.map { it.value.toString() }, equalTo(listOf(objectId)))
        assertThat(projected.draft!!.path("referencesExpanded").booleanValue(), equalTo(true))
        assertThat(projected.bounds, equalTo(MapViewerDialogBounds(10.0, 20.0, 560.0, 640.0)))
    }

    @Test
    fun `legacy Compose scratchpad text is deliberately discarded`() {
        val json = """{"uiState":{"schemaVersion":5,"workspace":{"mode":"DOCKED","activeEditorId":"operation-notes","editors":[{"id":"operation-notes","kind":"operation-notes","target":{"type":"operation-notes"},"draft":{"body":"discard me"},"floating":{"bounds":{"left":1,"top":2,"width":340,"height":360},"minimized":false,"pinned":false}}],"dockRegions":[{"region":"RIGHT","size":320,"editorIds":["operation-notes"],"activeEditorId":"operation-notes","collapsed":false}]}}}"""

        val workspace = OperationAdditionalData.fromJson(json.toByteArray()).operationUiState().workspace

        assertThat((workspace.editor(OperationWorkspaceState.DEFAULT_NOTES_ID)!!.draft as OperationEditorDraft.Notes).body, equalTo(""))
    }
}
