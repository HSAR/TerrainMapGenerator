package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.Graphics
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Rectangle
import korlibs.math.geom.vector.LineJoin
import korlibs.math.geom.vector.VectorPath
import java.awt.Color
import java.util.*

internal object MapViewerUiBoxes {
    private val activeStateByContainer = WeakHashMap<Container, Boolean>()

    fun frame(
        parent: Container,
        width: Double,
        height: Double,
        fillColor: Color = Palette.Colours.DARK,
        fillAlpha: Int = MapViewerUiStateStyle.ACTIVE_ALPHA,
        borderColor: Color = Palette.Colours.WHITE,
        borderAlpha: Int = MapViewerUiStateStyle.ACTIVE_ALPHA,
        inset: Double = BORDER_INSET,
    ): Container =
        frame(
            parent = parent,
            width = width,
            height = height,
            active = true,
            style = {
                UiFrameStyle(
                    fillColor = fillColor,
                    fillAlpha = fillAlpha,
                    borderColor = borderColor,
                    borderAlpha = borderAlpha,
                )
            },
            inset = inset,
        )

    fun box(
        parent: Container,
        width: Double,
        height: Double,
        active: Boolean = true,
        style: (Boolean) -> UiFrameStyle = { state -> defaultStyle(state) },
        inset: Double = BORDER_INSET,
    ): Container =
        frame(parent = parent, width = width, height = height, active = active, style = style, inset = inset)

    fun frame(
        parent: Container,
        width: Double,
        height: Double,
        active: Boolean = true,
        style: (Boolean) -> UiFrameStyle = { state -> defaultStyle(state) },
        inset: Double = BORDER_INSET,
    ): Container =
        parent.container().also { box ->
            box.isUiActive = active
            val frameStyle = style(active)
            frameGeometry(width, height, inset).also { geometry ->
                box.solidRect(geometry.fill.width, geometry.fill.height, toRgba(frameStyle.fillColor, frameStyle.fillAlpha)) {
                    x = geometry.fill.x
                    y = geometry.fill.y
                }
                geometry.borders.forEach { border ->
                    box.solidRect(border.width, border.height, toRgba(frameStyle.borderColor, frameStyle.borderAlpha)) {
                        x = border.x
                        y = border.y
                    }
                }
            }
        }

    fun shapePanel(
        parent: Container,
        shape: VectorPath,
        fillColor: Color = Palette.Colours.DARK,
        fillAlpha: Int = MapViewerUiStateStyle.ACTIVE_ALPHA,
        borderColor: Color = Palette.Colours.WHITE,
        borderAlpha: Int = MapViewerUiStateStyle.ACTIVE_ALPHA,
        borderWidth: Double = BORDER_INSET,
    ): Graphics =
        parent.graphics {
            fill(toRgba(fillColor, fillAlpha)) {
                path(shape)
            }
            lineJoin = LineJoin.MITER
            lineWidth = borderWidth
            stroke(toRgba(borderColor, borderAlpha)) {
                path(shape)
            }
        }

    fun frameGeometry(width: Double, height: Double, inset: Double = BORDER_INSET): UiFrameGeometry {
        val border = inset.coerceAtLeast(0.0)
        val fillWidth = (width - border * 2).coerceAtLeast(0.0)
        val fillHeight = (height - border * 2).coerceAtLeast(0.0)
        return UiFrameGeometry(
            fill = UiFrameRect(border, border, fillWidth, fillHeight),
            borders = listOf(
                UiFrameRect(0.0, 0.0, width, border),
                UiFrameRect(0.0, (height - border).coerceAtLeast(0.0), width, border),
                UiFrameRect(0.0, border, border, fillHeight),
                UiFrameRect((width - border).coerceAtLeast(0.0), border, border, fillHeight),
            ),
        )
    }

