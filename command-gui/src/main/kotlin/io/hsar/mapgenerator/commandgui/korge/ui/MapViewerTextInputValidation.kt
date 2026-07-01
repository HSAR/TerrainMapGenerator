package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.internal.KorgeInternal
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import kotlin.math.max

internal object MapViewerTextInputValidation {
    fun isPresent(text: String): Boolean =
        text.trim().isNotBlank()

    fun isReservedKey(text: String, reservedKeys: Set<String>): Boolean =
        text.trim() in reservedKeys

    fun frameStyle(valid: Boolean): (Boolean) -> UiFrameStyle =
        MapViewerUiBoxes::defaultStyle

    fun validationFailedIndicator(parent: Container, size: Double = VALIDATION_ICON_SIZE): Container =
        parent.container().also { icon ->
            MapViewerUiBoxes.frame(
                parent = icon,
                width = size,
                height = size,
                fillColor = Palette.Colours.DARK,
                fillAlpha = 245,
                borderColor = Palette.Colours.RED,
                borderAlpha = 235,
            )
            MapViewerTextViews.box(
                parent = icon,
                text = "!",
                width = size,
                height = size,
                role = MapViewerFontRole.STRONG,
                color = Palette.Colours.RED,
            )
        }

    @OptIn(KorgeInternal::class)
    fun validationExplanationTooltip(
        parent: Container,
        anchor: Vector2D,
        text: String,
        anchorHeight: Double = MapViewerDialogSpacing.INPUT_HEIGHT,
    ): Container {
        val width = max(TOOLTIP_MIN_WIDTH, MapViewerTextViews.measure(text).width + TOOLTIP_PADDING_X * 2.0)
        val height = TOOLTIP_HEIGHT
        val parentGlobal = parent.globalPos
        val localAnchor = Vector2D(anchor.x - parentGlobal.x, anchor.y - parentGlobal.y)
        val stage = parent.stage
        val viewportWidth = stage?.views?.actualVirtualWidth?.toDouble() ?: Double.POSITIVE_INFINITY
        val viewportHeight = stage?.views?.actualVirtualHeight?.toDouble() ?: Double.POSITIVE_INFINITY
        val viewportRight = viewportWidth - parentGlobal.x
        val viewportBottom = viewportHeight - parentGlobal.y
        val left = (localAnchor.x - width).coerceIn(0.0, (viewportRight - width).coerceAtLeast(0.0))
        val aboveTop = localAnchor.y - height - TOOLTIP_GAP
        val belowTop = localAnchor.y + anchorHeight + TOOLTIP_GAP
        val preferredTop = if (aboveTop >= 0.0) aboveTop else belowTop
        val top = preferredTop.coerceIn(0.0, (viewportBottom - height).coerceAtLeast(0.0))

        return parent.container().also { layer ->
            layer.xy(left, top)
            layer.zIndex = TOOLTIP_Z_INDEX
            layer.solidRect(width, height, RGBA(0, 0, 0, 220))
            MapViewerUiBoxes.frame(
                parent = layer,
                width = width,
                height = height,
                fillColor = Palette.Colours.DARK,
                fillAlpha = 245,
                borderColor = Palette.Colours.RED,
                borderAlpha = 220,
            )
            MapViewerTextViews.leftBox(
                parent = layer,
                text = text,
                width = width - TOOLTIP_PADDING_X * 2.0,
                height = height,
                color = Palette.Colours.WHITE,
            ) {
                x = TOOLTIP_PADDING_X
            }
        }
    }

    const val VALIDATION_ICON_SIZE = 20.0
    const val TOOLTIP_GAP = 8.0
    const val TOOLTIP_HEIGHT = 34.0
    const val TOOLTIP_MIN_WIDTH = 260.0
    const val TOOLTIP_PADDING_X = 10.0
    const val TOOLTIP_Z_INDEX = 90_000.0
}
