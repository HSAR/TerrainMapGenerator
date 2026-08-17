package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapCoordinate
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class OperationWorkspaceTest {
    private val unit = OperationUnit(
        id = OperationObjectId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        unitTemplateId = OperationUnitTemplateId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        instanceName = "Alpha",
        destroyed = false,
        facingDegrees = 90,
        coordinate = MapCoordinate(0.25, 0.75),
        normalizedWidth = 0.03,
        normalizedHeight = 0.03,
    )

    @Test
    fun `opening the same target activates one stable editor`() {
        // Arrange
        val initial = OperationWorkspaceState.default()

        // Act
        val opened = initial.open(unit.editorState()).open(unit.editorState())

        // Assert
        assertThat(opened.editors.count { it.kind == OperationEditorKind.UNIT }, equalTo(1))
        assertThat(opened.activeEditorId, equalTo(OperationEditorId("unit:${unit.id.value}")))
    }

    @Test
    fun `floating dialog opens without displacing a dock tab`() {
        // Arrange
        val docked = OperationWorkspaceState.default().open(OperationWorkspaceState.notesEditor(), OperationDockRegion.LEFT)
        val note = OperationNote(OperationNoteId(UUID.fromString("00000000-0000-0000-0000-000000000003")), "Orders", "Hold position")

        // Act
        val opened = docked.openFloating(note.editorState())

        // Assert
        assertThat(opened.dockContaining(OperationWorkspaceState.DEFAULT_NOTES_ID)?.activeEditorId, equalTo(OperationWorkspaceState.DEFAULT_NOTES_ID))
        assertThat(opened.dockContaining(OperationEditorId("note:${note.id.value}")), equalTo(null))
        assertThat(opened.activeEditorId, equalTo(OperationEditorId("note:${note.id.value}")))
    }

    @Test
    fun `switching modes retains draft floating state and dock placement`() {
        // Arrange
        val opened = OperationWorkspaceState.default().open(unit.editorState())
        val editorId = OperationEditorId("unit:${unit.id.value}")
        val bounds = OperationEditorBounds(40.0, 50.0, 600.0, 500.0)
        val edited = opened.updateEditor(editorId) { editor ->
            editor.copy(
                draft = (editor.draft as OperationEditorDraft.Unit).copy(name = "Working"),
                floating = editor.floating.copy(bounds = bounds, minimized = true),
            )
        }

        // Act
        val restored = edited.withMode(MapViewerWorkspaceMode.FLOATING).withMode(MapViewerWorkspaceMode.DOCKED)

        // Assert
        assertThat((restored.editor(editorId)!!.draft as OperationEditorDraft.Unit).name, equalTo("Working"))
        assertThat(restored.editor(editorId)!!.floating.bounds, equalTo(bounds))
        assertThat(restored.dockRegions.single().editorIds.contains(editorId), equalTo(true))
    }

    @Test
    fun `closing discards the editor and its draft while retaining sibling tabs`() {
        // Arrange
        val editorId = OperationEditorId("unit:${unit.id.value}")
        val opened = OperationWorkspaceState.default().open(unit.editorState())

        // Act
        val closed = opened.close(editorId)

        // Assert
        assertThat(closed.editor(editorId), equalTo(null))
        assertThat(closed.editors, equalTo(emptyList()))
        assertThat(closed.dockRegions.flatMap { it.editorIds }, equalTo(emptyList()))
    }

    @Test
    fun `dock placement and tab reordering retain every open editor`() {
        // Arrange
        val unitId = OperationEditorId("unit:${unit.id.value}")
        val opened = OperationWorkspaceState.default().open(unit.editorState())

        // Act
        val withNotes = opened.open(OperationWorkspaceState.notesEditor()).dock(OperationWorkspaceState.DEFAULT_NOTES_ID, OperationDockRegion.RIGHT)
        val moved = withNotes.dock(unitId, OperationDockRegion.BOTTOM, 0)
            .dock(unitId, OperationDockRegion.RIGHT, 0)
            .reorderDock(OperationDockRegion.RIGHT, listOf(OperationWorkspaceState.DEFAULT_NOTES_ID, unitId))

        // Assert
        val right = moved.dockRegions.single { it.region == OperationDockRegion.RIGHT }
        assertThat(right.editorIds, equalTo(listOf(OperationWorkspaceState.DEFAULT_NOTES_ID, unitId)))
        assertThat(moved.editors.map { it.id }.toSet(), equalTo(setOf(OperationWorkspaceState.DEFAULT_NOTES_ID, unitId)))
    }

    @Test
    fun `cycling and cross region docking retain the active draft`() {
        // Arrange
        val unitId = OperationEditorId("unit:${unit.id.value}")
        val opened = OperationWorkspaceState.default().open(OperationWorkspaceState.notesEditor()).open(unit.editorState()).updateEditor(unitId) { editor ->
            editor.copy(draft = (editor.draft as OperationEditorDraft.Unit).copy(name = "Unsaved"))
        }

        // Act
        val moved = opened.dock(unitId, OperationDockRegion.LEFT, 0)
        val cycled = moved.cycleDock(OperationDockRegion.RIGHT, 1)

        // Assert
        assertThat(moved.dockContaining(unitId)?.region, equalTo(OperationDockRegion.LEFT))
        assertThat((moved.editor(unitId)?.draft as OperationEditorDraft.Unit).name, equalTo("Unsaved"))
        assertThat(cycled.activeEditorId, equalTo(OperationWorkspaceState.DEFAULT_NOTES_ID))
        assertThat(cycled.activeDock()?.region, equalTo(OperationDockRegion.RIGHT))
    }

    @Test
    fun `new workspace is empty and menu editors have stable identities`() {
        val workspace = OperationWorkspaceState.default()
        val editors = listOf(
            OperationWorkspaceState.gameEditor(),
            OperationWorkspaceState.notesEditor(),
            OperationWorkspaceState.graphicTypesEditor(),
            OperationWorkspaceState.unitTypesEditor(),
        )

        assertThat(workspace.editors, equalTo(emptyList()))
        assertThat(editors.map { it.id.value }, equalTo(listOf("game", "operation-notes", "graphic-types", "unit-types")))
        assertThat(editors.map { it.target }, equalTo(listOf(OperationEditorTarget.Game, OperationEditorTarget.OperationNotes, OperationEditorTarget.GraphicTypes, OperationEditorTarget.UnitTypes)))
    }
}
