package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextBitmapRenderer
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
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
            button.image(MapViewerTextBitmapRenderer.render(label, Palette.Fonts.MAIN.first, textColor)) {
                x = (width - this.width) * 0.5
                y = (height - this.height) * 0.5
                alpha = if (disabled) 0.55 else 1.0
            }
            if (!disabled) {
                button.mouse {
                    onClick { onClick() }
                }
            }
        }

    fun label(parent: Container, text: String, strong: Boolean = false, color: Color = Palette.Colours.WHITE): Container =
        label(parent, text, strong, color, scale = 1.0)

    fun label(
        parent: Container,
        text: String,
        strong: Boolean = false,
        color: Color = Palette.Colours.WHITE,
        scale: Double,
    ): Container =
        parent.container().also { label ->
            label.image(
                MapViewerTextBitmapRenderer.render(
                    text = text,
                    font = if (strong) Palette.Fonts.STRONG.first else Palette.Fonts.MAIN.first,
                    color = color,
                ),
            )
            label.scale = scale
        }

    fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)
}
