package io.hsar.mapgenerator.commandgui.save

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.MapViewerTool
import io.hsar.mapgenerator.commandgui.MapViewerWorkspaceMode
import io.hsar.mapgenerator.commandgui.ViewerPoint
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegion
import io.hsar.mapgenerator.commandgui.operation.OperationDockRegionState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorBounds
import io.hsar.mapgenerator.commandgui.operation.OperationEditorDraft
import io.hsar.mapgenerator.commandgui.operation.OperationEditorId
import io.hsar.mapgenerator.commandgui.operation.OperationEditorKind
import io.hsar.mapgenerator.commandgui.operation.OperationEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationEditorTarget
import io.hsar.mapgenerator.commandgui.operation.OperationFloatingEditorState
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicDraft
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationGraphicTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationImageDraft
import io.hsar.mapgenerator.commandgui.operation.OperationPropertyDraft
import io.hsar.mapgenerator.commandgui.operation.OperationUiState
import io.hsar.mapgenerator.commandgui.operation.OperationUnitAffiliation
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateDraft
import io.hsar.mapgenerator.commandgui.operation.OperationUnitTemplateId
import io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptyMap
import java.util.Collections.emptySet

internal data class UiSessionState(
    val selectedTool: MapViewerTool = MapViewerTool.SELECT,
    val selectedObjectIds: List<OperationObjectId> = emptyList(),
    val dialogs: List<MapViewerPersistedDialog> = emptyList(),
    val workspaceMode: MapViewerWorkspaceMode = MapViewerWorkspaceMode.FLOATING,
    val dockPanels: List<DockPanelSessionState> = emptyList(),
    val enabledLayerIds: Set<String> = emptySet(),
    val openEditorIds: List<String> = emptyList(),
    val drafts: Map<String, String> = emptyMap(),
    val workspace: OperationWorkspaceState? = null,
)

internal data class DockPanelSessionState(
    val region: String,
    val size: Double,
    val tabIds: List<String> = emptyList(),
    val activeTabId: String? = null,
    val collapsed: Boolean = false,
)

private fun migrateWorkspace(state: UiSessionState): OperationWorkspaceState {
    val legacyUnitEditors = state.dialogs.filter { it.type == "unit-edit" }.mapNotNull { dialog ->
        val objectId = dialog.targetId?.let { runCatching { OperationObjectId(UUID.fromString(it)) }.getOrNull() } ?: return@mapNotNull null
        val draft = dialog.draft
        OperationEditorState(
            id = OperationEditorId("unit:${objectId.value}"),
            kind = OperationEditorKind.UNIT,
            target = OperationEditorTarget.Object(objectId),
            draft = OperationEditorDraft.Unit(
                name = draft?.path("name")?.textValue().orEmpty(),
                positionX = draft?.path("positionX")?.textValue().orEmpty(),
                positionY = draft?.path("positionY")?.textValue().orEmpty(),
                facing = draft?.path("facing")?.textValue().orEmpty(),
                destroyed = draft?.path("destroyed")?.booleanValue() == true,
                affiliation = draft?.path("affiliation")?.textValue()?.let { runCatching { OperationUnitAffiliation.valueOf(it.uppercase()) }.getOrNull() },
                properties = draft?.path("customProperties")?.properties()?.asSequence()?.map { OperationPropertyDraft(it.key, it.value.textValue().orEmpty()) }?.toList().orEmpty(),
            ),
            floating = OperationFloatingEditorState(
                bounds = dialog.bounds.let { OperationEditorBounds(it.left, it.top, it.width, it.height) },
                minimized = dialog.minimized,
                pinned = dialog.pinned,
                mapAnchor = dialog.pinAnchor,
            ),
        )
    }
    val legacyById = legacyUnitEditors.associateBy { it.id }
    val idEditors = state.openEditorIds.map { value ->
        val id = OperationEditorId(value)
        legacyById[id] ?: when {
            value == OperationWorkspaceState.DEFAULT_NOTES_ID.value -> OperationWorkspaceState.default(state.drafts[value].orEmpty()).editors.single()
            value.startsWith("unit:") -> value.removePrefix("unit:").let { raw ->
                runCatching { OperationObjectId(UUID.fromString(raw)) }.getOrNull()?.let { objectId ->
                    OperationEditorState(
                        id, OperationEditorKind.UNIT, OperationEditorTarget.Object(objectId),
                        OperationEditorDraft.Unit("", "", "", "", false),
                        OperationFloatingEditorState(OperationEditorBounds(96.0, 96.0, 520.0, 560.0)),
                    )
                }
            }

            else -> OperationEditorState(
                id, OperationEditorKind("opaque"), OperationEditorTarget.Opaque(value),
                OperationEditorDraft.Opaque(state.drafts[value].orEmpty()),
                OperationFloatingEditorState(OperationEditorBounds(96.0, 96.0, 420.0, 360.0)),
            )
        }
    }.filterNotNull()
    val editors = (idEditors + legacyUnitEditors).distinctBy { it.id }.ifEmpty { OperationWorkspaceState.default().editors }
    val validIds = editors.mapTo(hashSetOf()) { it.id }
    val docks = state.dockPanels.mapNotNull { panel ->
        val region = OperationDockRegion.entries.firstOrNull { it.name.equals(panel.region, ignoreCase = true) } ?: return@mapNotNull null
        val ids = panel.tabIds.map(::OperationEditorId).filter(validIds::contains)
        OperationDockRegionState(region, panel.size, ids, panel.activeTabId?.let(::OperationEditorId)?.takeIf(validIds::contains), panel.collapsed)
    }.let { regions ->
        if (regions.any { it.region == OperationDockRegion.RIGHT }) regions else regions + OperationDockRegionState(
            OperationDockRegion.RIGHT, 320.0, editors.map { it.id }, editors.lastOrNull()?.id,
        )
    }
    return OperationWorkspaceState(state.workspaceMode, editors, editors.lastOrNull()?.id, docks)
}