    fun frame(
        parent: Container,
        bounds: Rectangle,
        fillColor: Color = Palette.Colours.DARK,
        fillAlpha: Int = MapViewerUiStateStyle.ACTIVE_ALPHA,
        borderColor: Color = Palette.Colours.WHITE,
        borderAlpha: Int = MapViewerUiStateStyle.ACTIVE_ALPHA,
        inset: Double = BORDER_INSET,
    ): Container =
        frame(parent, bounds.width, bounds.height, fillColor, fillAlpha, borderColor, borderAlpha, inset).also {
            it.xy(bounds.left, bounds.top)
        }

    fun box(
        parent: Container,
        bounds: Rectangle,
        active: Boolean = true,
        style: (Boolean) -> UiFrameStyle = { state -> defaultStyle(state) },
        inset: Double = BORDER_INSET,
    ): Container =
        frame(parent, bounds.width, bounds.height, active, style, inset).also {
            it.xy(bounds.left, bounds.top)
        }

    fun button(
        parent: Container,
        width: Double,
        height: Double,
        active: Boolean = true,
        style: (Boolean) -> UiFrameStyle = { state -> defaultButtonStyle(state) },
    ): Container =
        frame(parent = parent, width = width, height = height, active = active, style = style)

    fun imageBox(
        parent: Container,
        bitmap: Bitmap32,
        maxWidth: Double,
        maxHeight: Double,
        active: Boolean = true,
        style: (Boolean) -> UiFrameStyle = { state -> defaultStyle(state) },
        inset: Double = BORDER_INSET,
    ): Container =
        frame(
            parent = parent,
            width = maxWidth,
            height = maxHeight,
            active = active,
            style = style,
            inset = inset,
        ).also { box ->
            val fitted = imageFit(bitmap, maxWidth, maxHeight, inset)
            MapViewerImageViews.icon(box, bitmap) {
                scale = fitted.scale
                x = fitted.x
                y = fitted.y
            }
        }

    fun colorAlpha(color: Color, alpha: Int): Color =
        Color(color.red, color.green, color.blue, alpha)

    fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    var Container.isUiActive: Boolean
        get() = activeStateByContainer[this] ?: true
        set(value) {
            activeStateByContainer[this] = value
        }

    fun defaultStyle(active: Boolean): UiFrameStyle =
        UiFrameStyle(
            fillColor = if (active) Palette.Colours.DARK else Palette.Colours.MAIN,
            fillAlpha = if (active) ACTIVE_ALPHA else MapViewerUiStateStyle.inactiveFillAlpha,
            borderColor = Palette.Colours.WHITE,
            borderAlpha = ACTIVE_ALPHA,
        )

    fun defaultButtonStyle(active: Boolean): UiFrameStyle =
        UiFrameStyle(
            fillColor = if (active) Palette.Colours.DARK else Palette.Colours.MAIN,
            fillAlpha = if (active) ACTIVE_ALPHA else MapViewerUiStateStyle.inactiveFillAlpha,
            borderColor = Palette.Colours.WHITE,
            borderAlpha = ACTIVE_ALPHA,
        )

    private fun imageFit(bitmap: Bitmap32, maxWidth: Double, maxHeight: Double, inset: Double): MapViewerIconFit.Result =
        MapViewerIconFit.inside(
            bitmap = bitmap,
            width = (maxWidth - inset * 2.0).coerceAtLeast(0.0),
            height = (maxHeight - inset * 2.0).coerceAtLeast(0.0),
            maxScale = 1.0,
        )

    private const val BORDER_INSET = 2.0
    private const val ACTIVE_ALPHA = MapViewerUiStateStyle.ACTIVE_ALPHA
}

internal data class UiFrameGeometry(
    val fill: UiFrameRect,
    val borders: List<UiFrameRect>,
)

internal data class UiFrameRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

internal data class UiFrameStyle(
    val fillColor: Color,
    val fillAlpha: Int,
    val borderColor: Color,
    val borderAlpha: Int,
)
