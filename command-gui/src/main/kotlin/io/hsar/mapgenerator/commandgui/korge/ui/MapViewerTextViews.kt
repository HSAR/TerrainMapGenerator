package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.text
import korlibs.math.geom.Rectangle
import java.awt.Color

internal object MapViewerTextViews {
    fun label(
        parent: Container,
        text: String,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        size: Double = DEFAULT_TEXT_SIZE,
        color: Color = Color.WHITE,
        alignment: TextAlignment = TextAlignment.TOP_LEFT,
        bounds: Rectangle? = null,
        block: Text.() -> Unit = {},
    ): Text {
        return parent.text(
            text = text,
            textSize = size,
            color = color.toRgba(),
            font = MapViewerKorgeFonts.font(role),
            alignment = alignment,
            autoScaling = true,
        ) {
            smoothing = true
            if (bounds == null) {
                autoSize = true
            } else {
                autoSize = false
                setTextBounds(bounds)
            }
            block()
        }
    }

    fun box(
        parent: Container,
        text: String,
        width: Double,
        height: Double,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        size: Double = DEFAULT_TEXT_SIZE,
        color: Color = Color.WHITE,
        alignment: TextAlignment = TextAlignment.MIDDLE_CENTER,
        block: Text.() -> Unit = {},
    ): Text =
        label(
            parent = parent,
            text = text,
            role = role,
            size = size,
            color = color,
            alignment = alignment,
            bounds = Rectangle(0.0, 0.0, width, height),
            block = block,
        )

    fun leftBox(
        parent: Container,
        text: String,
        width: Double,
        height: Double,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        size: Double = DEFAULT_TEXT_SIZE,
        color: Color = Color.WHITE,
        block: Text.() -> Unit = {},
    ): Text =
        box(
            parent = parent,
            text = text,
            width = width,
            height = height,
            role = role,
            size = size,
            color = color,
            alignment = TextAlignment.MIDDLE_LEFT,
            block = block,
        )

    fun bounds(text: String, role: MapViewerFontRole = MapViewerFontRole.MAIN, size: Double = DEFAULT_TEXT_SIZE): Rectangle =
        measure(text, role, size)

    fun measure(text: String, role: MapViewerFontRole = MapViewerFontRole.MAIN, size: Double = DEFAULT_TEXT_SIZE): Rectangle =
        Container().text(
            text = text,
            textSize = size,
            color = Color.WHITE.toRgba(),
            font = MapViewerKorgeFonts.font(role),
            alignment = TextAlignment.TOP_LEFT,
            autoScaling = true,
        ) {
            smoothing = true
            autoSize = true
        }.textBounds.let { bounds ->
            Rectangle(0.0, 0.0, bounds.width, bounds.height)
        }

    fun Color.toRgba(alpha: Int = this.alpha): RGBA =
        RGBA(red, green, blue, alpha)

    const val DEFAULT_TEXT_SIZE = 20.0
}
