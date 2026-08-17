package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import io.hsar.mapgenerator.commandgui.MapViewerSaveCoordinatorState
import io.hsar.mapgenerator.commandgui.MapViewerSaveStatus

class ComposeInteractionPresentationUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `version label invokes About`() = runComposeUiTest {
        var clicked = false
        setContent { VersionAboutButton("1.2.3", { clicked = true }) }

        onNodeWithText("v1.2.3").performClick()

        assertThat(clicked, equalTo(true))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `single modal host presents only the active request`() = runComposeUiTest {
        val state = ComposeModalHostState()
        state.show(ComposeModalRequest("first") { dismiss ->
            CommandPanel { CommandButton(dismiss) { androidx.compose.material3.Text("FIRST") } }
        })
        state.show(ComposeModalRequest("second") {
            CommandPanel { androidx.compose.material3.Text("SECOND") }
        })
        setContent { Box { ComposeModalHost(state) } }

        onNodeWithText("FIRST").performClick()

        assertThat(state.active?.key, equalTo("second"))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `Game save presentation dispatches dirty saves but disables the current in flight revision`() = runComposeUiTest {
        var saves = 0
        val dirty = saveState(MapViewerSaveStatus.DIRTY)
        val saving = saveState(MapViewerSaveStatus.SAVING, inFlightRevision = 1L)
        setContent {
            Column {
                SaveCommandButton(dirty, { saves++ })
                SaveCommandButton(saving, { saves++ })
            }
        }

        onNodeWithText("SAVE").performClick()
        onNodeWithText("SAVING…").assertIsNotEnabled()

        assertThat(saves, equalTo(1))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `save failure dialog exposes retry and cancel exit actions`() = runComposeUiTest {
        var retries = 0
        var cancellations = 0
        setContent {
            SaveFailureDialog("disk unavailable", { retries++ }, { cancellations++ })
        }

        onNodeWithText("disk unavailable").assertExists()
        onNodeWithText("RETRY").performClick()
        onNodeWithText("CANCEL EXIT").performClick()

        assertThat(retries, equalTo(1))
        assertThat(cancellations, equalTo(1))
    }

    private fun saveState(status: MapViewerSaveStatus, inFlightRevision: Long? = null) = MapViewerSaveCoordinatorState(
        currentRevision = 1L,
        savedRevision = 0L,
        inFlightRevision = inFlightRevision,
        status = status,
        failure = null,
        failureCount = 0,
        pendingExit = null,
        nextScheduledAtMillis = null,
    )
}
