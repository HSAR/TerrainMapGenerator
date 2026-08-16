package io.hsar.mapgenerator.commandgui

import io.hsar.mapgenerator.commandgui.operation.OperationContent
import io.hsar.mapgenerator.commandgui.operation.OperationNoteId
import io.hsar.mapgenerator.commandgui.operation.OperationObjectId
import io.hsar.mapgenerator.commandgui.operation.OperationUnit
import io.hsar.mapgenerator.commandgui.operation.OperationUnitPresentation
import io.hsar.mapgenerator.commandgui.operation.OperationWorkspaceState
import io.hsar.mapgenerator.commandgui.operation.OperationDestroyedUnitDisplayMode
import io.hsar.mapgenerator.commandgui.operation.OperationFireArcDisplayMode

enum class MapViewerPanel { GAME, NOTES, GRAPHICS, UNITS }

enum class MapViewerObjectCommand {
    EDIT_UNIT,
    DELETE_UNIT,
    SCIREP,
    NAVREP,
    TACTICAL,
    STATUS,
    EDIT_GRAPHIC,
    DELETE_GRAPHIC,
    EDIT_DRAWING_NAME,
    DELETE_DRAWING,
}

sealed interface MapViewerCommand {
    data class TogglePanel(val panel: MapViewerPanel) : MapViewerCommand
    data object Save : MapViewerCommand
    data object Undo : MapViewerCommand
    data object Redo : MapViewerCommand
    data object ReturnToLaunchpad : MapViewerCommand
    data object Exit : MapViewerCommand
    data object CloseEscapeMenu : MapViewerCommand
    data class SetWorkspaceMode(val mode: MapViewerWorkspaceMode) : MapViewerCommand
    data class SetDestroyedUnitDisplayMode(val mode: OperationDestroyedUnitDisplayMode) : MapViewerCommand
    data class SetUnitIconSize(val sizePx: Double) : MapViewerCommand
    data class SetFireArcDisplayMode(val mode: OperationFireArcDisplayMode) : MapViewerCommand
    data class DeleteObjects(val objectIds: Set<OperationObjectId>) : MapViewerCommand
    data class ObjectAction(val objectId: OperationObjectId, val action: MapViewerObjectCommand) : MapViewerCommand
    data class OpenNote(val noteId: OperationNoteId) : MapViewerCommand
}

data class MapViewerCommandItem(
    val label: String,
    val command: MapViewerCommand? = null,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val destructive: Boolean = false,
    val children: List<MapViewerCommandItem> = emptyList(),
)

data class MapViewerCommandContext(
    val content: OperationContent,
    val selectedObjectIds: Set<OperationObjectId>,
    val readOnly: Boolean,
    val olympiadDawnEnabled: Boolean,
    val activeTool: MapViewerTool = MapViewerTool.SELECT,
    val activeWorkflow: Boolean = false,
    val workspace: OperationWorkspaceState = OperationWorkspaceState.default(),
    val saveEnabled: Boolean = true,
)

object MapViewerCommandResolver {
    fun contextMenu(targetId: OperationObjectId, context: MapViewerCommandContext): List<MapViewerCommandItem> {
        if (targetId in context.selectedObjectIds && context.selectedObjectIds.size > 1) {
            return listOf(
                commandItem(
                    label = "Delete Selected",
                    command = MapViewerCommand.DeleteObjects(context.selectedObjectIds),
                    enabled = !context.readOnly,
                    disabledReason = "History preview is read-only.",
                    destructive = true,
                ),
            )
        }
        val target = context.content.objectById(targetId) ?: return emptyList()
        val mutable = !context.readOnly
        val items = when (target) {
            is OperationUnit -> buildList {
                add(commandItem("Edit Unit", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.EDIT_UNIT)))
                add(commandItem("Delete Unit", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.DELETE_UNIT), mutable, "History preview is read-only.", true))
                if (context.olympiadDawnEnabled) {
                    val reportError = OperationUnitPresentation.sitrepValidationError(target, context.content)
                    val reportDisabledReason = if (!mutable) "History preview is read-only." else reportError
                    add(commandItem("SCIREP", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.SCIREP), mutable && reportError == null, reportDisabledReason))
                    add(commandItem("NAVREP", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.NAVREP), mutable && reportError == null, reportDisabledReason))
                    add(commandItem("TACTICAL", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.TACTICAL)))
                    add(commandItem("STATUS", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.STATUS)))
                }
            }

            is io.hsar.mapgenerator.commandgui.operation.OperationGraphic -> listOf(
                commandItem("Edit Graphic", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.EDIT_GRAPHIC)),
                commandItem("Delete Graphic", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.DELETE_GRAPHIC), mutable, "History preview is read-only.", true),
            )

            is io.hsar.mapgenerator.commandgui.operation.OperationDrawingObject -> listOf(
                commandItem("Edit Name", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.EDIT_DRAWING_NAME), mutable, "History preview is read-only."),
                commandItem("Delete Drawing", MapViewerCommand.ObjectAction(targetId, MapViewerObjectCommand.DELETE_DRAWING), mutable, "History preview is read-only.", true),
            )
        }
        val notes = context.content.notes.filter { targetId in it.referencedObjectIds }.map { note ->
            commandItem(note.title.ifBlank { "Untitled Note" }, MapViewerCommand.OpenNote(note.id))
        }
        return items + if (notes.isEmpty()) emptyList() else listOf(MapViewerCommandItem("Notes", children = notes))
    }

    private fun commandItem(
        label: String,
        command: MapViewerCommand,
        enabled: Boolean = true,
        disabledReason: String? = null,
        destructive: Boolean = false,
    ) = MapViewerCommandItem(label, command, enabled, disabledReason.takeUnless { enabled }, destructive)
}
