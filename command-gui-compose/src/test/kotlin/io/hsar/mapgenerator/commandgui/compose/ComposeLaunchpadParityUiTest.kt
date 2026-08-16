package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Test

class ComposeLaunchpadParityUiTest {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `launchpad uses legacy header panel and button dimensions`() = runComposeUiTest {
        setContent {
            NordTheme {
                Box(Modifier.size(800.dp, 600.dp)) {
                    ComposeMainLaunchpad({}, {}, {}, backgroundEnabled = false)
                }
            }
        }

        onNodeWithTag("command-launch-header")
            .assertWidthIsEqualTo(540.dp)
            .assertHeightIsEqualTo(78.dp)
        onNodeWithTag("launchpad-panel")
            .assertWidthIsEqualTo(420.dp)
            .assertHeightIsEqualTo(320.dp)
        (0..2).forEach { index ->
            onNodeWithTag("launchpad-button-$index")
                .assertWidthIsEqualTo(300.dp)
                .assertHeightIsEqualTo(44.dp)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `temporary notice uses legacy font metrics and padding`() = runComposeUiTest {
        val state = ComposeTransientMessageHostState().apply { show("Operation saved") }
        setContent {
            NordTheme {
                Box(Modifier.size(800.dp, 600.dp)) {
                    ComposeTransientMessageHost(state)
                }
            }
        }

        onNodeWithTag("transient-notice-0").assertHeightIsEqualTo(43.dp)
    }
}
