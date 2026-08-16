package io.hsar.mapgenerator.commandgui.save

import io.hsar.mapgenerator.commandgui.operation.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class OperationOperationalWorkspacePersistenceTest {
    @Test
    fun `operational editor selections round trip through additive workspace state`() {
        val objectId = OperationObjectId(UUID(0, 42))
        val editor = OperationEditorState(
            OperationEditorId("tactical:$objectId"), OperationEditorKind.TACTICAL, OperationEditorTarget.Object(objectId),
            OperationEditorDraft.Operational("tactical", "report", OperationObjectId(UUID(0, 43)), setOf("beam-1")),
            OperationFloatingEditorState(OperationEditorBounds(1.0, 2.0, 500.0, 600.0)),
        )
        val additional = OperationAdditionalData.fromJson("""{"unknown":{"keep":true}}""".toByteArray())
        additional.updateOperationUiState(OperationUiState(workspace = OperationWorkspaceState.default().open(editor)))

        val restored = OperationAdditionalData.fromJson(additional.toJsonOrNull()).operationUiState().workspace.editor(editor.id)

        assertThat(restored?.draft, equalTo(editor.draft))
        val canonicalJson = additional.toJsonOrNull()!!.toString(Charsets.UTF_8).filterNot(Char::isWhitespace)
        assertThat(canonicalJson.contains("\"keep\":true"), equalTo(true))
    }
}
