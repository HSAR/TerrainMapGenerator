package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.image.Palette
import java.awt.Color

internal object UnitAffiliationUi {
    val options: List<MapViewerDropdownOption<UnitAffiliation>> =
        listOf(
            option(null, "No affiliation", Palette.Colours.MAIN),
            option(UnitAffiliation.Friend, "Friend", Palette.Colours.BLUE),
            option(UnitAffiliation.Hostile, "Hostile", Palette.Colours.RED),
            option(UnitAffiliation.Neutral, "Neutral", Palette.Colours.GREEN),
            option(UnitAffiliation.Unknown, "Unknown", Palette.Colours.YELLOW),
        )

    fun label(value: UnitAffiliation?): String =
        value?.name ?: "No affiliation"

    fun swatchColor(value: UnitAffiliation?): Color =
        when (value) {
            UnitAffiliation.Friend -> Palette.Colours.BLUE
            UnitAffiliation.Hostile -> Palette.Colours.RED
            UnitAffiliation.Neutral -> Palette.Colours.GREEN
            UnitAffiliation.Unknown -> Palette.Colours.YELLOW
            null -> Palette.Colours.MAIN
        }

    private fun option(
        value: UnitAffiliation?,
        label: String,
        color: Color,
    ): MapViewerDropdownOption<UnitAffiliation> =
        MapViewerDropdownOption(
            value = value,
            label = label,
            swatchColor = color,
        )
}
