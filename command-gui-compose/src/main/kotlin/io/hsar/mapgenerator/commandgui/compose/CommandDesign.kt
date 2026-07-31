package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CommandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = OutlinedButton(
    onClick = onClick,
    modifier = Modifier.defaultMinSize(
        minWidth = COMMAND_BUTTON_MIN_WIDTH,
        minHeight = COMMAND_BUTTON_MIN_HEIGHT,
    ).then(modifier),
    enabled = enabled,
    shape = RectangleShape,
    border = BorderStroke(2.dp, if (enabled) CommandColours.White else CommandColours.Light),
    colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (active) CommandColours.Dark else CommandColours.Main,
        contentColor = CommandColours.White,
        disabledContainerColor = CommandColours.Main,
        disabledContentColor = CommandColours.Light,
    ),
    contentPadding = PaddingValues(horizontal = COMMAND_BUTTON_HORIZONTAL_PADDING, vertical = 0.dp),
) {
    ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontSize = COMMAND_BUTTON_FONT_SIZE, lineHeight = COMMAND_BUTTON_FONT_SIZE)) {
        content()
    }
}

internal val COMMAND_BUTTON_FONT_SIZE = 22.sp
internal val COMMAND_BUTTON_MIN_WIDTH = 120.dp
internal val COMMAND_BUTTON_MIN_HEIGHT = 40.dp
internal val COMMAND_BUTTON_HORIZONTAL_PADDING = 12.dp

@Composable
internal fun CommandPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Surface(
    modifier = modifier,
    shape = RectangleShape,
    color = CommandColours.Dark.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, CommandColours.Light),
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
internal fun CommandLaunchScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize()
        .background(CommandColours.Dark),
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            onBack?.let { back ->
                CommandButton(onClick = back) {
                    Text("BACK")
                }
            }
            Column {
                Text(
                    "OPERATIONAL COMMAND TOOL",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    title.uppercase(),
                    color = CommandColours.Backing,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        HorizontalDivider(color = CommandColours.Light)
        Box(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

internal fun Modifier.commandPanelWidth() = widthIn(max = 980.dp)
