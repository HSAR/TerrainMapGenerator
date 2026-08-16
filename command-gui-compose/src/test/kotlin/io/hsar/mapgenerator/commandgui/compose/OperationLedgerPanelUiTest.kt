package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.MapViewerLedgerEntryKind
import io.hsar.mapgenerator.commandgui.MapViewerRenderData
import io.hsar.mapgenerator.commandgui.MapViewerSettings
import io.hsar.mapgenerator.commandgui.operation.OperationChangeSet
import io.hsar.mapgenerator.commandgui.operation.OperationDocument
import io.hsar.mapgenerator.commandgui.operation.OperationTimeline
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntries
import io.hsar.mapgenerator.commandgui.operation.OperationTimelineEntry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.UUID

class OperationLedgerPanelUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `narrow ledger keeps compact turn buttons clickable`() = runComposeUiTest {
        val selected = mutableListOf<Int>()
        setContent {
            MaterialTheme {
                Box(Modifier.width(340.dp).height(600.dp)) {
                    OperationLedgerPanel(
                        state = ComposeOperationState(document()),
                        onUndo = {},
                        onRedo = {},
                        onTurnSelected = selected::add,
                        onCurrent = {},
                        onAddTurn = {},
                    )
                }
            }
        }

        onNodeWithText("Operation Start").performClick()
        onNodeWithText("Turn 1").performClick()

        assertThat(selected, equalTo(listOf(0, 2)))
    }

    @Test
    fun `turn indices retain the legacy four digit hexadecimal presentation`() {
        assertThat(operationTurnIndexLabel(0x1e0), equalTo("01E0"))
    }

    private fun document(): OperationDocument {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val timeline = OperationTimeline(
            entries = listOf(
                OperationTimelineEntries.turn("Operation Start"),
                OperationTimelineEntry(OperationChangeSet(), MapViewerLedgerEntryKind.OBJECT, "created something verbose"),
                OperationTimelineEntries.turn("Turn 1"),
            ),
            appliedCount = 3,
        )
        return OperationDocument(
            operationId = UUID.fromString("00000000-0000-0000-0000-000000000099"),
            operationName = "Test",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            settings = MapViewerSettings(operationName = "Test"),
            renderData = MapViewerRenderData(image),
            timeline = timeline,
        )
    }
}
