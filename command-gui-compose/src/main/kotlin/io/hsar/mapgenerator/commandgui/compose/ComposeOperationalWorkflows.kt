package io.hsar.mapgenerator.commandgui.compose

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlags
import io.hsar.mapgenerator.commandgui.operation.*
import java.util.UUID

internal val ComposeOperationState.olympiadDawnEnabled: Boolean
    get() = MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL in MapViewerFeatureFlags.enabled

internal fun ComposeOperationState.openOlympiadDawnStatus(unitId: OperationObjectId) =
    openOperationalEditor(unitId, OperationEditorKind.OLYMPIAD_DAWN_STATUS, "status", "status:$unitId")

internal fun ComposeOperationState.openSensorReport(unitId: OperationObjectId, mode: OperationWorkflowReportMode): ComposeOperationState {
    if (!olympiadDawnEnabled || isReadOnly) return this
    val result = OperationWorkflowFacade.sensorReport(
        unitId, mode, operationalTurnNumber(), document.content, document.renderData, document.settings, document.additionalDataJson,
    ) ?: return this
    val editor = operationalEditor(unitId, OperationEditorKind.SENSOR_REPORT, mode.name.lowercase(), "sensor:${mode.name.lowercase()}:$unitId", result.text)
    return copy(
        document = document.copy(additionalDataJson = result.additionalDataJson), workspace = workspace.open(editor),
        revision = revision + 1,
    )
}

internal fun ComposeOperationState.openTactical(unitId: OperationObjectId) =
    openOperationalEditor(unitId, OperationEditorKind.TACTICAL, "tactical", "tactical:$unitId")

internal fun ComposeOperationState.tacticalReport(unitId: OperationObjectId): OperationWorkflowTacticalReport? =
    takeIf { olympiadDawnEnabled }?.let {
        OperationWorkflowFacade.tacticalReport(unitId, operationalTurnNumber(), document.content, document.renderData, document.settings, document.additionalDataJson)
    }

internal fun ComposeOperationState.ecmFields(): List<OperationWorkflowEcmField> =
    if (olympiadDawnEnabled) OperationWorkflowFacade.ecmFields(operationalTurnNumber(), document.additionalDataJson) else emptyList()

internal fun ComposeOperationState.toggleOperationalDamage(unitId: OperationObjectId, token: String): ComposeOperationState {
    if (!olympiadDawnEnabled || isReadOnly) return this
    val unit = document.content.objectById(unitId) as? OperationUnit ?: return this
    val updated = OperationWorkflowFacade.toggleDamage(unit, document.content, token) ?: return this
    return editUnit(updated)
}

internal fun ComposeOperationState.fireTactical(editorId: OperationEditorId): ComposeOperationState {
    if (!olympiadDawnEnabled || isReadOnly) return this
    val editor = workspace.editor(editorId) ?: return this
    val attackerId = (editor.target as? OperationEditorTarget.Object)?.objectId ?: return this
    val draft = editor.draft as? OperationEditorDraft.Operational ?: return this
    val targetId = draft.selectedTargetId ?: return this
    val result = OperationWorkflowFacade.fire(
        attackerId, targetId, draft.selectedWeaponKeys, operationalTurnNumber(), document.content,
        document.renderData, document.settings, document.additionalDataJson,
    ) ?: return this
    val change = result.content.changesFrom(document.content)
    val entry = OperationTimelineEntries.change(document.content, change) ?: return this
    val gunrep = operationalEditor(targetId, OperationEditorKind.GUNREP, "gunrep", "gunrep:${UUID.randomUUID()}", result.gunrepText)
    return copy(
        document = document.copy(timeline = document.timeline.record(entry), additionalDataJson = result.additionalDataJson),
        workspace = workspace.open(gunrep), revision = revision + 1,
    )
}

private fun ComposeOperationState.openOperationalEditor(unitId: OperationObjectId, kind: OperationEditorKind, mode: String, id: String): ComposeOperationState {
    if (!olympiadDawnEnabled || visibleContent.objectById(unitId) !is OperationUnit) return this
    return copy(workspace = workspace.open(operationalEditor(unitId, kind, mode, id)), revision = revision + 1)
}

private fun operationalEditor(unitId: OperationObjectId, kind: OperationEditorKind, mode: String, id: String, report: String = "") = OperationEditorState(
    OperationEditorId(id), kind, OperationEditorTarget.Object(unitId), OperationEditorDraft.Operational(mode, report),
    OperationFloatingEditorState(OperationEditorBounds(120.0, 90.0, 600.0, 640.0)),
)

private fun ComposeOperationState.operationalTurnNumber() =
    document.timeline.entries.take(document.timeline.appliedCount).count { it.turnName != null }.coerceAtLeast(1)
