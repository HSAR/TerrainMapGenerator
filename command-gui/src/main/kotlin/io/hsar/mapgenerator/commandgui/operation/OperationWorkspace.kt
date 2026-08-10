package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.ViewerPoint
import java.util.Collections.emptyList

@JvmInline
value class OperationEditorId(val value: String)

@JvmInline
value class OperationEditorKind(val value: String) {
    companion object {
        val OPERATION_NOTES = OperationEditorKind("operation-notes")
        val UNIT = OperationEditorKind("unit")
        val UNIT_TEMPLATE = OperationEditorKind("unit-template")
    }
}

sealed interface OperationEditorTarget {
    data object OperationNotes : OperationEditorTarget
    data class Object(val objectId: OperationObjectId) : OperationEditorTarget
    data object NewUnitTemplate : OperationEditorTarget
    data class UnitTemplate(val templateId: OperationUnitTemplateId) : OperationEditorTarget
    data class Opaque(val value: String?) : OperationEditorTarget
}

sealed interface OperationEditorDraft {
    data class Notes(val body: String = "") : OperationEditorDraft
    data class Unit(
        val name: String,
        val positionX: String,
        val positionY: String,
        val facing: String,
        val destroyed: Boolean,
        val unitTemplateId: OperationUnitTemplateId? = null,
        val affiliation: OperationUnitAffiliation? = null,
        val properties: List<OperationPropertyDraft> = emptyList(),
    ) : OperationEditorDraft

    data class UnitTemplate(val value: OperationUnitTemplateDraft = OperationUnitTemplateDraft()) : OperationEditorDraft

    data class Opaque(val value: String = "") : OperationEditorDraft
}

data class OperationEditorBounds(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
)

data class OperationFloatingEditorState(
    val bounds: OperationEditorBounds,
    val minimized: Boolean = false,
    val pinned: Boolean = false,
    val mapAnchor: ViewerPoint? = null,
)

enum class OperationDockRegion { LEFT, RIGHT, BOTTOM }

data class OperationDockRegionState(
    val region: OperationDockRegion,
    val size: Double,
    val editorIds: List<OperationEditorId> = emptyList(),
    val activeEditorId: OperationEditorId? = null,
    val collapsed: Boolean = false,
)

data class OperationEditorState(
    val id: OperationEditorId,
    val kind: OperationEditorKind,
    val target: OperationEditorTarget,
    val draft: OperationEditorDraft,
    val floating: OperationFloatingEditorState,
)

