package io.hsar.mapgenerator.commandgui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class ComposeUniversalListItem(
    val title: String,
    val subtitle: String? = null,
    val preview: ImageBitmap? = null,
    val previewContentDescription: String? = null,
    val previewPlaceholder: String? = null,
    val trailingLabel: String? = null,
    val selected: Boolean = false,
    val borderColour: Color = CommandColours.White,
)

internal data class ComposeUniversalListAction(
    val label: String,
    val enabled: Boolean = true,
    val active: Boolean = false,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun ComposeUniversalListRow(
    item: ComposeUniversalListItem,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    previewWidth: Dp = height - 8.dp,
    onClick: (() -> Unit)? = null,
    trailingAction: ComposeUniversalListAction? = null,
    contextActions: List<ComposeUniversalListAction> = emptyList(),
) {
    var contextMenuOpen by remember { mutableStateOf(false) }
    val interactionModifier = Modifier
        .onPointerEvent(PointerEventType.Press) { event ->
            if (event.buttons.isSecondaryPressed && contextActions.isNotEmpty()) {
                event.changes.forEach { it.consume() }
                contextMenuOpen = true
            }
        }
        .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(interactionModifier),
        color = if (item.selected) CommandColours.Dark else CommandColours.Main,
        border = BorderStroke(2.dp, item.borderColour),
        shape = RectangleShape,
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxSize().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    item.preview != null -> Image(
                        bitmap = item.preview,
                        contentDescription = item.previewContentDescription,
                        modifier = Modifier.width(previewWidth).fillMaxHeight().background(CommandColours.Dark),
                        contentScale = ContentScale.Fit,
                    )
                    item.previewPlaceholder != null -> Box(
                        Modifier.width(previewWidth).fillMaxHeight().background(CommandColours.Dark),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(item.previewPlaceholder, fontSize = 12.sp, maxLines = 1)
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    if (item.subtitle == null) {
                        Text(item.title, fontSize = 24.sp, lineHeight = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(
                            item.title,
                            Modifier.align(Alignment.TopStart),
                            fontSize = 24.sp,
                            lineHeight = 24.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            item.subtitle,
                            Modifier.align(Alignment.BottomStart),
                            color = CommandColours.Light,
                            fontSize = 18.sp,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                item.trailingLabel?.let { label ->
                    Text(label, color = CommandColours.Light, fontSize = 18.sp, maxLines = 1)
                }
                trailingAction?.let { action ->
                    CommandButton(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        active = action.active,
                        destructive = action.destructive,
                        modifier = Modifier.width(76.dp).height((height - 12.dp).coerceAtMost(36.dp)),
                    ) { Text(action.label, fontSize = 16.sp, maxLines = 1) }
                }
            }
            DropdownMenu(
                expanded = contextMenuOpen,
                onDismissRequest = { contextMenuOpen = false },
                modifier = Modifier.background(CommandColours.Dark).border(2.dp, CommandColours.White),
            ) {
                contextActions.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                action.label,
                                color = if (action.destructive) CommandColours.Red else CommandColours.White,
                                fontSize = 20.sp,
                                maxLines = 1,
                            )
                        },
                        enabled = action.enabled,
                        modifier = Modifier.height(44.dp).background(CommandColours.Dark),
                        onClick = {
                            contextMenuOpen = false
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
}
