package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.korge.ui.UnitAffiliationUi
import java.awt.Color

internal fun MapPlacedObjectSnapshot.selectionAccentColor(): Color =
    when (this) {
        is UnitSnapshot -> UnitAffiliationUi.swatchColor(template.affiliation)
        is DrawingSnapshot -> UnitAffiliationUi.swatchColor(null)
    }

internal fun List<MapPlacedObjectSnapshot>.selectionAccentColor(): Color =
    selectedUnitAffiliation()?.let(UnitAffiliationUi::swatchColor) ?: UnitAffiliationUi.swatchColor(null)

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
