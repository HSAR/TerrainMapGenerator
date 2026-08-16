package io.hsar.mapgenerator.commandgui.korge.persistence

import io.hsar.mapgenerator.commandgui.korge.interaction.EcmActivation
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.korge
import io.hsar.mapgenerator.commandgui.korge.interaction.operation
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalFireUsage
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalTurnMarker

/** KorGE ID adapter over the neutral tactical turn-marker codec. */
internal object TacticalTurnMarker {
    const val PROPERTY_KEY = OperationTacticalTurnMarker.PROPERTY_KEY
    fun hasMarker(properties: Map<String, String>) = OperationTacticalTurnMarker.hasMarker(properties)
    fun usage(properties: Map<String, String>, turnNumber: Int) = OperationTacticalTurnMarker.usage(properties, turnNumber)?.korge()
    fun usages(properties: Map<String, String>, unitId: MapPlacedObjectId) = OperationTacticalTurnMarker.usages(properties, OperationObjectId(unitId.value))
        .mapKeys { TacticalFireKey(it.key.turnNumber, unitId) }.mapValues { it.value.korge() }
    fun activations(properties: Map<String, String>, emitterId: MapPlacedObjectId) = OperationTacticalTurnMarker.activations(properties, OperationObjectId(emitterId.value)).map { it.korge() }
    fun update(properties: Map<String, String>, turnNumber: Int, usage: TacticalFireUsage, activation: EcmActivation? = null) =
        OperationTacticalTurnMarker.update(properties, turnNumber, OperationTacticalFireUsage(usage.firedWeapons, usage.fireControlUsed), activation?.operation())

    private fun OperationTacticalFireUsage.korge() = TacticalFireUsage(firedWeapons, fireControlUsed)
}
