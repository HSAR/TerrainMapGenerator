package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import io.hsar.mapgenerator.commandgui.MapViewerTransientNoticeTokens
import kotlin.math.roundToInt

@Composable
internal fun ComposeModalHost(state: ComposeModalHostState) {
    val request = state.active ?: return
    Dialog(onDismissRequest = { state.dismiss(cancelled = true) }) {
        request.content { state.dismiss() }
    }
}

@Composable
internal fun VersionAboutButton(version: String, onClick: () -> Unit, modifier: Modifier = Modifier) = CommandButton(
    onClick = onClick,
    modifier = modifier.semantics { contentDescription = "About Operational Command Tool, version $version" },
) { Text("v$version") }

@Composable
internal fun DefaultCommandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    CommandButton(onClick, modifier.focusRequester(requester), enabled = enabled, content = content)
}

@Composable
internal fun ComposeTransientMessageHost(
    state: ComposeTransientMessageHostState,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.messages) {
        while (state.messages.isNotEmpty()) {
            delay(50)
            now = System.currentTimeMillis()
            state.advance(now)
        }
    }
    Column(
        modifier = modifier.padding(CommandDesignTokens.noticeInset),
        verticalArrangement = Arrangement.spacedBy(CommandDesignTokens.noticeGap),
        horizontalAlignment = Alignment.Start,
    ) {
        state.messages.forEach { message ->
            val alpha = state.alpha(message, now)
            Surface(
                modifier = Modifier.testTag("transient-notice-${message.id}").alpha(alpha).offset {
                    IntOffset(0, (-MapViewerTransientNoticeTokens.FADE_LIFT * (1f - alpha)).roundToInt())
                },
                color = Color.Black.copy(alpha = MapViewerTransientNoticeTokens.BACKGROUND_ALPHA / 255f),
                shape = RectangleShape,
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(
                        horizontal = CommandDesignTokens.noticeHorizontalPadding,
                        vertical = CommandDesignTokens.noticeVerticalPadding,
                    ),
                    color = CommandColours.White,
                    fontSize = CommandDesignTokens.noticeFontSize,
                    lineHeight = CommandDesignTokens.noticeFontSize,
                )
            }
        }
    }
}

@Composable
internal fun AboutModalContent(
    buildInfo: ComposeBuildInfo,
    resources: ComposePresentationResources,
    fontStatus: String,
    dismiss: () -> Unit,
) {
    val diagnostics = buildInfo.diagnostics(fontStatus)
    CommandPanel(Modifier.width(560.dp).heightIn(max = 680.dp)) {
        Text("ABOUT", style = MaterialTheme.typography.headlineMedium)
        Text("OPERATIONAL COMMAND TOOL", style = MaterialTheme.typography.titleLarge)
        Text("Version ${buildInfo.version}", color = CommandColours.Backing)
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("RUNTIME", fontWeight = FontWeight.Bold)
            Text(diagnostics)
            Text("PROJECT LICENCE", fontWeight = FontWeight.Bold)
            Text(resources.projectLicense, style = MaterialTheme.typography.bodySmall)
            Text("THIRD-PARTY NOTICES", fontWeight = FontWeight.Bold)
            Text(resources.thirdPartyNotices, style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommandButton(
                onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(diagnostics), null) },
                modifier = Modifier.weight(1f),
            ) { Text("COPY DIAGNOSTICS") }
            DefaultCommandButton(onClick = dismiss, modifier = Modifier.weight(1f)) { Text("CLOSE") }
        }
    }
}
