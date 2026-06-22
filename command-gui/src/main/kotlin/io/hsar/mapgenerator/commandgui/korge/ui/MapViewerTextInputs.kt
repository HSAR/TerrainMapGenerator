package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.UITextInput
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.ViewRenderer
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D
import java.awt.Color

@OptIn(KorgeExperimental::class)
internal object MapViewerTextInputs {
    fun input(
        parent: Container,
        text: String,
        width: Double,
        height: Double,
        x: Double = 0.0,
        y: Double = 0.0,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
    ): UITextInput =
        parent.container().let { frame ->
            frame.xy(x, y)
            frame.solidRect(width, height, RGBA(255, 255, 255, 255))
            frame.solidRect(width - 4.0, height - 4.0, toRgba(Palette.Colours.DARK)) {
                this.x = 2.0
                this.y = 2.0
            }

            val content = frame.container().also {
                it.xy(2.0, 2.0)
            }

            content.uiTextInput(text, Size2D(width - 4.0, height - 4.0)).apply {
                skin = ViewRenderer { }
                padding = Margin(4f, 6f, 4f, 6f)
                controller.textColor = RGBA(255, 255, 255, 255)
                font = MapViewerKorgeFonts.font(role)
                textSize = MapViewerTextViews.DEFAULT_TEXT_SIZE
                mouse {
                    onClick {
                        focus()
                    }
                }
            }
        }

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)
}
