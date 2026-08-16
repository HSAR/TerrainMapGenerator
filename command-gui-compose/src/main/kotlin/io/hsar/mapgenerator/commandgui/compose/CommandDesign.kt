package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.hsar.mapgenerator.commandgui.MapViewerLaunchpadTokens
import io.hsar.mapgenerator.commandgui.MapViewerTransientNoticeTokens

@Composable
internal fun CommandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = true,
    destructive: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val backing = when {
        !enabled -> CommandColours.Main
        destructive && (hovered || focused || pressed) -> CommandColours.Red
        pressed -> CommandColours.Light
        active -> CommandColours.Dark
        else -> CommandColours.Main
    }
    val border = when {
        !enabled -> CommandColours.Light
        destructive -> CommandColours.Red
        focused -> COMMAND_BUTTON_FOCUS_COLOUR
        hovered -> CommandColours.Backing
        else -> CommandColours.White
    }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(
            minWidth = CommandDesignTokens.buttonMinimumWidth,
            minHeight = CommandDesignTokens.controlHeight,
        ).then(modifier),
        enabled = enabled,
        interactionSource = interactions,
        shape = RectangleShape,
        border = BorderStroke(
            if (focused) CommandDesignTokens.focusBorderWidth else CommandDesignTokens.borderWidth,
            border,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backing,
            contentColor = CommandColours.White,
            disabledContainerColor = CommandColours.Main,
            disabledContentColor = CommandColours.Light,
        ),
        contentPadding = PaddingValues(horizontal = CommandDesignTokens.buttonHorizontalPadding, vertical = 0.dp),
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontSize = CommandDesignTokens.buttonFontSize, lineHeight = CommandDesignTokens.buttonFontSize)) {
            content()
        }
    }
}

internal val COMMAND_BUTTON_FONT_SIZE = 22.sp
internal val COMMAND_BUTTON_MIN_WIDTH = 120.dp
internal val COMMAND_BUTTON_MIN_HEIGHT = 40.dp
internal val COMMAND_BUTTON_HORIZONTAL_PADDING = 12.dp
internal val COMMAND_BUTTON_FOCUS_COLOUR = CommandColours.White

internal object CommandDesignTokens {
    val buttonFontSize = COMMAND_BUTTON_FONT_SIZE
    val buttonMinimumWidth = COMMAND_BUTTON_MIN_WIDTH
    val controlHeight = COMMAND_BUTTON_MIN_HEIGHT
    val buttonHorizontalPadding = COMMAND_BUTTON_HORIZONTAL_PADDING
    val panelPadding = 24.dp
    val panelGap = 16.dp
    val compactGap = 8.dp
    val titleBarHeight = 42.dp
    val borderWidth = 2.dp
    val focusBorderWidth = 3.dp
    val noticeInset = MapViewerTransientNoticeTokens.VIEWPORT_INSET.dp
    val noticeGap = MapViewerTransientNoticeTokens.STACK_GAP.dp
    val noticeHorizontalPadding = MapViewerTransientNoticeTokens.HORIZONTAL_PADDING.dp
    val noticeVerticalPadding = MapViewerTransientNoticeTokens.VERTICAL_PADDING.dp
    val noticeFontSize = MapViewerTransientNoticeTokens.FONT_SIZE.sp
    val menuWidth = 220.dp
    val menuRowInset = 8.dp
}

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
        modifier = Modifier.padding(CommandDesignTokens.panelPadding),
        verticalArrangement = Arrangement.spacedBy(CommandDesignTokens.panelGap),
        content = content,
    )
}

@Composable
internal fun CommandFloatingPanel(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    titleBarModifier: Modifier = Modifier,
    titleActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) = Surface(
    modifier = modifier,
    shape = RectangleShape,
    color = CommandColours.Dark.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, CommandColours.Light),
) {
    Column {
        Row(
            modifier = titleBarModifier
                .fillMaxWidth()
                .height(CommandDesignTokens.titleBarHeight)
                .background(CommandColours.Dark.copy(alpha = 0.92f))
                .border(1.dp, CommandColours.White),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                color = CommandColours.White,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = CommandDesignTokens.buttonFontSize),
            )
            titleActions()
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, CommandColours.White)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = CommandColours.White, fontSize = 28.sp, lineHeight = 28.sp)
            }
        }
        Column(
                modifier = Modifier.weight(1f).padding(CommandDesignTokens.panelPadding),
                verticalArrangement = Arrangement.spacedBy(CommandDesignTokens.panelGap),
            content = content,
        )
    }
}

@Composable
internal fun CommandLaunchScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    backgroundEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize().semantics { contentDescription = title },
) {
    ComposeLaunchBackground(Modifier.fillMaxSize(), enabled = backgroundEnabled)
    Surface(
        modifier = Modifier.align(Alignment.TopCenter)
            .testTag("command-launch-header")
            .offset(y = MapViewerLaunchpadTokens.HEADER_TOP.dp)
            .width(MapViewerLaunchpadTokens.HEADER_WIDTH.dp)
            .height(MapViewerLaunchpadTokens.HEADER_HEIGHT.dp),
        shape = RectangleShape,
        color = CommandColours.Dark.copy(alpha = MapViewerLaunchpadTokens.PANEL_ALPHA / 255f),
        border = BorderStroke(CommandDesignTokens.borderWidth, CommandColours.White),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "OPERATIONAL COMMAND TOOL",
                color = CommandColours.White,
                fontSize = MapViewerLaunchpadTokens.HEADER_FONT_SIZE.sp,
                lineHeight = MapViewerLaunchpadTokens.HEADER_FONT_SIZE.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }
    }
    onBack?.let { back ->
        CommandButton(
            onClick = back,
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp).widthIn(min = 120.dp).height(42.dp),
        ) { Text("BACK") }
    }
    val contentModifier = if (onBack == null) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize().padding(start = 28.dp, top = 116.dp, end = 28.dp, bottom = 28.dp)
    }
    Box(
        modifier = contentModifier,
        contentAlignment = Alignment.Center,
        content = content,
    )
}

internal fun Modifier.commandPanelWidth() = widthIn(max = 980.dp)
