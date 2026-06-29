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
    val footer: MapViewerDialogFooter?,
    val resizeHandle: ScreenBounds,
)

internal enum class MapViewerDialogFooterLayout {
    ROW,
    COLUMN,
}

internal data class MapViewerDialogFooter(
    val bounds: ScreenBounds,
    val layout: MapViewerDialogFooterLayout,
)

internal fun ScreenBounds.offsetBy(dx: Double, dy: Double): ScreenBounds =
    ScreenBounds(
        left = left + dx,
        top = top + dy,
        width = width,
        height = height,
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
    const val MIN_HEIGHT = 300.0

    fun defaultBounds(
        viewportWidth: Int,
        viewportHeight: Int,
        preferredWidth: Double,
        preferredHeight: Double,
        minimumWidth: Double = 0.0,
    ): MapViewerDialogBounds {
        val size = clampSize(
            size = MapViewerDialogSize(preferredWidth, preferredHeight),
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            minimumWidth = minimumWidth,
        )
        return MapViewerDialogBounds(
            left = (viewportWidth - size.width) * 0.5,
            top = (viewportHeight - size.height) * 0.5,
            width = size.width,
            height = size.height,
        ).clampedTo(viewportWidth, viewportHeight)
    }

    fun minimumShellWidth(contentMinimumWidth: Double, actionButtonCount: Int = 0): Double =
        maxOf(
            contentMinimumWidth,
            MapViewerDialogActions.minimumWidth(actionButtonCount),
        ) + (CONTENT_PADDING * 2.0) + MapViewerScrollbar.WIDTH + SCROLLBAR_GAP

    fun build(bounds: MapViewerDialogBounds, actionButtonCount: Int = 0): MapViewerDialogLayout {
        val footerLayout = MapViewerDialogActions.footerLayout(bounds.width, actionButtonCount)
        val footerHeight = MapViewerDialogActions.footerHeight(actionButtonCount, footerLayout)
        val contentTop = BORDER_INSET + TITLE_BAR_HEIGHT + FOOTER_TOP_GAP
        val contentLeft = CONTENT_PADDING
        val contentWidth = bounds.width - CONTENT_PADDING * 2.0 - MapViewerScrollbar.WIDTH - SCROLLBAR_GAP
        val contentHeight = bounds.height - contentTop - CONTENT_PADDING - footerHeight
        return MapViewerDialogLayout(
            bounds = bounds,
            titleBar = ScreenBounds(
                left = BORDER_INSET,
                top = BORDER_INSET,
                width = (bounds.width - BORDER_INSET * 2.0).coerceAtLeast(0.0),
                height = TITLE_BAR_HEIGHT,
            ),
            body = ScreenBounds(
                left = BORDER_INSET,
                top = BORDER_INSET + TITLE_BAR_HEIGHT,
                width = (bounds.width - BORDER_INSET * 2.0).coerceAtLeast(0.0),
                height = (bounds.height - (BORDER_INSET * 2.0) - TITLE_BAR_HEIGHT - footerHeight).coerceAtLeast(0.0),
            ),
            closeButton = ScreenBounds(
                left = (bounds.width - BORDER_INSET - CONTENT_PADDING - CLOSE_BUTTON_SIZE).coerceAtLeast(BORDER_INSET),
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
                MapViewerDialogFooter(
                    bounds = ScreenBounds(
                        left = 0.0,
                        top = bounds.height - BORDER_INSET - FOOTER_BOTTOM_PADDING - footerHeight,
                        width = bounds.width,
                        height = footerHeight,
                    ),
                    layout = footerLayout,
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

    fun clampSize(
        size: MapViewerDialogSize,
        viewportWidth: Int,
        viewportHeight: Int,
        minimumWidth: Double = 0.0,
    ): MapViewerDialogSize =
        MapViewerDialogSize(
            width = size.width.coerceIn(
                minimumWidth.coerceAtLeast(0.0).coerceAtMost((viewportWidth - OUTER_PADDING * 2.0).coerceAtLeast(0.0)),
                (viewportWidth - OUTER_PADDING * 2.0).coerceAtLeast(0.0),
            ),
            height = size.height.coerceIn(
                MIN_HEIGHT.coerceAtMost((viewportHeight - OUTER_PADDING * 2.0).coerceAtLeast(0.0)),
                (viewportHeight - OUTER_PADDING * 2.0).coerceAtLeast(0.0),
            ),
        )

    fun MapViewerDialogBounds.clampedTo(
        viewportWidth: Int,
        viewportHeight: Int,
        minimumWidth: Double = 0.0,
    ): MapViewerDialogBounds {
        val size = clampSize(this.size, viewportWidth, viewportHeight, minimumWidth)
        return resizedTo(size.width, size.height).movedTo(
            left = left.coerceIn(OUTER_PADDING, (viewportWidth - size.width - OUTER_PADDING).coerceAtLeast(OUTER_PADDING)),
            top = top.coerceIn(OUTER_PADDING, (viewportHeight - size.height - OUTER_PADDING).coerceAtLeast(OUTER_PADDING)),
        )
    }

    fun MapViewerDialogLayout.containsLocal(point: Vector2D): Boolean =
        point.x in 0.0..bounds.width && point.y in 0.0..bounds.height

    private const val SCROLLBAR_GAP = 10.0
}
