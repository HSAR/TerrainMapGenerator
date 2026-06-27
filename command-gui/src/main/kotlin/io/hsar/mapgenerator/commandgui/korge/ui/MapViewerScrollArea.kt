package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.view.Container
import korlibs.korge.view.xy
import kotlin.math.max

internal data class MapViewerScrollState(
    val viewportHeight: Double,
    val contentHeight: Double,
    val offset: Double = 0.0,
) {
    val maxOffset: Double
        get() = (contentHeight - viewportHeight).coerceAtLeast(0.0)

    val needsScrolling: Boolean
        get() = maxOffset > 0.0

    fun scrolledBy(delta: Double): MapViewerScrollState =
        copy(offset = (offset + delta).coerceIn(0.0, maxOffset))

    fun withOffset(offset: Double): MapViewerScrollState =
        copy(offset = offset.coerceIn(0.0, maxOffset))

    fun withContent(viewportHeight: Double = this.viewportHeight, contentHeight: Double = this.contentHeight): MapViewerScrollState =
        copy(
            viewportHeight = viewportHeight,
            contentHeight = contentHeight,
            offset = offset.coerceIn(0.0, (contentHeight - viewportHeight).coerceAtLeast(0.0)),
        )
}

internal object MapViewerScrollbar {
    const val WIDTH = 18.0
    const val MIN_THUMB_HEIGHT = 28.0
    const val INNER_INSET = 2.0
    const val SCROLL_STEP_PX = 36.0

    fun thumbBounds(bounds: ScreenBounds, state: MapViewerScrollState): ScreenBounds {
        val disabled = !state.needsScrolling
        val thumbHeight = if (disabled) {
            bounds.height
        } else {
            max(bounds.height * (state.viewportHeight / state.contentHeight), MIN_THUMB_HEIGHT)
        }
        val thumbTop = if (disabled) {
            bounds.top
        } else {
            bounds.top + ((bounds.height - thumbHeight) * (state.offset / state.maxOffset))
        }
        return ScreenBounds(
            left = bounds.left + INNER_INSET,
            top = thumbTop + INNER_INSET,
            width = bounds.width - INNER_INSET * 2.0,
            height = thumbHeight - INNER_INSET * 2.0,
        )
    }

    fun render(parent: Container, bounds: ScreenBounds, state: MapViewerScrollState) {
        val disabled = !state.needsScrolling
        MapViewerUiBoxes.box(
            parent,
            bounds.width,
            bounds.height,
            style = {
                UiFrameStyle(
                    fillColor = Palette.Colours.DARK,
                    fillAlpha = if (disabled) DISABLED_TRACK_ALPHA else ENABLED_TRACK_ALPHA,
                    borderColor = Palette.Colours.WHITE,
                    borderAlpha = MapViewerUiStateStyle.ACTIVE_ALPHA,
                )
            },
        ).xy(bounds.left, bounds.top)

        val thumb = thumbBounds(bounds, state)
        MapViewerUiBoxes.box(
            parent,
            thumb.width,
            thumb.height,
            style = {
                UiFrameStyle(
                    fillColor = Palette.Colours.WHITE,
                    fillAlpha = if (disabled) DISABLED_THUMB_ALPHA else ENABLED_THUMB_ALPHA,
                    borderColor = Palette.Colours.WHITE,
                    borderAlpha = MapViewerUiStateStyle.ACTIVE_ALPHA,
                )
            },
        ).xy(thumb.left, thumb.top)
    }

    fun dragOffset(
        bounds: ScreenBounds,
        state: MapViewerScrollState,
        dragStartPointerY: Double,
        dragStartOffset: Double,
        currentPointerY: Double,
    ): Double {
        if (!state.needsScrolling) {
            return 0.0
        }

        val thumb = thumbBounds(bounds, state)
        val travel = (bounds.height - thumb.height).coerceAtLeast(1.0)
        val delta = currentPointerY - dragStartPointerY
        return (dragStartOffset + delta * (state.maxOffset / travel)).coerceIn(0.0, state.maxOffset)
    }

    private const val DISABLED_TRACK_ALPHA = 70
    private const val ENABLED_TRACK_ALPHA = 140
    private const val DISABLED_THUMB_ALPHA = 80
    private const val ENABLED_THUMB_ALPHA = 220
}