private fun OperationWorkspaceState.compatibilityDrafts(): Map<String, String> = editors.mapNotNull { editor ->
    when (val draft = editor.draft) {
        is OperationEditorDraft.Notes -> editor.id.value to draft.body
        is OperationEditorDraft.Opaque -> editor.id.value to draft.value
        is OperationEditorDraft.Unit -> null
        is OperationEditorDraft.UnitTemplate -> null
        is OperationEditorDraft.Graphic -> null
        is OperationEditorDraft.GraphicTemplate -> null
    }
}.toMap()

private fun OperationDockRegionState.toCompatibilityPanel() = DockPanelSessionState(
    region = region.name.lowercase(),
    size = size,
    tabIds = editorIds.map { it.value },
    activeTabId = activeEditorId?.value,
    collapsed = collapsed,
)

private fun List<MapViewerPersistedDialog>.mergeWorkspaceDialogs(workspace: OperationWorkspaceState): List<MapViewerPersistedDialog> {
    val existingUnits = filter { it.type == "unit-edit" }.associateBy(MapViewerPersistedDialog::targetId)
    val retained = filterNot { it.type == "unit-edit" }
    val generated = workspace.editors.mapNotNull { editor ->
        val target = editor.target as? OperationEditorTarget.Object ?: return@mapNotNull null
        val draft = editor.draft as? OperationEditorDraft.Unit ?: return@mapNotNull null
        if (editor.kind != OperationEditorKind.UNIT) return@mapNotNull null
        val original = existingUnits[target.objectId.value.toString()]
        val draftJson = original?.draft?.takeIf(JsonNode::isObject)?.deepCopy<ObjectNode>()
            ?: JsonNodeFactory.instance.objectNode()
        draftJson.put("name", draft.name)
        draftJson.put("positionX", draft.positionX)
        draftJson.put("positionY", draft.positionY)
        draftJson.put("facing", draft.facing)
        draftJson.put("destroyed", draft.destroyed)
        draft.affiliation?.let { draftJson.put("affiliation", it.name) } ?: draftJson.remove("affiliation")
        draftJson.putObject("customProperties").also { properties -> draft.properties.forEach { properties.put(it.key, it.value) } }
        MapViewerPersistedDialog(
            type = "unit-edit",
            targetId = target.objectId.value.toString(),
            bounds = editor.floating.bounds.let { MapViewerDialogBounds(it.left, it.top, it.width, it.height) },
            draft = draftJson,
            minimized = editor.floating.minimized,
            pinned = editor.floating.pinned,
            pinAnchor = editor.floating.mapAnchor,
        )
    }
    return retained + generated
}

