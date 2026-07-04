package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import io.hsar.mapgenerator.image.Palette
import java.awt.Color

internal fun MapPlacedObjectSnapshot.selectionAccentColor(): Color =
    when (this) {
        is UnitSnapshot -> UnitAffiliationUi.swatchColor(template.affiliation)
        is DrawingSnapshot -> UnitAffiliationUi.swatchColor(null)
    }

internal fun List<MapPlacedObjectSnapshot>.selectionAccentColor(): Color =
    selectedUnitAffiliation()?.let(UnitAffiliationUi::swatchColor) ?: UnitAffiliationUi.swatchColor(null)

internal fun MapPlacedObjectSnapshot.measureTextColor(): Color =
    when (this) {
        is UnitSnapshot -> template.affiliation?.let(UnitAffiliationUi::swatchColor) ?: Palette.Colours.LIGHT
        is DrawingSnapshot -> UnitAffiliationUi.swatchColor(null)
    }

internal fun List<MapPlacedObjectSnapshot>.measureTextColor(): Color =
    selectedUnitAffiliation()?.let(UnitAffiliationUi::swatchColor) ?: Palette.Colours.LIGHT

private fun List<MapPlacedObjectSnapshot>.selectedUnitAffiliation(): UnitAffiliation? {
    val unitAffiliations = asSequence()
        .filterIsInstance<UnitSnapshot>()
        .mapNotNull { it.template.affiliation }
        .toList()

    if (unitAffiliations.isEmpty()) {
        return null
    }

    val firstAffiliation = unitAffiliations.first()
    return if (unitAffiliations.all { it == firstAffiliation }) {
        firstAffiliation
    } else {
        null
    }
}
