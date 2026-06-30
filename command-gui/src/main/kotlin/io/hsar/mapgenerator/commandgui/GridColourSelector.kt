package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.image.Palette
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

object GridColourSelector {
    val PRESETS = listOf(
        Palette.Colours.LIGHT,
        Palette.Colours.MAIN,
        Palette.Colours.DARK,
    )

    fun resolve(savedHex: String?, mapImage: BufferedImage): Color =
        savedHex
            ?.let(::fromHex)
            ?.takeIf { it in PRESETS }
            ?: bestPresetFor(mapImage)

    fun bestPresetFor(mapImage: BufferedImage): Color {
        val average = mapImage.averageColor()
        return PRESETS.maxBy { preset -> contrastRatio(preset, average) }
    }

    fun toHex(color: Color): String =
        "#%02X%02X%02X".format(color.red, color.green, color.blue)

    fun fromHex(hex: String): Color? {
        val normalized = hex.trim().removePrefix("#")
        if (!Regex("[0-9A-Fa-f]{6}").matches(normalized)) {
            return null
        }
        return Color(
            normalized.substring(0, 2).toInt(16),
            normalized.substring(2, 4).toInt(16),
            normalized.substring(4, 6).toInt(16),
        )
    }

    private fun BufferedImage.averageColor(): Color {
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L

        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = getRGB(x, y)
                val alpha = (argb ushr 24) and 0xFF
                if (alpha > 0) {
                    red += (argb ushr 16) and 0xFF
                    green += (argb ushr 8) and 0xFF
                    blue += argb and 0xFF
                    count++
                }
            }
        }

        return if (count == 0L) {
            Color.BLACK
        } else {
            Color((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminosity = first.relativeLuminosity()
        val secondLuminosity = second.relativeLuminosity()
        return (max(firstLuminosity, secondLuminosity) + 0.05) /
                (min(firstLuminosity, secondLuminosity) + 0.05)
    }

    private fun Color.relativeLuminosity(): Double =
        0.2126 * red.linearChannel() +
                0.7152 * green.linearChannel() +
                0.0722 * blue.linearChannel()

    private fun Int.linearChannel(): Double =
        (this / 255.0).let { channel ->
            if (channel <= 0.03928) {
                channel / 12.92
            } else {
                Math.pow((channel + 0.055) / 1.055, 2.4)
            }
        }
}
