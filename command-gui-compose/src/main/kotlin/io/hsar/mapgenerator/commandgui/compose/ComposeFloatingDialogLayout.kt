package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapSelectionCorner
import io.hsar.mapgenerator.commandgui.MapSelectionCornerBracket
import io.hsar.mapgenerator.commandgui.MapViewerDialogChromeTokens
import io.hsar.mapgenerator.commandgui.ViewerPoint

internal data class ComposeFloatingDialogResizeHandleLayout(
    val left: Float,
    val top: Float,
    val size: Float,
)

internal fun composeFloatingDialogResizeHandleLayout(width: Float, height: Float) =
    ComposeFloatingDialogResizeHandleLayout(
        left = width - MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE.toFloat() + MapViewerDialogChromeTokens.RESIZE_HANDLE_OUTSET.toFloat(),
        top = height - MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE.toFloat() + MapViewerDialogChromeTokens.RESIZE_HANDLE_OUTSET.toFloat(),
        size = MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE.toFloat(),
    )

internal fun composeFloatingDialogResizeBracket() = MapSelectionCornerBracket(
    corner = MapSelectionCorner.BOTTOM_RIGHT,
    points = listOf(
        ViewerPoint(MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE, MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE),
        ViewerPoint(MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE, MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH),
        ViewerPoint(MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS, MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH),
        ViewerPoint(MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS, MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS),
        ViewerPoint(MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH, MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_THICKNESS),
        ViewerPoint(MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE - MapViewerDialogChromeTokens.RESIZE_BRACKET_LENGTH, MapViewerDialogChromeTokens.RESIZE_HANDLE_SIZE),
    ),
)

/** Resolves a clamped dimension from the full drag distance, retaining pointer movement beyond the clamp. */
internal fun clampedDialogDimensionAtPointer(
    initial: Float,
    dragDistance: Float,
    minimum: Float,
) = (initial + dragDistance).coerceAtLeast(minimum)
