package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

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
}
