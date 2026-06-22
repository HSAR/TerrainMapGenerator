package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.UITextInput
import korlibs.korge.view.Container
import korlibs.korge.view.ViewRenderer
import korlibs.korge.view.addTo
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D
import java.awt.Color

@OptIn(KorgeExperimental::class)
internal class MapViewerStyledTextInput(
    parent: Container,
    width: Double,
    height: Double,
    x: Double = 0.0,
    y: Double = 0.0,
    initialText: String = "",
) {
    private val frame = parent.container()
    private val content = frame.container()
    private val input: UITextInput = UITextInput(initialText, Size2D(width - 4.0, height - 4.0)).addTo(content)

    init {
        frame.xy(x, y)
        frame.solidRect(width, height, RGBA(255, 255, 255, 255))
        frame.solidRect(width - 4.0, height - 4.0, toRgba(Palette.Colours.DARK)) {
            this.x = 2.0
            this.y = 2.0
        }

        content.xy(2.0, 2.0)
        input.skin = ViewRenderer { }
        input.padding = Margin(4f, 6f, 4f, 6f)
        input.controller.textColor = RGBA(255, 255, 255, 255)
        input.font = MapViewerKorgeFonts.font(MapViewerFontRole.MAIN)
        input.textSize = MapViewerTextViews.DEFAULT_TEXT_SIZE
    }

    var text: String
        get() = input.text
        set(value) {
            input.text = value
        }

    fun focus() {
    }

    fun blur() {
    }

    fun selectAll() {
    }

    fun onClick(block: () -> Unit) {
        frame.mouse {
            onClick { block() }
        }
    }

    private fun toRgba(color: Color, alpha: Int = color.alpha): RGBA =
        RGBA(color.red, color.green, color.blue, alpha)
}
