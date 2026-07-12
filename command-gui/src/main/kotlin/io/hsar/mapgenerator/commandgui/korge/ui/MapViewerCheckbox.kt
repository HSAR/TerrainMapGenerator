package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

internal object MapViewerCheckbox {
    fun render(
        parent: Container,
        x: Double,
        y: Double,
        size: Double,
        checked: Boolean,
        onToggle: () -> Unit,
    ): Container {
        val checkbox = parent.container().xy(x, y)
        MapViewerUiBoxes.box(checkbox, size, size, active = checked)
        if (checked) {
            checkbox.solidRect(
                size - MARK_INSET * 2.0,
                size - MARK_INSET * 2.0,
                CHECK_MARK_COLOR,
            ).xy(MARK_INSET, MARK_INSET)
        }
        checkbox.mouse { onClick { onToggle() } }
        return checkbox
    }

    private const val MARK_INSET = 4.0
    private val CHECK_MARK_COLOR = korlibs.image.color.RGBA(255, 255, 255, 255)
}
