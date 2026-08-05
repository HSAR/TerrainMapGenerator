package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.SELECTION_FRAME_SCREEN_PADDING
import korlibs.math.geom.Vector2D

internal data class MapViewerDialogSize(
    val width: Double,
    val height: Double,
)

internal data class MapViewerDialogLayout(
    val bounds: MapViewerDialogBounds,
    val showWindowControls: Boolean,
    val titleBar: ScreenBounds,
    val body: ScreenBounds,
    val minimizeButton: ScreenBounds,
    val pinButton: ScreenBounds,
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
    const val CLOSE_BUTTON_SIZE = TITLE_BAR_HEIGHT + BORDER_INSET * 2.0
    const val MIN_HEIGHT = 300.0

    fun defaultBounds(
        viewportWidth: Int,
        viewportHeight: Int,
        preferredWidth: Double,
        preferredHeight: Double,
        minimumWidth: Double = 0.0,
        actionButtonCount: Int = 0,
        contentHeight: ((Double) -> Double)? = null,
    ): MapViewerDialogBounds {
        val preferredSize = MapViewerDialogSize(preferredWidth, preferredHeight)
        val preferredWidthClamped = clampSize(
            size = preferredSize,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            minimumWidth = minimumWidth,
        ).width
        val contentAwareHeight = contentHeight
            ?.let { heightProvider ->
                contentAwareShellHeight(
                    width = preferredWidthClamped,
                    viewportHeight = viewportHeight,
                    actionButtonCount = actionButtonCount,
                    preferredHeight = preferredHeight,
                    contentHeight = heightProvider(contentViewportWidth(preferredWidthClamped)),
                )
            }
            ?: preferredHeight
        val size = clampSize(
            size = MapViewerDialogSize(preferredWidth, contentAwareHeight),
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

    fun build(
        bounds: MapViewerDialogBounds,
        actionButtonCount: Int = 0,
        showWindowControls: Boolean = false,
    ): MapViewerDialogLayout {
        val footerLayout = MapViewerDialogActions.footerLayout(bounds.width, actionButtonCount)
        val footerHeight = MapViewerDialogActions.footerHeight(actionButtonCount, footerLayout)
        val contentTop = BORDER_INSET + TITLE_BAR_HEIGHT + FOOTER_TOP_GAP
        val contentLeft = CONTENT_PADDING
        val contentWidth = contentViewportWidth(bounds.width)
        val contentHeight = bounds.height - contentTop - CONTENT_PADDING - footerHeight
        val closeButtonLeft = (bounds.width - BORDER_INSET - CLOSE_BUTTON_SIZE).coerceAtLeast(BORDER_INSET)
        return MapViewerDialogLayout(
            bounds = bounds,
            showWindowControls = showWindowControls,
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
            minimizeButton = ScreenBounds(
                left = closeButtonLeft - CLOSE_BUTTON_SIZE * 2.0,
                top = 0.0,
                width = CLOSE_BUTTON_SIZE,
                height = CLOSE_BUTTON_SIZE,
            ),
            pinButton = ScreenBounds(
                left = closeButtonLeft - CLOSE_BUTTON_SIZE,
                top = 0.0,
                width = CLOSE_BUTTON_SIZE,
                height = CLOSE_BUTTON_SIZE,
            ),
            closeButton = ScreenBounds(
                left = closeButtonLeft,
                top = 0.0,
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
                left = bounds.width - RESIZE_HANDLE_SIZE + SELECTION_FRAME_SCREEN_PADDING,
                top = bounds.height - RESIZE_HANDLE_SIZE + SELECTION_FRAME_SCREEN_PADDING,
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

    fun contentAwareShellHeight(
        width: Double,
        viewportHeight: Int,
        actionButtonCount: Int,
        preferredHeight: Double,
        contentHeight: Double,
    ): Double {
        val footerLayout = MapViewerDialogActions.footerLayout(width, actionButtonCount)
        val footerHeight = MapViewerDialogActions.footerHeight(actionButtonCount, footerLayout)
        val requiredHeight = BORDER_INSET + TITLE_BAR_HEIGHT + FOOTER_TOP_GAP + contentHeight + CONTENT_PADDING + footerHeight
        val maxInitialHeight = (viewportHeight * MAX_INITIAL_HEIGHT_RATIO)
            .coerceAtMost((viewportHeight - OUTER_PADDING * 2.0).coerceAtLeast(0.0))
        return maxOf(preferredHeight, requiredHeight)
            .coerceAtMost(maxInitialHeight)
    }

    fun maximumInitialContentHeight(
        shellWidth: Double,
        viewportHeight: Int,
        actionButtonCount: Int = 0,
    ): Double =
        build(
            bounds = MapViewerDialogBounds(
                left = 0.0,
                top = 0.0,
                width = shellWidth,
                height = maximumInitialShellHeight(viewportHeight),
            ),
            actionButtonCount = actionButtonCount,
        ).contentViewport.height

    fun contentViewportWidth(shellWidth: Double): Double =
        (shellWidth - CONTENT_PADDING * 2.0 - MapViewerScrollbar.WIDTH - SCROLLBAR_GAP).coerceAtLeast(0.0)

    /** Standard starting point for a window associated with the left sidebar controls. */
    fun alignLeft(bounds: MapViewerDialogBounds): Vector2D =
        Vector2D(OUTER_PADDING, sidebarTop())

    /** Standard starting point for a window associated with the right sidebar controls. */
    fun alignRight(bounds: MapViewerDialogBounds, viewportWidth: Int): Vector2D =
        Vector2D(
            x = viewportWidth - OUTER_PADDING - bounds.width,
            y = sidebarTop(),
        )

    private fun maximumInitialShellHeight(viewportHeight: Int): Double =
        (viewportHeight * MAX_INITIAL_HEIGHT_RATIO)
            .coerceAtMost((viewportHeight - OUTER_PADDING * 2.0).coerceAtLeast(0.0))

    private fun sidebarTop(): Double =
        OUTER_PADDING + MapViewerSidebarLayoutBuilder.TOGGLE_HEIGHT + MapViewerSidebarLayoutBuilder.PANEL_TOP_GAP

    fun MapViewerDialogBounds.clampedTo(
        viewportWidth: Int,
        viewportHeight: Int,
        minimumWidth: Double = 0.0,
    ): MapViewerDialogBounds {
        val size = clampSize(MapViewerDialogSize(width, height), viewportWidth, viewportHeight, minimumWidth)
        return resizedTo(size.width, size.height).movedTo(
            left = left.coerceIn(OUTER_PADDING, (viewportWidth - size.width - OUTER_PADDING).coerceAtLeast(OUTER_PADDING)),
            top = top.coerceIn(OUTER_PADDING, (viewportHeight - size.height - OUTER_PADDING).coerceAtLeast(OUTER_PADDING)),
        )
    }

    fun MapViewerDialogBounds.resizedWithinViewport(
        viewportWidth: Int,
        viewportHeight: Int,
        minimumWidth: Double = 0.0,
    ): MapViewerDialogBounds {
        val maxWidth = (viewportWidth - left - OUTER_PADDING).coerceAtLeast(0.0)
        val maxHeight = (viewportHeight - top - OUTER_PADDING).coerceAtLeast(0.0)
        val width = width.coerceIn(
            minimumWidth.coerceAtLeast(0.0).coerceAtMost(maxWidth),
            maxWidth,
        )
        val height = height.coerceIn(
            MIN_HEIGHT.coerceAtMost(maxHeight),
            maxHeight,
        )
        return resizedTo(width, height)
    }

    fun MapViewerDialogLayout.containsLocal(point: Vector2D): Boolean =
        point.x in 0.0..bounds.width && point.y in 0.0..bounds.height

    private const val SCROLLBAR_GAP = 10.0
    private const val MAX_INITIAL_HEIGHT_RATIO = 0.75
}
