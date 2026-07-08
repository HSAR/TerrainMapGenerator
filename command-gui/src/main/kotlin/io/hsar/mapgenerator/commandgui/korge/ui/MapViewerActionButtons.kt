package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.image.Palette
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container

@OptIn(KorgeExperimental::class)
internal object MapViewerActionButtons {
    fun button(
        parent: Container,
        width: Double,
        height: Double,
        label: String,
        enabled: Boolean = true,
        active: Boolean = enabled,
        uppercase: Boolean = false,
        onClick: () -> Unit = {},
    ): Container =
        MapViewerUiBoxes.button(
            parent = parent,
            width = width,
            height = height,
            active = active,
        ).also { button ->
            MapViewerTextViews.box(
                parent = button,
                text = if (uppercase) MapViewerTextViews.titleText(label) else label,
                width = width,
                height = height,
                color = Palette.Colours.WHITE,
            ) {
                alpha = if (enabled) 1.0 else 0.55
            }
            if (enabled) {
                button.mouse {
                    onClick { onClick() }
                }
            }
        }
}
