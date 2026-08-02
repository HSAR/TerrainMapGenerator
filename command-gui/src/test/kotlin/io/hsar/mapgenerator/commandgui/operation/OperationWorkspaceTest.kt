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
        assertThat(closed.editors.map { it.id }, equalTo(listOf(OperationWorkspaceState.DEFAULT_NOTES_ID)))
        assertThat(closed.dockRegions.single().editorIds, equalTo(listOf(OperationWorkspaceState.DEFAULT_NOTES_ID)))
    }

    @Test
    fun `dock placement and tab reordering retain every open editor`() {
        // Arrange
        val unitId = OperationEditorId("unit:${unit.id.value}")
        val opened = OperationWorkspaceState.default().open(unit.editorState())

        // Act
        val moved = opened.dock(unitId, OperationDockRegion.BOTTOM, 0)
            .dock(unitId, OperationDockRegion.RIGHT, 0)
            .reorderDock(OperationDockRegion.RIGHT, listOf(OperationWorkspaceState.DEFAULT_NOTES_ID, unitId))

        // Assert
        val right = moved.dockRegions.single { it.region == OperationDockRegion.RIGHT }
        assertThat(right.editorIds, equalTo(listOf(OperationWorkspaceState.DEFAULT_NOTES_ID, unitId)))
        assertThat(moved.editors.map { it.id }.toSet(), equalTo(setOf(OperationWorkspaceState.DEFAULT_NOTES_ID, unitId)))
    }
}