private fun OperationWorkspaceState.mergeLegacyUnitDialogs(state: UiSessionState): OperationWorkspaceState {
    val legacyUnits = migrateWorkspace(state.copy(openEditorIds = emptyList(), drafts = emptyMap()))
        .editors.filter { it.kind == OperationEditorKind.UNIT }
    val retained = editors.filterNot { it.kind == OperationEditorKind.UNIT }
    val merged = retained + legacyUnits
    val ids = merged.mapTo(hashSetOf()) { it.id }
    val regions = dockRegions.map { dock ->
        val retainedIds = dock.editorIds.filter(ids::contains)
        val appended = if (dock.region == OperationDockRegion.RIGHT) legacyUnits.map { it.id }.filterNot(retainedIds::contains) else emptyList()
        val editorIds = retainedIds + appended
        dock.copy(editorIds = editorIds, activeEditorId = dock.activeEditorId?.takeIf(ids::contains) ?: editorIds.lastOrNull())
    }
    return copy(
        mode = state.workspaceMode,
        editors = merged,
        activeEditorId = activeEditorId?.takeIf(ids::contains) ?: merged.lastOrNull()?.id,
        dockRegions = regions,
    )
}

/** Owns the complete additive additionalData.json document. */
internal class OperationAdditionalData private constructor(internal val root: ObjectNode) {

    fun uiState(): UiSessionState {
        val state = root.path(UI_STATE_FIELD).takeIf { it.isObject } ?: return UiSessionState()
        val tool = state.path("selectedTool").textValue()
            ?.let { value -> MapViewerTool.entries.firstOrNull { it.name == value } }
            ?: MapViewerTool.SELECT
        val selected = state.path("selectedObjectIds").mapNotNull { node ->
            node.textValue()?.let { runCatching { OperationObjectId(UUID.fromString(it)) }.getOrNull() }
        }
        val dialogs = state.path("dialogs").mapNotNull(MapViewerPersistedDialog::fromJson)
        val workspaceMode = state.path("workspaceMode").textValue()
            ?.let { value -> MapViewerWorkspaceMode.entries.firstOrNull { it.name == value } }
            ?: MapViewerWorkspaceMode.FLOATING
        val dockPanels = state.path("dockPanels").mapNotNull { panel ->
            val region = panel.path("region").textValue() ?: return@mapNotNull null
            val size = panel.path("size").doubleValue().takeIf { panel.path("size").isNumber && it >= 0.0 }
                ?: return@mapNotNull null
            DockPanelSessionState(
                region = region,
                size = size,
                tabIds = panel.path("tabIds").mapNotNull(JsonNode::textValue),
                activeTabId = panel.path("activeTabId").textValue(),
                collapsed = panel.path("collapsed").booleanValue(),
            )
        }
        val enabledLayerIds = state.path("enabledLayerIds").mapNotNull(JsonNode::textValue).toSet()
        val openEditorIds = state.path("openEditorIds").mapNotNull(JsonNode::textValue)
        val drafts = state.path("drafts").takeIf(JsonNode::isObject)?.properties()?.asSequence()
            ?.mapNotNull { (id, value) -> value.textValue()?.let { id to it } }
            ?.toMap().orEmpty()
        val workspace = state.path("workspace").takeIf(JsonNode::isObject)?.let(::readWorkspace)
        return UiSessionState(tool, selected, dialogs, workspaceMode, dockPanels, enabledLayerIds, openEditorIds, drafts, workspace)
    }