data class OperationWorkspaceState(
    val mode: MapViewerWorkspaceMode = MapViewerWorkspaceMode.DOCKED,
    /** Back-to-front order for floating windows. */
    val editors: List<OperationEditorState> = emptyList(),
    val activeEditorId: OperationEditorId? = editors.lastOrNull()?.id,
    val dockRegions: List<OperationDockRegionState> = emptyList(),
) {
    fun editor(id: OperationEditorId) = editors.firstOrNull { it.id == id }

    fun open(editor: OperationEditorState, region: OperationDockRegion = OperationDockRegion.RIGHT): OperationWorkspaceState {
        val existing = editors.firstOrNull { it.id == editor.id || it.kind == editor.kind && it.target == editor.target }
        if (existing != null) return activate(existing.id)
        val nextEditors = editors + editor
        return copy(
            editors = nextEditors,
            activeEditorId = editor.id,
            dockRegions = dockRegions.withEditor(editor.id, region),
        )
    }

    fun close(id: OperationEditorId) = copy(
        editors = editors.filterNot { it.id == id },
        activeEditorId = activeEditorId.takeUnless { it == id } ?: editors.lastOrNull { it.id != id }?.id,
        dockRegions = dockRegions.map { region ->
            val remaining = region.editorIds.filterNot { it == id }
            region.copy(
                editorIds = remaining,
                activeEditorId = region.activeEditorId.takeUnless { it == id } ?: remaining.lastOrNull(),
            )
        },
    )

    fun activate(id: OperationEditorId): OperationWorkspaceState {
        val editor = editor(id) ?: return this
        return copy(
            editors = editors.filterNot { it.id == id } + editor,
            activeEditorId = id,
            dockRegions = dockRegions.map { region ->
                if (id in region.editorIds) region.copy(activeEditorId = id, collapsed = false) else region
            },
        )
    }

    fun updateEditor(id: OperationEditorId, update: (OperationEditorState) -> OperationEditorState) =
        copy(editors = editors.map { if (it.id == id) update(it) else it })

    fun updateDock(region: OperationDockRegion, update: (OperationDockRegionState) -> OperationDockRegionState) = copy(
        dockRegions = dockRegions.map { if (it.region == region) update(it) else it },
    )

    fun dock(id: OperationEditorId, region: OperationDockRegion, index: Int? = null): OperationWorkspaceState {
        if (editor(id) == null) return this
        val without = dockRegions.map { dock ->
            val ids = dock.editorIds.filterNot { it == id }
            dock.copy(editorIds = ids, activeEditorId = dock.activeEditorId.takeUnless { it == id } ?: ids.lastOrNull())
        }
        val target = without.firstOrNull { it.region == region } ?: OperationDockRegionState(region, 320.0)
        val insertion = (index ?: target.editorIds.size).coerceIn(0, target.editorIds.size)
        val ids = target.editorIds.toMutableList().also { it.add(insertion, id) }.toList()
        val updated = target.copy(editorIds = ids, activeEditorId = id, collapsed = false)
        return copy(dockRegions = if (target in without) without.map { if (it.region == region) updated else it } else without + updated)
    }

    fun reorderDock(region: OperationDockRegion, orderedIds: List<OperationEditorId>): OperationWorkspaceState = updateDock(region) { dock ->
        val requested = orderedIds.filter(dock.editorIds::contains).distinct()
        dock.copy(editorIds = requested + dock.editorIds.filterNot(requested::contains))
    }

    fun withMode(value: MapViewerWorkspaceMode) = copy(mode = value)

    companion object {
        val DEFAULT_NOTES_ID = OperationEditorId("operation-notes")

        fun default(notes: String = "") = OperationWorkspaceState().open(
            OperationEditorState(
                id = DEFAULT_NOTES_ID,
                kind = OperationEditorKind.OPERATION_NOTES,
                target = OperationEditorTarget.OperationNotes,
                draft = OperationEditorDraft.Notes(notes),
                floating = OperationFloatingEditorState(OperationEditorBounds(32.0, 32.0, 340.0, 360.0)),
            ),
        )
    }
}

fun OperationUnit.editorState(
    bounds: OperationEditorBounds = OperationEditorBounds(96.0, 96.0, 520.0, 560.0),
    renderData: io.hsar.mapgenerator.commandgui.MapViewerRenderData? = null,
): OperationEditorState {
    val position = renderData?.let { OperationUnitAuthoring.formatPosition(it, coordinate) }
        ?: io.hsar.mapgenerator.commandgui.GridReferenceFields(coordinate.x.toString(), coordinate.y.toString())
    return OperationEditorState(
        id = OperationEditorId("unit:${id.value}"),
        kind = OperationEditorKind.UNIT,
        target = OperationEditorTarget.Object(id),
        draft = OperationEditorDraft.Unit(
            name = instanceName, positionX = position.easting, positionY = position.northing,
            facing = facingDegrees?.toString().orEmpty(), destroyed = destroyed, unitTemplateId = unitTemplateId, affiliation = affiliation,
            properties = customProperties.map { OperationPropertyDraft(it.key, it.value) },
        ),
        floating = OperationFloatingEditorState(bounds),
    )
}

fun OperationUnitTemplate.editorState(
    bounds: OperationEditorBounds = OperationEditorBounds(96.0, 96.0, 520.0, 600.0),
) = OperationEditorState(
    id = OperationEditorId("unit-template:${id.value}"),
    kind = OperationEditorKind.UNIT_TEMPLATE,
    target = OperationEditorTarget.UnitTemplate(id),
    draft = OperationEditorDraft.UnitTemplate(
        OperationUnitTemplateDraft(
            name = name,
            affiliation = affiliation,
            properties = customProperties.map { OperationPropertyDraft(it.key, it.value) },
        ),
    ),
    floating = OperationFloatingEditorState(bounds),
)

private fun List<OperationDockRegionState>.withEditor(id: OperationEditorId, target: OperationDockRegion): List<OperationDockRegionState> {
    val existing = firstOrNull { it.region == target }
    val updated = (existing ?: OperationDockRegionState(target, 320.0)).let { region ->
        region.copy(editorIds = region.editorIds + id, activeEditorId = id, collapsed = false)
    }
    return if (existing == null) this + updated else map { if (it.region == target) updated else it }
}
