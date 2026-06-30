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
import korlibs.korge.view.clipContainer
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
        frameStyle: (Boolean) -> UiFrameStyle = MapViewerUiBoxes::defaultStyle,
    ): MapViewerInputField =
        parent.container().let { field ->
            field.xy(x, y)
            MapViewerUiBoxes.box(field, width, height, style = frameStyle)

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
            val visibleTextClip = field.clipContainer(Size2D(width - 12.0, height - 4.0)) {
                zIndex = 10.0
                mouseEnabled = false
            }.also {
                it.xy(6.0, 2.0)
            }
            val visibleText: Text = MapViewerTextViews.leftBox(
                parent = visibleTextClip,
                text = text,
                width = width - 12.0,
                height = height - 4.0,
                role = role,
                color = Palette.Colours.WHITE,
            ).also {
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
    var isFocused: Boolean = false
        private set

    var text: String
        get() = input.text
        set(value) {
            input.text = value
            visibleText.text = value
        }

    fun focus() {
        isFocused = true
        input.focus()
    }

    fun blur() {
        isFocused = false
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