    fun updateUiState(state: UiSessionState) {
        val value = root.path(UI_STATE_FIELD).takeIf(JsonNode::isObject)?.deepCopy<ObjectNode>()
            ?: JsonNodeFactory.instance.objectNode()
        root.set<ObjectNode>(UI_STATE_FIELD, value)
        val workspace = state.workspace ?: value.path("workspace").takeIf(JsonNode::isObject)
            ?.let(::readWorkspace)
            ?.mergeLegacyUnitDialogs(state)
        value.put("schemaVersion", CURRENT_UI_SCHEMA_VERSION)
        value.put("selectedTool", state.selectedTool.name)
        value.putArray("selectedObjectIds").also { ids -> state.selectedObjectIds.forEach { ids.add(it.value.toString()) } }
        value.putArray("dialogs").also { dialogs -> state.dialogs.forEach { dialogs.add(it.toJson()) } }
        value.put("workspaceMode", state.workspaceMode.name)
        value.putArray("dockPanels").also { panels ->
            state.dockPanels.forEach { panel ->
                panels.addObject().apply {
                    put("region", panel.region)
                    put("size", panel.size)
                    putArray("tabIds").also { tabs -> panel.tabIds.forEach(tabs::add) }
                    panel.activeTabId?.let { put("activeTabId", it) }
                    put("collapsed", panel.collapsed)
                }
            }
        }
        value.putArray("enabledLayerIds").also { layers -> state.enabledLayerIds.forEach(layers::add) }
        value.putArray("openEditorIds").also { editors -> state.openEditorIds.forEach(editors::add) }
        value.putObject("drafts").also { drafts -> state.drafts.forEach(drafts::put) }
        workspace?.let { value.set<ObjectNode>("workspace", writeWorkspace(it)) }
    }

    fun updateOperationUiState(state: OperationUiState) = updateUiState(
        uiState().copy(
            selectedTool = state.selectedTool,
            selectedObjectIds = state.selectedObjectIds.toList(),
            workspaceMode = state.workspace.mode,
            enabledLayerIds = state.enabledLayerIds,
            openEditorIds = state.workspace.editors.map { it.id.value },
            drafts = state.workspace.compatibilityDrafts(),
            workspace = state.workspace,
            dialogs = uiState().dialogs.mergeWorkspaceDialogs(state.workspace),
            dockPanels = state.workspace.dockRegions.map { it.toCompatibilityPanel() },
        ),
    )

    fun operationUiState() = uiState().let { state ->
        OperationUiState(
            selectedTool = state.selectedTool,
            selectedObjectIds = state.selectedObjectIds.toCollection(linkedSetOf()),
            enabledLayerIds = state.enabledLayerIds,
            workspace = state.workspace ?: migrateWorkspace(state).let { migrated ->
                val schemaVersion = root.path(UI_STATE_FIELD).path("schemaVersion").intValue()
                if (schemaVersion < 2 && state.dialogs.isEmpty()) migrated.copy(mode = MapViewerWorkspaceMode.DOCKED) else migrated
            },
        )
    }

    private fun readWorkspace(node: JsonNode): OperationWorkspaceState? {
        val mode = node.path("mode").textValue()?.let { value -> MapViewerWorkspaceMode.entries.firstOrNull { it.name == value } } ?: return null
        val editors = node.path("editors").mapNotNull(::readEditor)
        val editorIds = editors.mapTo(hashSetOf()) { it.id }
        val docks = node.path("dockRegions").mapNotNull { dock ->
            val region = dock.path("region").textValue()?.let { value -> OperationDockRegion.entries.firstOrNull { it.name == value } }
                ?: return@mapNotNull null
            val size = dock.path("size").doubleValue().takeIf { dock.path("size").isNumber && it >= 0.0 } ?: return@mapNotNull null
            val ids = dock.path("editorIds").mapNotNull(JsonNode::textValue).map(::OperationEditorId).filter(editorIds::contains)
            OperationDockRegionState(
                region = region,
                size = size,
                editorIds = ids,
                activeEditorId = dock.path("activeEditorId").textValue()?.let(::OperationEditorId)?.takeIf(editorIds::contains),
                collapsed = dock.path("collapsed").booleanValue(),
            )
        }
        return OperationWorkspaceState(
            mode = mode,
            editors = editors,
            activeEditorId = node.path("activeEditorId").textValue()?.let(::OperationEditorId)?.takeIf(editorIds::contains),
            dockRegions = docks,
        )
    }

