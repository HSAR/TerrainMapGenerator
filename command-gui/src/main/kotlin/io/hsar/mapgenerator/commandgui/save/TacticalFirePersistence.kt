package io.hsar.mapgenerator.commandgui.korge.persistence

import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalFireKey
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalFirePersistence
import io.hsar.mapgenerator.commandgui.operation.OperationTacticalFireUsage

internal data class TacticalFireKey(val turnNumber: Int, val unitId: MapPlacedObjectId)
internal data class TacticalFireUsage(val firedWeapons: Set<String> = emptySet(), val fireControlUsed: Int = 0)

internal class TacticalFirePersistence private constructor(private val delegate: OperationTacticalFirePersistence) {
    private fun TacticalFireKey.operation() = OperationTacticalFireKey(turnNumber, OperationObjectId(unitId.value))
    private fun TacticalFireUsage.operation() = OperationTacticalFireUsage(firedWeapons, fireControlUsed)
    private fun OperationTacticalFireUsage.korge() = TacticalFireUsage(firedWeapons, fireControlUsed)
    fun usage(key: TacticalFireKey) = delegate.usage(key.operation()).korge()
    fun markFired(key: TacticalFireKey, weaponKeys: Set<String>) = delegate.markFired(key.operation(), weaponKeys)
    fun replace(key: TacticalFireKey, usage: TacticalFireUsage) = delegate.replace(key.operation(), usage.operation())
    fun clearTurn(turnNumber: Int) = delegate.clearTurn(turnNumber)
    fun reconcileMarked(entries: Map<TacticalFireKey, TacticalFireUsage>) = delegate.reconcileMarked(entries.mapKeys { it.key.operation() }.mapValues { it.value.operation() })
    fun toJson() = delegate.toJson()
    companion object {
        const val FIELD_NAME = OperationTacticalFirePersistence.FIELD_NAME
        fun fromRoot(root: ObjectNode) = TacticalFirePersistence(OperationTacticalFirePersistence.fromRoot(root))
        fun fromJson(json: ByteArray?) = TacticalFirePersistence(OperationTacticalFirePersistence.fromJson(json))
    }
}
