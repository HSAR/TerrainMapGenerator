package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.hsar.mapgenerator.commandgui.MapViewerLaunchpadTokens
import io.hsar.mapgenerator.commandgui.MapViewerDialogChromeTokens
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
    val titleBarHeight = MapViewerDialogChromeTokens.CONTROL_SIZE.dp
    val borderWidth = 2.dp
    val focusBorderWidth = 3.dp
    val noticeInset = MapViewerTransientNoticeTokens.VIEWPORT_INSET.dp
    val noticeGap = MapViewerTransientNoticeTokens.STACK_GAP.dp
    val noticeHorizontalPadding = MapViewerTransientNoticeTokens.HORIZONTAL_PADDING.dp
    val noticeVerticalPadding = MapViewerTransientNoticeTokens.VERTICAL_PADDING.dp
    val noticeFontSize = MapViewerTransientNoticeTokens.FONT_SIZE.sp
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
    minimized: Boolean = false,
    pinned: Boolean = false,
    onMinimize: () -> Unit = {},
    onPin: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) = Surface(
    modifier = modifier,
    shape = RectangleShape,
    color = CommandColours.Dark.copy(alpha = MapViewerDialogChromeTokens.PANEL_ALPHA / 255f),
    border = BorderStroke(MapViewerDialogChromeTokens.BORDER_WIDTH.dp, CommandColours.White),
) {
    Column {
        CommandDialogTitleBar(title, minimized, pinned, onMinimize, onPin, onClose, titleBarModifier)
        Column(
            modifier = Modifier.weight(1f).padding(MapViewerDialogChromeTokens.CONTENT_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(CommandDesignTokens.panelGap),
            content = content,
        )
    }
}

@Composable
internal fun CommandDialogTitleBar(
    title: String,
    minimized: Boolean,
    pinned: Boolean,
    onMinimize: () -> Unit,
    onPin: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = Modifier
        .testTag("command-dialog-title-bar")
        .fillMaxWidth()
        .height(MapViewerDialogChromeTokens.CONTROL_SIZE.dp),
) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            color = CommandColours.Dark.copy(alpha = MapViewerDialogChromeTokens.TITLE_BAR_ALPHA / 255f),
            topLeft = androidx.compose.ui.geometry.Offset(
                MapViewerDialogChromeTokens.BORDER_WIDTH.dp.toPx(),
                MapViewerDialogChromeTokens.BORDER_WIDTH.dp.toPx(),
            ),
            size = androidx.compose.ui.geometry.Size(
                (size.width - MapViewerDialogChromeTokens.BORDER_WIDTH.dp.toPx() * 2f).coerceAtLeast(0f),
                MapViewerDialogChromeTokens.TITLE_BAR_HEIGHT.dp.toPx(),
            ),
        )
        drawRect(
            color = CommandColours.White,
            topLeft = androidx.compose.ui.geometry.Offset(
                MapViewerDialogChromeTokens.BORDER_WIDTH.dp.toPx(),
                (MapViewerDialogChromeTokens.BORDER_WIDTH + MapViewerDialogChromeTokens.TITLE_BAR_HEIGHT).dp.toPx(),
            ),
            size = androidx.compose.ui.geometry.Size(
                (size.width - MapViewerDialogChromeTokens.BORDER_WIDTH.dp.toPx() * 2f).coerceAtLeast(0f),
                MapViewerDialogChromeTokens.BORDER_WIDTH.dp.toPx(),
            ),
        )
    }
    Row(
        Modifier.fillMaxSize().padding(end = MapViewerDialogChromeTokens.BORDER_WIDTH.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(modifier),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = title.uppercase(),
                modifier = Modifier
                    .testTag("command-dialog-title")
                    .fillMaxWidth()
                    .padding(
                        start = (MapViewerDialogChromeTokens.BORDER_WIDTH + MapViewerDialogChromeTokens.TITLE_TEXT_LEFT).dp,
                        end = MapViewerDialogChromeTokens.TITLE_TEXT_RIGHT_GAP.dp,
                    ),
                color = CommandColours.White,
                fontSize = MapViewerLaunchpadTokens.BODY_FONT_SIZE.sp,
                lineHeight = MapViewerLaunchpadTokens.BODY_FONT_SIZE.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CommandDialogControl(
            type = CommandDialogControlType.MINIMIZE,
            active = false,
            directionDown = minimized,
            contentDescription = if (minimized) "Restore dialog" else "Minimize dialog",
            onClick = onMinimize,
        )
        CommandDialogControl(
            type = CommandDialogControlType.PIN,
            active = pinned,
            contentDescription = if (pinned) "Unpin dialog" else "Pin dialog",
            onClick = onPin,
        )
        CommandDialogControl(
            type = CommandDialogControlType.CLOSE,
            active = false,
            contentDescription = "Close dialog",
            onClick = onClose,
        )
    }
}