    private fun readEditor(node: JsonNode): OperationEditorState? {
        val id = node.path("id").textValue()?.let(::OperationEditorId) ?: return null
        val kind = node.path("kind").textValue()?.let(::OperationEditorKind) ?: return null
        val targetNode = node.path("target")
        val target = when (targetNode.path("type").textValue()) {
            "operation-notes" -> OperationEditorTarget.OperationNotes
            "object" -> targetNode.path("objectId").textValue()
                ?.let { runCatching { OperationObjectId(UUID.fromString(it)) }.getOrNull() }
                ?.let(OperationEditorTarget::Object)
                ?: return null

            "new-unit-template" -> OperationEditorTarget.NewUnitTemplate
            "unit-template" -> targetNode.path("templateId").textValue()
                ?.let { runCatching { OperationUnitTemplateId(UUID.fromString(it)) }.getOrNull() }
                ?.let(OperationEditorTarget::UnitTemplate)
                ?: return null

            "new-graphic-template" -> OperationEditorTarget.NewGraphicTemplate
            "graphic-template" -> targetNode.path("templateId").textValue()
                ?.let { runCatching { OperationGraphicTemplateId(UUID.fromString(it)) }.getOrNull() }
                ?.let(OperationEditorTarget::GraphicTemplate)
                ?: return null

            else -> OperationEditorTarget.Opaque(targetNode.path("value").textValue())
        }
        val draftNode = node.path("draft")
        val draft = when (kind) {
            OperationEditorKind.OPERATION_NOTES -> OperationEditorDraft.Notes(draftNode.path("body").textValue().orEmpty())
            OperationEditorKind.UNIT -> OperationEditorDraft.Unit(
                name = draftNode.path("name").textValue().orEmpty(),
                positionX = draftNode.path("positionX").textValue().orEmpty(),
                positionY = draftNode.path("positionY").textValue().orEmpty(),
                facing = draftNode.path("facing").textValue().orEmpty(),
                destroyed = draftNode.path("destroyed").booleanValue(),
                unitTemplateId = draftNode.path("unitTemplateId").textValue()?.let { runCatching { OperationUnitTemplateId(UUID.fromString(it)) }.getOrNull() },
                affiliation = draftNode.path("affiliation").textValue()?.let { runCatching { OperationUnitAffiliation.valueOf(it) }.getOrNull() },
                properties = readProperties(draftNode.path("properties")),
            )

            OperationEditorKind.UNIT_TEMPLATE -> OperationEditorDraft.UnitTemplate(
                OperationUnitTemplateDraft(
                    name = draftNode.path("name").textValue().orEmpty(),
                    affiliation = draftNode.path("affiliation").textValue()?.let { runCatching { OperationUnitAffiliation.valueOf(it) }.getOrNull() },
                    replacementImage = draftNode.path("image").takeIf(JsonNode::isObject)?.let { image ->
                        val name = image.path("originalFilename").textValue() ?: return@let null
                        val mediaType = image.path("mediaType").textValue() ?: return@let null
                        val bytes = image.path("base64Bytes").textValue() ?: return@let null
                        runCatching { OperationImageDraft.fromBase64(name, mediaType, bytes) }.getOrNull()
                    },
                    properties = readProperties(draftNode.path("properties")),
                ),
            )

            OperationEditorKind.GRAPHIC -> if (target is OperationEditorTarget.Object) OperationEditorDraft.Graphic(
                OperationGraphicDraft(
                    graphicTemplateId = draftNode.path("graphicTemplateId").textValue()
                        ?.let { runCatching { OperationGraphicTemplateId(UUID.fromString(it)) }.getOrNull() },
                    displayName = draftNode.path("displayName").textValue().orEmpty(),
                    positionX = draftNode.path("positionX").textValue().orEmpty(),
                    positionY = draftNode.path("positionY").textValue().orEmpty(),
                    properties = readProperties(draftNode.path("properties")),
                ),
            ) else OperationEditorDraft.Opaque(draftNode.path("value").textValue().orEmpty())

            OperationEditorKind.GRAPHIC_TEMPLATE -> if (target == OperationEditorTarget.NewGraphicTemplate || target is OperationEditorTarget.GraphicTemplate) OperationEditorDraft.GraphicTemplate(
                OperationGraphicTemplateDraft(
                    name = draftNode.path("name").textValue().orEmpty(),
                    widthPercent = draftNode.path("widthPercent").textValue().orEmpty(),
                    heightPercent = draftNode.path("heightPercent").textValue().orEmpty(),
                    replacementImage = readImageDraft(draftNode.path("image")),
                    properties = readProperties(draftNode.path("properties")),
                ),
            ) else OperationEditorDraft.Opaque(draftNode.path("value").textValue().orEmpty())

            else -> OperationEditorDraft.Opaque(draftNode.path("value").textValue().orEmpty())
        }
        val floatingNode = node.path("floating")
        val boundsNode = floatingNode.path("bounds")
        val boundsValues = listOf("left", "top", "width", "height").map(boundsNode::path)
        if (boundsValues.any { !it.isNumber }) return null
        val anchorNode = floatingNode.path("mapAnchor")
        val anchor = anchorNode.takeIf { it.path("x").isNumber && it.path("y").isNumber }
            ?.let { ViewerPoint(it.path("x").doubleValue(), it.path("y").doubleValue()) }
        return OperationEditorState(
            id = id,
            kind = kind,
            target = target,
            draft = draft,
            floating = OperationFloatingEditorState(
                bounds = OperationEditorBounds(
                    boundsValues[0].doubleValue(), boundsValues[1].doubleValue(),
                    boundsValues[2].doubleValue(), boundsValues[3].doubleValue(),
                ),
                minimized = floatingNode.path("minimized").booleanValue(),
                pinned = floatingNode.path("pinned").booleanValue() && anchor != null,
                mapAnchor = anchor,
            ),
        )
    }

