package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.image.Palette

internal object MapViewerUnitTemplateFactory {
    fun defaults(): List<MapViewerUnitTemplate> =
        listOf(
            textTemplate("Unit 1"),
            textTemplate("Unit 2"),
        )

    fun textTemplate(name: String): MapViewerUnitTemplate =
        MapViewerUnitTemplate(
            name = name,
            bitmap = MapViewerTextBitmapRenderer.render(name, Palette.Fonts.MAIN.first, Palette.Colours.WHITE),
        )
}
