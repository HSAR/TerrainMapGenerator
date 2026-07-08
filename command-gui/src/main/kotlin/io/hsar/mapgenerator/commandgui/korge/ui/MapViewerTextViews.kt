package io.hsar.mapgenerator.commandgui.korge.ui

import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.text
import korlibs.math.geom.Rectangle
import java.awt.Color
import java.util.*

internal object MapViewerTextViews {
    fun titleText(text: String): String =
        text.uppercase(Locale.ROOT)

    fun titleFontRole(bold: Boolean = false): MapViewerFontRole =
        if (bold) MapViewerFontRole.STRONG else MapViewerFontRole.MAIN

    fun titleBox(
        parent: Container,
        text: String,
        width: Double,
        height: Double,
        bold: Boolean = false,
        size: Double = DEFAULT_TEXT_SIZE,
        color: Color = Color.WHITE,
        alignment: TextAlignment = TextAlignment.MIDDLE_CENTER,
        block: Text.() -> Unit = {},
    ): Text =
        box(
            parent = parent,
            text = titleText(text),
            width = width,
            height = height,
            role = titleFontRole(bold),
            size = size,
            color = color,
            alignment = alignment,
            block = block,
        )

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

    fun ellipsize(
        text: String,
        maxWidth: Double,
        role: MapViewerFontRole = MapViewerFontRole.MAIN,
        size: Double = DEFAULT_TEXT_SIZE,
        suffix: String = "...",
    ): String {
        val textWidth = measure(text, role, size).width
        if (textWidth <= maxWidth) {
            return text
        }

        val suffixWidth = measure(suffix, role, size).width
        if (suffixWidth >= maxWidth) {
            return longestPrefixThatFits(text, maxWidth, role, size)
        }

        val prefixWidthLimit = maxWidth - suffixWidth
        return longestPrefixThatFits(text, prefixWidthLimit, role, size)
            .let { prefix -> if (prefix.length < text.length) "$prefix$suffix" else prefix }
    }

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

    private fun longestPrefixThatFits(
        text: String,
        maxWidth: Double,
        role: MapViewerFontRole,
        size: Double,
    ): String {
        if (text.isEmpty() || maxWidth <= 0.0) {
            return ""
        }

        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (measure(text.take(mid), role, size).width <= maxWidth) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return text.take(low)
    }

    const val DEFAULT_TEXT_SIZE = 24.0
}
