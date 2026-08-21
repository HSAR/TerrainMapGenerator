package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.hsar.mapgenerator.commandgui.MapViewerLaunchBackgroundSpec
import io.hsar.mapgenerator.commandgui.MapViewerSavedOperation
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.junit.jupiter.api.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo

class ComposeLaunchpadParityUiTest {
    @Test
    fun `launch background world extent does not double count display density`() {
        val worldViewportWidth = launchBackgroundWorldPixels(logicalDp = 800f)

        val sourceWidth = MapViewerLaunchBackgroundSpec.forViewport(worldViewportWidth, 600, seed = 73L).textureWidth

        assertThat(worldViewportWidth, equalTo(800))
        assertThat(sourceWidth, equalTo(1200))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `Nord theme defaults ordinary content to white`() = runComposeUiTest {
        var inherited: Color? = null
        setContent { NordTheme { inherited = LocalContentColor.current } }

        waitForIdle()

        assertThat(inherited, equalTo(CommandColours.White))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `background biome box expands in place without changing its title`() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { ComposeLaunchBackgroundState(73L, scope) }
            NordTheme {
                ComposeLaunchBackgroundPanel(
                    state = state,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    onRegenerate = {},
                    onClose = { expanded = false },
                )
            }
        }

        onNodeWithText(MapViewerLaunchBackgroundSpec.biome(73L)).performClick()

        assertThat(expanded, equalTo(true))
        onNodeWithText("REGENERATE").performClick()
        assertThat(onAllNodesWithText("BACKGROUND:", substring = true).fetchSemanticsNodes().isEmpty(), equalTo(true))
        onNodeWithText(MapViewerLaunchBackgroundSpec.biome(73L)).performClick()
        assertThat(expanded, equalTo(false))
        onNodeWithText(MapViewerLaunchBackgroundSpec.biome(73L)).performClick()
        onNodeWithText("CLOSE").performClick()
        assertThat(expanded, equalTo(false))
    }

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
    fun `ongoing operations uses legacy shell controls and save row chrome`() = runComposeUiTest {
        var loadedPath: Path? = null
        val operation = MapViewerSavedOperation(
            path = Path.of("operation-echo.zip"),
            operationId = UUID.fromString("12345678-1234-1234-1234-123456789abc"),
            operationName = "Operation Echo",
            updatedAt = Instant.parse("2026-08-17T12:30:00Z"),
            createdAt = Instant.parse("2026-08-16T12:30:00Z"),
            thumbnailAssetId = null,
            thumbnail = null,
        )
        setContent {
            NordTheme {
                Box(Modifier.size(1600.dp, 900.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    ComposeOngoingOperationsPanel(
                        panelHeight = 702.0,
                        operations = listOf(operation),
                        loading = false,
                        loadingPath = null,
                        errorMessage = null,
                        onBack = {},
                        onBrowse = {},
                        onLoad = { loadedPath = it },
                    )
                }
            }
        }

        onNodeWithTag("ongoing-operations-panel")
            .assertWidthIsEqualTo(760.dp)
            .assertHeightIsEqualTo(702.dp)
        onNodeWithTag("ongoing-operations-back")
            .assertWidthIsEqualTo(120.dp)
            .assertHeightIsEqualTo(42.dp)
        onNodeWithTag("ongoing-operations-browse")
            .assertWidthIsEqualTo(280.dp)
            .assertHeightIsEqualTo(44.dp)
        onNodeWithTag("ongoing-operation-row-0")
            .assertWidthIsEqualTo(658.dp)
            .assertHeightIsEqualTo(96.dp)
        onNodeWithText("ONGOING OPERATIONS").fetchSemanticsNode()
        onNodeWithText("BROWSE FOR SAVE FILE").fetchSemanticsNode()
        onNodeWithText("Operation Echo").fetchSemanticsNode()
        onNodeWithText(
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                .format(operation.updatedAt.atZone(ZoneId.systemDefault())),
        ).fetchSemanticsNode()
        listOf("SAVED OPERATIONS", "REFRESH", "operation-echo.zip", "Open").forEach { absentText ->
            assertThat(onAllNodesWithText(absentText).fetchSemanticsNodes().isEmpty(), equalTo(true))
        }
        onNodeWithTag("ongoing-operation-row-0").performClick()
        assertThat(loadedPath, equalTo(operation.path))
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `ongoing operations retains the legacy loading and empty copy`() = runComposeUiTest {
        var loading by mutableStateOf(true)
        setContent {
            NordTheme {
                ComposeOngoingOperationsPanel(
                    panelHeight = 520.0,
                    operations = emptyList(),
                    loading = loading,
                    loadingPath = null,
                    errorMessage = null,
                    onBack = {},
                    onBrowse = {},
                    onLoad = {},
                )
            }
        }

        onNodeWithText("Loading saved operations...").fetchSemanticsNode()
        loading = false
        waitForIdle()
        onNodeWithText("No valid saves found in the default save directory.").fetchSemanticsNode()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `ongoing operation load exposes cancellation`() = runComposeUiTest {
        var cancelled = false
        setContent {
            NordTheme {
                ComposeOngoingOperationsPanel(
                    panelHeight = 520.0,
                    operations = emptyList(),
                    loading = false,
                    loadingPath = "operation.zip",
                    errorMessage = null,
                    onBack = {},
                    onBrowse = {},
                    onCancelLoad = { cancelled = true },
                    onLoad = {},
                )
            }
        }

        onNodeWithText("CANCEL LOADING").performClick()

        assertThat(cancelled, equalTo(true))
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
