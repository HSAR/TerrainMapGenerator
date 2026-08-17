package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.operation.OperationChangeSet
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationNote
import io.hsar.mapgenerator.commandgui.operation.OperationNoteId
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import io.hsar.mapgenerator.commandgui.operation.OperationValueChange
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.UUID

class NotesPanelUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `KorGE note row opens the existing note as a floating dialog`() = runComposeUiTest {
        val note = OperationNote(OperationNoteId(UUID(0, 72)), "Orders", "Hold position")
        val entry = OperationTimelineEntry(
            OperationChangeSet(notes = listOf(OperationValueChange(after = note))),
            MapViewerLedgerEntryKind.NOTE,
            "created note",
        )
        var state = ComposeOperationState(OperationDocument(
            operationId = UUID(0, 71),
            operationName = "Test",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            settings = MapViewerSettings(operationName = "Test"),
            renderData = MapViewerRenderData(BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)),
            timeline = OperationTimeline().record(entry),
        ))
        setContent {
            NordTheme {
                NotesPanel(state, { state = it }, Modifier)
            }
        }

        onNodeWithTag("notes-create").assertWidthIsEqualTo(140.dp).assertHeightIsEqualTo(40.dp)
        onNodeWithTag("notes-row-${note.id.value}").assertHeightIsEqualTo(62.dp).performClick()

        val editor = state.openEditors.single { it.kind == OperationEditorKind.NOTE }
        assertThat((editor.draft as OperationEditorDraft.Note).value.title, equalTo("Orders"))
        assertThat(state.workspace.dockContaining(editor.id), equalTo(null))
    }
}
