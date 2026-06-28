package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
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
        val trackHeight = (bounds.height - INNER_INSET * 2.0).coerceAtLeast(0.0)
        val thumbHeight = if (disabled) {
            trackHeight
        } else {
            max(trackHeight * (state.viewportHeight / state.contentHeight), MIN_THUMB_HEIGHT)
        }
        val thumbTop = if (disabled) {
            bounds.top + INNER_INSET
        } else {
            bounds.top + INNER_INSET + ((trackHeight - thumbHeight).coerceAtLeast(0.0) * (state.offset / state.maxOffset))
        }
        return ScreenBounds(
            left = bounds.left + INNER_INSET,
            top = thumbTop,
            width = (bounds.width - INNER_INSET * 2.0).coerceAtLeast(0.0),
            height = thumbHeight,
        )
    }

    fun render(
        parent: Container,
        bounds: ScreenBounds,
        state: MapViewerScrollState,
        onThumbDown: ((Double) -> Unit)? = null,
    ) {
        val disabled = !state.needsScrolling
        val track = parent.container()
        track.xy(bounds.left, bounds.top)
        MapViewerUiBoxes.box(
            track,
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
        )

        val thumb = thumbBounds(bounds, state)
        val thumbView = parent.container()
        thumbView.xy(thumb.left, thumb.top)
        MapViewerUiBoxes.box(
            thumbView,
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
        )
        if (!disabled && onThumbDown != null) {
            track.mouse {
                onDown {
                    val thumbLocalLeft = thumb.left - bounds.left
                    val thumbLocalTop = thumb.top - bounds.top
                    if (currentPosLocal.x in thumbLocalLeft..(thumbLocalLeft + thumb.width) &&
                        currentPosLocal.y in thumbLocalTop..(thumbLocalTop + thumb.height)
                    ) {
                        onThumbDown(currentPosGlobal.y)
                    }
                }
            }
        }
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
        val travel = ((bounds.height - INNER_INSET * 2.0) - thumb.height).coerceAtLeast(1.0)
        val delta = currentPointerY - dragStartPointerY
        return (dragStartOffset + delta * (state.maxOffset / travel)).coerceIn(0.0, state.maxOffset)
    }

    private const val DISABLED_TRACK_ALPHA = 70
    private const val ENABLED_TRACK_ALPHA = 140
    private const val DISABLED_THUMB_ALPHA = 80
    private const val ENABLED_THUMB_ALPHA = 220
}
