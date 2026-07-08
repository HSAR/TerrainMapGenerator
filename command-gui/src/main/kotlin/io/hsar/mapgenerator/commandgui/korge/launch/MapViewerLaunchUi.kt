package io.hsar.mapgenerator.commandgui.korge.launch

import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerActionButtons
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerFontRole
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerTextViews
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiBoxes
import io.hsar.mapgenerator.commandgui.korge.ui.MapViewerUiStateStyle
import io.hsar.mapgenerator.commandgui.korge.ui.ScreenBounds
import io.hsar.mapgenerator.image.Palette
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.xy
import java.awt.Color

internal object MapViewerLaunchUi {
    fun panel(parent: Container, bounds: ScreenBounds, alpha: Int = 220): Container =
        MapViewerUiBoxes.box(parent, bounds.width, bounds.height, style = {
            io.hsar.mapgenerator.commandgui.korge.ui.UiFrameStyle(
                fillColor = Palette.Colours.DARK,
                fillAlpha = alpha,
                borderColor = Palette.Colours.WHITE,
                borderAlpha = MapViewerUiStateStyle.ACTIVE_ALPHA,
            )
        }).also {
            it.xy(bounds.left, bounds.top)
        }

    fun button(
        parent: Container,
        width: Double,
        height: Double,
        label: String,
        disabled: Boolean = false,
        uppercase: Boolean = false,
        onClick: () -> Unit = {},
    ): Container =
        MapViewerActionButtons.button(
            parent = parent,
            width = width,
            height = height,
            label = label,
            enabled = !disabled,
            uppercase = uppercase,
            onClick = onClick,
        )

    fun backButton(parent: Container, onClick: () -> Unit = {}): Container =
        button(parent, 120.0, 42.0, "Back", uppercase = true, onClick = onClick)

    fun label(parent: Container, text: String, strong: Boolean = false, color: Color = Palette.Colours.WHITE): Text =
        label(parent, text, strong, color, scale = 1.0)

    fun title(
        parent: Container,
        text: String,
        width: Double,
        height: Double,
        bold: Boolean = false,
        size: Double = MapViewerTextViews.DEFAULT_TEXT_SIZE,
        color: Color = Palette.Colours.WHITE,
        alignment: TextAlignment = TextAlignment.MIDDLE_CENTER,
    ): Text =
        MapViewerTextViews.titleBox(
            parent = parent,
            text = text,
            width = width,
            height = height,
            bold = bold,
            size = size,
            color = color,
            alignment = alignment,
        )

    fun screenTitle(parent: Container, text: String, width: Double, height: Double, color: Color = Palette.Colours.WHITE): Text =
        title(parent, text, width, height, bold = true, size = SCREEN_TITLE_TEXT_SIZE, color = color)

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

    private const val SCREEN_TITLE_TEXT_SIZE = 34.0
}