private enum class CommandDialogControlType { MINIMIZE, PIN, CLOSE }

internal data class CommandDialogFrameEdges(val near: Float, val far: Float)

internal fun commandDialogFrameEdges(size: Float, strokeWidth: Float): CommandDialogFrameEdges =
    CommandDialogFrameEdges(strokeWidth / 2f, size - strokeWidth / 2f)

@Composable
private fun CommandDialogControl(
    type: CommandDialogControlType,
    active: Boolean,
    directionDown: Boolean = false,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    Canvas(
        Modifier
            .testTag("command-dialog-${type.name.lowercase()}")
            .size(MapViewerDialogChromeTokens.CONTROL_SIZE.dp)
            .semantics { this.contentDescription = contentDescription }
            .hoverable(interactions)
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .focusable(interactionSource = interactions),
    ) {
        val stroke = MapViewerDialogChromeTokens.ICON_STROKE_WIDTH.dp.toPx()
        val frameEdges = commandDialogFrameEdges(size.height, stroke)
        drawRect(
            when {
                pressed -> CommandColours.Light
                hovered || focused -> CommandColours.Main
                else -> CommandColours.Dark
            },
        )
        if (active) {
            drawRect(
                CommandColours.White.copy(alpha = MapViewerDialogChromeTokens.ACTIVE_CONTROL_FILL_ALPHA / 255f),
                topLeft = androidx.compose.ui.geometry.Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(size.width - stroke * 2f, size.height - stroke * 2f),
            )
        }
        drawLine(
            CommandColours.White,
            androidx.compose.ui.geometry.Offset(0f, frameEdges.near),
            androidx.compose.ui.geometry.Offset(size.width, frameEdges.near),
            stroke,
        )
        drawLine(
            CommandColours.White,
            androidx.compose.ui.geometry.Offset(0f, frameEdges.far),
            androidx.compose.ui.geometry.Offset(size.width, frameEdges.far),
            stroke,
        )
        drawLine(
            CommandColours.White,
            androidx.compose.ui.geometry.Offset(frameEdges.near, 0f),
            androidx.compose.ui.geometry.Offset(frameEdges.near, size.height),
            stroke,
        )
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val arm = size.minDimension * 0.22f
        when (type) {
            CommandDialogControlType.MINIMIZE -> {
                val tipY = center.y + if (directionDown) arm * 0.55f else -arm * 0.55f
                val edgeY = center.y + if (directionDown) -arm * 0.55f else arm * 0.55f
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x - arm, edgeY), androidx.compose.ui.geometry.Offset(center.x, tipY), stroke)
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x, tipY), androidx.compose.ui.geometry.Offset(center.x + arm, edgeY), stroke)
            }
            CommandDialogControlType.PIN -> {
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x - arm, center.y - arm * .8f), androidx.compose.ui.geometry.Offset(center.x + arm, center.y - arm * .8f), stroke)
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x - arm * .55f, center.y - arm * .8f), androidx.compose.ui.geometry.Offset(center.x - arm * .55f, center.y + arm * .25f), stroke)
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x - arm * .55f, center.y + arm * .25f), androidx.compose.ui.geometry.Offset(center.x + arm * .55f, center.y + arm * .25f), stroke)
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x + arm * .55f, center.y + arm * .25f), androidx.compose.ui.geometry.Offset(center.x + arm * .55f, center.y - arm * .8f), stroke)
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x, center.y + arm * .25f), androidx.compose.ui.geometry.Offset(center.x, center.y + arm), stroke)
            }
            CommandDialogControlType.CLOSE -> {
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x - arm, center.y - arm), androidx.compose.ui.geometry.Offset(center.x + arm, center.y + arm), stroke)
                drawLine(CommandColours.White, androidx.compose.ui.geometry.Offset(center.x + arm, center.y - arm), androidx.compose.ui.geometry.Offset(center.x - arm, center.y + arm), stroke)
            }
        }
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
