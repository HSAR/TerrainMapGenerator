package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.math.geom.Vector2D

internal data class MapViewerDialogSize(
    val width: Double,
    val height: Double,
)

internal data class MapViewerDialogBounds(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    val right: Double
        get() = left + width

    val bottom: Double
        get() = top + height

    val size: MapViewerDialogSize
        get() = MapViewerDialogSize(width, height)

    fun movedTo(left: Double, top: Double): MapViewerDialogBounds =
        copy(left = left, top = top)

    fun resizedTo(width: Double, height: Double): MapViewerDialogBounds =
        copy(width = width, height = height)
}

internal data class MapViewerDialogLayout(
    val bounds: MapViewerDialogBounds,
    val titleBar: ScreenBounds,
    val body: ScreenBounds,
    val closeButton: ScreenBounds,
    val contentViewport: ScreenBounds,
    val scrollbar: ScreenBounds,
    val footer: ScreenBounds?,
    val resizeHandle: ScreenBounds,
)

internal object MapViewerDialogLayoutBuilder {
    const val OUTER_PADDING = 16.0
    const val TITLE_BAR_HEIGHT = 38.0
    const val BORDER_INSET = 2.0
    const val CONTENT_PADDING = 18.0
    const val FOOTER_TOP_GAP = 14.0
    const val FOOTER_BOTTOM_PADDING = 16.0
    const val FOOTER_BUTTON_HEIGHT = 40.0
    const val RESIZE_HANDLE_SIZE = 24.0
    const val CLOSE_BUTTON_SIZE = 28.0
    const val MIN_WIDTH = 360.0
    const val MIN_HEIGHT = 300.0

    fun defaultBounds(
        viewportWidth: Int,
        viewportHeight: Int,
        preferredWidth: Double,
        preferredHeight: Double,
    ): MapViewerDialogBounds {
        val size = clampSize(
            size = MapViewerDialogSize(preferredWidth, preferredHeight),
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        return MapViewerDialogBounds(
            left = (viewportWidth - size.width) * 0.5,
            top = (viewportHeight - size.height) * 0.5,
            width = size.width,
            height = size.height,
        ).clampedTo(viewportWidth, viewportHeight)
    }

    fun build(bounds: MapViewerDialogBounds, actionButtonCount: Int = 0): MapViewerDialogLayout {
        val footerHeight = footerHeight(actionButtonCount)
        val contentTop = BORDER_INSET + TITLE_BAR_HEIGHT + FOOTER_TOP_GAP
        val contentLeft = CONTENT_PADDING
        val contentWidth = bounds.width - CONTENT_PADDING * 2.0 - MapViewerScrollbar.WIDTH - SCROLLBAR_GAP
        val contentHeight = bounds.height - contentTop - CONTENT_PADDING - footerHeight
        return MapViewerDialogLayout(
            bounds = bounds,
            titleBar = ScreenBounds(
                left = BORDER_INSET,
                top = BORDER_INSET,
                width = bounds.width - BORDER_INSET * 2.0,
                height = TITLE_BAR_HEIGHT,
            ),
            body = ScreenBounds(
                left = BORDER_INSET,
                top = BORDER_INSET + TITLE_BAR_HEIGHT,
                width = bounds.width - BORDER_INSET * 2.0,
                height = bounds.height - (BORDER_INSET * 2.0) - TITLE_BAR_HEIGHT - footerHeight,
            ),
            closeButton = ScreenBounds(
                left = bounds.width - BORDER_INSET - CONTENT_PADDING - CLOSE_BUTTON_SIZE,
                top = BORDER_INSET + (TITLE_BAR_HEIGHT - CLOSE_BUTTON_SIZE) * 0.5,
                width = CLOSE_BUTTON_SIZE,
                height = CLOSE_BUTTON_SIZE,
            ),
            contentViewport = ScreenBounds(
                left = contentLeft,
                top = contentTop,
                width = contentWidth.coerceAtLeast(0.0),
                height = contentHeight.coerceAtLeast(0.0),
            ),
            scrollbar = ScreenBounds(
                left = contentLeft + contentWidth + SCROLLBAR_GAP,
                top = contentTop,
                width = MapViewerScrollbar.WIDTH,
                height = contentHeight.coerceAtLeast(0.0),
            ),
            footer = if (actionButtonCount > 0) {
                ScreenBounds(
                    left = 0.0,
                    top = bounds.height - BORDER_INSET - FOOTER_BOTTOM_PADDING - FOOTER_BUTTON_HEIGHT,
                    width = bounds.width,
                    height = FOOTER_BUTTON_HEIGHT,
                )
            } else {
                null
            },
            resizeHandle = ScreenBounds(
                left = bounds.width - RESIZE_HANDLE_SIZE,
                top = bounds.height - RESIZE_HANDLE_SIZE,
                width = RESIZE_HANDLE_SIZE,
                height = RESIZE_HANDLE_SIZE,
            ),
        )
    }

    fun clampSize(size: MapViewerDialogSize, viewportWidth: Int, viewportHeight: Int): MapViewerDialogSize =
        MapViewerDialogSize(
            width = size.width.coerceIn(MIN_WIDTH, (viewportWidth - OUTER_PADDING * 2.0).coerceAtLeast(MIN_WIDTH)),
            height = size.height.coerceIn(MIN_HEIGHT, (viewportHeight - OUTER_PADDING * 2.0).coerceAtLeast(MIN_HEIGHT)),
        )

    fun MapViewerDialogBounds.clampedTo(viewportWidth: Int, viewportHeight: Int): MapViewerDialogBounds {
        val size = clampSize(this.size, viewportWidth, viewportHeight)
        return resizedTo(size.width, size.height).movedTo(
            left = left.coerceIn(OUTER_PADDING, (viewportWidth - size.width - OUTER_PADDING).coerceAtLeast(OUTER_PADDING)),
            top = top.coerceIn(OUTER_PADDING, (viewportHeight - size.height - OUTER_PADDING).coerceAtLeast(OUTER_PADDING)),
        )
    }

    fun MapViewerDialogLayout.containsLocal(point: Vector2D): Boolean =
        point.x in 0.0..bounds.width && point.y in 0.0..bounds.height

    fun footerHeight(actionButtonCount: Int): Double =
        if (actionButtonCount <= 0) {
            0.0
        } else {
            FOOTER_TOP_GAP + FOOTER_BOTTOM_PADDING + FOOTER_BUTTON_HEIGHT
        }

    private const val SCROLLBAR_GAP = 10.0
}