    private fun readProperties(node: JsonNode): List<OperationPropertyDraft> =
        node.takeIf(JsonNode::isArray)?.map { value ->
            OperationPropertyDraft(value.path("key").textValue().orEmpty(), value.path("value").textValue().orEmpty())
        }.orEmpty()

    private fun readImageDraft(node: JsonNode) = node.takeIf(JsonNode::isObject)?.let { image ->
        val name = image.path("originalFilename").textValue() ?: return@let null
        val mediaType = image.path("mediaType").textValue() ?: return@let null
        val bytes = image.path("base64Bytes").textValue() ?: return@let null
        runCatching { OperationImageDraft.fromBase64(name, mediaType, bytes) }.getOrNull()
    }

    private fun writeProperties(properties: List<OperationPropertyDraft>) = JsonNodeFactory.instance.arrayNode().also { values ->
        properties.forEach { property -> values.addObject().apply { put("key", property.key); put("value", property.value) } }
    }

    private fun writeWorkspace(workspace: OperationWorkspaceState) = JsonNodeFactory.instance.objectNode().apply {
        put("mode", workspace.mode.name)
        workspace.activeEditorId?.let { put("activeEditorId", it.value) }
        putArray("editors").also { values -> workspace.editors.forEach { values.add(writeEditor(it)) } }
        putArray("dockRegions").also { values ->
            workspace.dockRegions.forEach { dock ->
                values.addObject().apply {
                    put("region", dock.region.name)
                    put("size", dock.size)
                    putArray("editorIds").also { ids -> dock.editorIds.forEach { ids.add(it.value) } }
                    dock.activeEditorId?.let { put("activeEditorId", it.value) }
                    put("collapsed", dock.collapsed)
                }
            }
        }
    }

