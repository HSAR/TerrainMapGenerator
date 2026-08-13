package io.hsar.mapgenerator.commandgui.korge.persistence

import io.hsar.mapgenerator.commandgui.save.OperationAdditionalData
import io.hsar.mapgenerator.commandgui.save.EcmPersistence
import io.hsar.mapgenerator.commandgui.save.SitrepContactPersistence
import io.hsar.mapgenerator.commandgui.save.UiSessionState

/** KorGE-only tactical records layered over the neutral additive additional-data document. */
internal class LegacyOperationAdditionalData private constructor(
    private val shared: OperationAdditionalData,
) {
    val sitrepContacts = SitrepContactPersistence.fromRoot(shared.root)
    val tacticalFire = TacticalFirePersistence.fromRoot(shared.root)
    val ecm = EcmPersistence.fromRoot(shared.root)

    fun uiState() = shared.uiState()
    fun updateUiState(state: UiSessionState) = shared.updateUiState(state)

    fun toJsonOrNull(): ByteArray? {
        sitrepContacts.toJson()
        tacticalFire.toJson()
        ecm.toJson()
        return shared.toJsonOrNull()
    }

    companion object {
        fun fromJson(json: ByteArray?) = LegacyOperationAdditionalData(OperationAdditionalData.fromJson(json))
    }
}
