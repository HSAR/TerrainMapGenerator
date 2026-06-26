package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.view.Container

internal class MapViewerStyledTextInput(
    parent: Container,
    width: Double,
    height: Double,
    x: Double = 0.0,
    y: Double = 0.0,
    initialText: String = "",
) {
    private val inputField: MapViewerInputField = MapViewerTextInputs.input(
        parent = parent,
        text = initialText,
        width = width,
        height = height,
        x = x,
        y = y,
    )

    var text: String = inputField.text
        set(value) {
            inputField.text = value
            field = value
        }

    val controller get() = inputField.controller

    fun focus() {
        inputField.focus()
    }

    fun blur() {
        inputField.blur()
    }

    fun selectAll() {
        inputField.selectAll()
    }

    fun onClick(block: () -> Unit) {
        inputField.onClick(block)
    }
}