    private fun writeEditor(editor: OperationEditorState) = JsonNodeFactory.instance.objectNode().apply {
        put("id", editor.id.value)
        put("kind", editor.kind.value)
        putObject("target").apply {
            when (val target = editor.target) {
                OperationEditorTarget.OperationNotes -> put("type", "operation-notes")
                is OperationEditorTarget.Object -> {
                    put("type", "object"); put("objectId", target.objectId.value.toString())
                }

                OperationEditorTarget.NewUnitTemplate -> put("type", "new-unit-template")
                is OperationEditorTarget.UnitTemplate -> {
                    put("type", "unit-template"); put("templateId", target.templateId.value.toString())
                }

                OperationEditorTarget.NewGraphicTemplate -> put("type", "new-graphic-template")
                is OperationEditorTarget.GraphicTemplate -> {
                    put("type", "graphic-template"); put("templateId", target.templateId.value.toString())
                }

                is OperationEditorTarget.Opaque -> {
                    put("type", "opaque"); target.value?.let { put("value", it) }
                }
            }
        }
        putObject("draft").apply {
            when (val draft = editor.draft) {
                is OperationEditorDraft.Notes -> put("body", draft.body)
                is OperationEditorDraft.Unit -> {
                    put("name", draft.name); put("positionX", draft.positionX); put("positionY", draft.positionY)
                    put("facing", draft.facing); put("destroyed", draft.destroyed)
                    draft.unitTemplateId?.let { put("unitTemplateId", it.value.toString()) }
                    draft.affiliation?.let { put("affiliation", it.name) }
                    set<com.fasterxml.jackson.databind.node.ArrayNode>("properties", writeProperties(draft.properties))
                }

                is OperationEditorDraft.UnitTemplate -> {
                    put("name", draft.value.name)
                    draft.value.affiliation?.let { put("affiliation", it.name) }
                    draft.value.replacementImage?.let { image -> putObject("image").apply {
                        put("originalFilename", image.originalFilename); put("mediaType", image.mediaType); put("base64Bytes", image.base64Bytes)
                    } }
                    set<com.fasterxml.jackson.databind.node.ArrayNode>("properties", writeProperties(draft.value.properties))
                }

                is OperationEditorDraft.Graphic -> {
                    draft.value.graphicTemplateId?.let { put("graphicTemplateId", it.value.toString()) }
                    put("displayName", draft.value.displayName)
                    put("positionX", draft.value.positionX); put("positionY", draft.value.positionY)
                    set<com.fasterxml.jackson.databind.node.ArrayNode>("properties", writeProperties(draft.value.properties))
                }

                is OperationEditorDraft.GraphicTemplate -> {
                    put("name", draft.value.name); put("widthPercent", draft.value.widthPercent); put("heightPercent", draft.value.heightPercent)
                    draft.value.replacementImage?.let { image -> putObject("image").apply {
                        put("originalFilename", image.originalFilename); put("mediaType", image.mediaType); put("base64Bytes", image.base64Bytes)
                    } }
                    set<com.fasterxml.jackson.databind.node.ArrayNode>("properties", writeProperties(draft.value.properties))
                }

                is OperationEditorDraft.Opaque -> put("value", draft.value)
            }
        }
        putObject("floating").apply {
            putObject("bounds").apply {
                put("left", editor.floating.bounds.left); put("top", editor.floating.bounds.top)
                put("width", editor.floating.bounds.width); put("height", editor.floating.bounds.height)
            }
            put("minimized", editor.floating.minimized)
            put("pinned", editor.floating.pinned)
            editor.floating.mapAnchor?.let { anchor -> putObject("mapAnchor").apply { put("x", anchor.x); put("y", anchor.y) } }
        }
    }

    fun toJsonOrNull(): ByteArray? =
        OperationSaveJson.objectMapper.writeValueAsBytes(root).takeIf { root.size() > 0 }

    companion object {
        const val UI_STATE_FIELD = "uiState"
        const val CURRENT_UI_SCHEMA_VERSION = 5

        fun fromJson(json: ByteArray?): OperationAdditionalData = OperationAdditionalData(
            runCatching {
                json?.takeIf { it.isNotEmpty() }
                    ?.let(OperationSaveJson.objectMapper::readTree)
                    ?.takeIf(JsonNode::isObject)
                    ?.deepCopy<ObjectNode>()
            }.getOrNull() ?: JsonNodeFactory.instance.objectNode(),
        )
    }
}
