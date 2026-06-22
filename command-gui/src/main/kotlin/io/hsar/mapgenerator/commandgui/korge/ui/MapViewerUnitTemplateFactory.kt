package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitTemplateId
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import java.util.*

internal object MapViewerUnitTemplateFactory {
    fun defaults(): List<MapViewerUnitTemplate> =
        listOf(
            template("Unit 1", RGBA(194, 62, 60, 255)),
            template("Unit 2", RGBA(39, 106, 136, 255)),
        )

    fun template(name: String, color: RGBA): MapViewerUnitTemplate =
        MapViewerUnitTemplate(
            id = UnitTemplateId(UUID.randomUUID()),
            name = name,
            bitmap = unitMarker(color),
        )

    private fun unitMarker(color: RGBA): Bitmap32 =
        Bitmap32(32, 32) { x, y ->
            val edge = x == 15 || x == 16 || y == 15 || y == 16
            val diamond = kotlin.math.abs(x - 15.5) + kotlin.math.abs(y - 15.5) <= 15.5
            when {
                edge -> RGBA(0, 0, 0, 255)
                diamond -> color
                else -> RGBA(0, 0, 0, 0)
            }
        }.premultipliedIfRequired()
}
