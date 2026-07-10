package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import java.awt.Color

internal fun MapPlacedObjectSnapshot.selectionAccentColor(): Color =
    selectionAccentColor(UnitAffiliationUi.unaffiliatedColor())

internal fun MapPlacedObjectSnapshot.selectionAccentColor(gridColor: Color): Color =
    when (this) {
        is UnitSnapshot -> template.affiliation.overlayColor()
        is DrawingSnapshot -> gridColor
    }

internal fun List<MapPlacedObjectSnapshot>.selectionAccentColor(): Color =
    selectionAccentColor(UnitAffiliationUi.unaffiliatedColor())

internal fun List<MapPlacedObjectSnapshot>.selectionAccentColor(gridColor: Color): Color =
    selectedUnitAffiliation()?.overlayColor() ?: gridColor

internal fun MapPlacedObjectSnapshot.measureTextColor(): Color =
    when (this) {
        is UnitSnapshot -> template.affiliation.overlayColor()
        is DrawingSnapshot -> UnitAffiliationUi.unaffiliatedColor()
    }

internal fun List<MapPlacedObjectSnapshot>.measureTextColor(): Color =
    selectedUnitAffiliation()?.overlayColor() ?: UnitAffiliationUi.unaffiliatedColor()

internal fun UnitAffiliation?.overlayColor(): Color =
    UnitAffiliationUi.swatchColor(this)

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
