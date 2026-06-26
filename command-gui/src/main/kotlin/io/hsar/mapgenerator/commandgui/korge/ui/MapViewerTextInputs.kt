package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.ui.UITextInput
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.ViewRenderer
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.geom.Margin
import korlibs.math.geom.Size2D

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
    ): MapViewerInputField =
        parent.container().let { field ->
            field.xy(x, y)
            MapViewerUiBoxes.box(field, width, height)

            val inputHost = field.container().also {
                it.xy(2.0, 2.0)
                it.zIndex = 1.0
            }
            val input = inputHost.uiTextInput(text, Size2D(width - 4.0, height - 4.0)).apply {
                skin = ViewRenderer { }
                padding = Margin(4f, 6f, 4f, 6f)
                controller.textColor = RGBA(255, 255, 255, 0)
                font = MapViewerKorgeFonts.font(role)
                textSize = MapViewerTextViews.DEFAULT_TEXT_SIZE
                mouse {
                    onClick {
                        focus()
                    }
                }
            }
            val visibleText: Text = MapViewerTextViews.leftBox(
                parent = field,
                text = text,
                width = width - 4.0,
                height = height - 4.0,
                role = role,
                color = Palette.Colours.WHITE,
            ).also {
                it.x = 6.0
                it.y = 2.0
                it.zIndex = 10.0
                it.mouseEnabled = false
            }

            input.controller.onTextUpdated.add {
                visibleText.text = input.text
            }

            MapViewerInputField(field, input, visibleText)
        }
}

@OptIn(KorgeExperimental::class)
internal class MapViewerInputField(
    private val root: Container,
    private val input: UITextInput,
    private val visibleText: Text,
) {
    val controller get() = input.controller

    var text: String
        get() = input.text
        set(value) {
            input.text = value
            visibleText.text = value
        }

    fun focus() {
        input.focus()
    }

    fun blur() {
        input.blur()
    }

    fun selectAll() {
        input.selectAll()
    }

    fun onClick(block: () -> Unit) {
        root.mouse {
            onClick { block() }
        }
    }
}
