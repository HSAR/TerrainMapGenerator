package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import java.awt.Color

internal object MapViewerLaunchUi {
    fun panel(parent: Container, bounds: ScreenBounds, alpha: Int = 235): Container =
        parent.container().also { panel ->
            panel.xy(bounds.left, bounds.top)
            panel.solidRect(bounds.width, bounds.height, RGBA(255, 255, 255, 255))
            panel.solidRect(bounds.width - 4.0, bounds.height - 4.0, toRgba(Palette.Colours.DARK, alpha)) {
                x = 2.0
                y = 2.0
            }
        }

    fun button(
        parent: Container,
        width: Double,
        height: Double,
        label: String,
        disabled: Boolean = false,
        onClick: () -> Unit = {},
    ): Container =
        parent.container().also { button ->
            val fill = if (disabled) toRgba(Palette.Colours.DARK, 140) else toRgba(Palette.Colours.DARK, 245)
            val textColor = if (disabled) Palette.Colours.LIGHT else Palette.Colours.WHITE
            button.solidRect(width, height, RGBA(255, 255, 255, if (disabled) 120 else 255))
            button.solidRect(width - 4.0, height - 4.0, fill) {
                x = 2.0
                y = 2.0
            }
            MapViewerTextViews.box(
                parent = button,
                text = label,
                width = width,
                height = height,
                color = textColor,
            ) {
                alpha = if (disabled) 0.55 else 1.0
            }
            if (!disabled) {
                button.mouse {
                    onClick { onClick() }
                }
            }
        }

    fun backButton(parent: Container, onClick: () -> Unit = {}): Container =
        button(parent, 120.0, 42.0, "Back", onClick = onClick)

    fun label(parent: Container, text: String, strong: Boolean = false, color: Color = Palette.Colours.WHITE): Text =
        label(parent, text, strong, color, scale = 1.0)

    fun screenTitle(parent: Container, text: String, width: Double, height: Double, color: Color = Palette.Colours.WHITE): Text =
        MapViewerTextViews.box(
            parent = parent,
            text = text,
            width = width,
            height = height,
            role = MapViewerFontRole.STRONG,
            size = SCREEN_TITLE_TEXT_SIZE,
            color = color,
        )

    fun label(
        parent: Container,
        text: String,
        strong: Boolean = false,
        color: Color = Palette.Colours.WHITE,
        scale: Double,
    ): Text =
        MapViewerTextViews.label(
            parent = parent,
            text = text,
            role = if (strong) MapViewerFontRole.STRONG else MapViewerFontRole.MAIN,
            size = MapViewerTextViews.DEFAULT_TEXT_SIZE * scale,
            color = color,
        )

    fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)

    private const val SCREEN_TITLE_TEXT_SIZE = 34.0
}
