package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.commandgui.operation.OperationFireArcDisplayMode
import io.hsar.mapgenerator.commandgui.operation.OperationFireArcSelectionService
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation

internal typealias FireArcDisplayMode = OperationFireArcDisplayMode

internal object FireArcSelectionResolver {
    fun <T> shouldDraw(mode: FireArcDisplayMode, selectedUnitIds: Set<T>, selectedAffiliations: List<UnitAffiliation?>, targetId: T, targetAffiliation: UnitAffiliation?) =
        OperationFireArcSelectionService.shouldDraw(mode, selectedUnitIds, selectedAffiliations.map { it?.operation() }, targetId, targetAffiliation?.operation())

    private fun UnitAffiliation.operation() = OperationUnitAffiliation.valueOf(name.uppercase())
}
