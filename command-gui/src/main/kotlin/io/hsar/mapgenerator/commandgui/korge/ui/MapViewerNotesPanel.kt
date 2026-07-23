package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.CameraViewport
import io.hsar.mapgenerator.commandgui.korge.interaction.CreateNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.DeleteNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.EditNoteAction
import io.hsar.mapgenerator.commandgui.korge.interaction.LedgerEntry
import io.hsar.mapgenerator.commandgui.korge.interaction.MapCoordinateSpace
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNotePatch
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapNoteStore
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectId
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectSnapshot
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D

@OptIn(KorgeExperimental::class)
internal class MapViewerNotesPanel(
    root: Container,
    viewportWidth: Int,
    viewportHeight: Int,
    private val noteStore: MapNoteStore,
    private val dialogManager: MapViewerDialogManager,
    private val viewportProvider: () -> CameraViewport,
    private val coordinateSpace: MapCoordinateSpace,
    private val isReadOnly: () -> Boolean,
    private val objectSnapshot: (MapPlacedObjectId) -> MapPlacedObjectSnapshot?,
    private val onLedgerEntry: (LedgerEntry) -> Unit,
) {
    private val overlay = root.container()
    private val toggleButton = overlay.container()
    private var collapsed = true
    private var dialogWindow: MapViewerDialogWindow? = null
    private var restoredPanelBounds: MapViewerDialogBounds? = null
    private val noteDialogs = mutableSetOf<MapViewerNoteDialog>()
    private var layout = MapViewerNotesPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed)

    init {
        overlay.zIndex = Z_INDEX
        toggleButton.mouse {
            onClick {
                if (collapsed) {
                    openDialog()
                } else {
                    closeDialog()
                }
                relayout(layoutViewportWidth(), layoutViewportHeight())
            }
        }
        overlay.onStageResized { width, height -> relayout(width, height) }
        relayout(viewportWidth, viewportHeight)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        layout.toggleBounds.contains(point) ||
                dialogWindow?.blocksScreenPoint(point) == true ||
                dialogManager.blocksScreenPoint(point)

    fun refresh() {
        dialogWindow?.refresh()
    }

    fun addDroppedReferences(point: Vector2D, objectIds: Set<MapPlacedObjectId>): Boolean =
        noteDialogs.any { dialog -> dialog.addReferencesAt(point, objectIds) }

    fun openNote(noteId: MapNoteId): Boolean =
        noteStore.snapshot(noteId)?.let { openNote(it) } ?: false

    fun persistedDialog(): MapViewerPersistedDialog? =
        dialogWindow?.persistedDialog()

    fun restoreDialog(dialog: MapViewerPersistedDialog) {
        restoredPanelBounds = dialog.bounds
        closeDialog()
        openDialog()
    }

    fun restoreNote(dialog: MapViewerPersistedDialog): Boolean =
        dialog.targetId
            ?.let { runCatching { MapNoteId(java.util.UUID.fromString(it)) }.getOrNull() }
            ?.let(noteStore::snapshot)
            ?.let { openNote(it, dialog) }
            ?: false

    private fun relayout(viewportWidth: Int, viewportHeight: Int) {
        layout = MapViewerNotesPanelLayoutBuilder.build(viewportWidth, viewportHeight, collapsed = true)
        MapViewerSidePanelChrome.renderToggle(
            parent = toggleButton,
            bounds = layout.toggleBounds,
            label = "NOTES",
        )
    }

    private fun openDialog() {
        val stage = overlay.stage ?: return
        collapsed = false
        dialogWindow = MapViewerDialogWindow(
            root = overlay,
            title = "Notes",
            initialPosition = ::initialDialogPosition,
            persistence = MapViewerDialogPersistence(
                type = "notes",
                restored = restoredPanelBounds?.let { bounds -> MapViewerPersistedDialog("notes", bounds = bounds) },
            ),
            preferredWidth = MapViewerNotesPanelLayoutBuilder.PANEL_WIDTH,
            preferredHeight = PREFERRED_DIALOG_HEIGHT,
            contentMinimumWidth = { CONTENT_MINIMUM_WIDTH },
            contentHeight = ::contentHeight,
            onClose = ::closeDialog,
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
        )
        dialogWindow?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        restoredPanelBounds = null
    }

    private fun initialDialogPosition(defaultBounds: MapViewerDialogBounds): Vector2D =
        MapViewerDialogLayoutBuilder.alignLeft(defaultBounds)

    private fun closeDialog() {
        collapsed = true
        dialogWindow?.close()
        dialogWindow = null
    }

    private fun renderContent(parent: Container, itemWidth: Double) {
        val createButton = parent.container()
        createButton.xy(0.0, 0.0)
        MapViewerActionButtons.button(
            parent = createButton,
            width = CREATE_BUTTON_WIDTH,
            height = CONTROL_HEIGHT,
            label = "Create Note",
            enabled = !isReadOnly(),
        ) {
            val snapshot = noteStore.createNote()
            onLedgerEntry(CreateNoteAction(snapshot))
            dialogWindow?.refresh()
            openNote(snapshot)
        }

        val notes = noteStore.snapshots()
        if (notes.isEmpty()) {
            MapViewerTextViews.box(
                parent = parent,
                text = "No notes",
                width = itemWidth,
                height = EMPTY_HEIGHT,
                color = Palette.Colours.WHITE,
            ) {
                y = CONTROL_HEIGHT + SECTION_GAP
            }
            return
        }

        notes.forEachIndexed { index, note ->
            renderNoteRow(
                parent = parent,
                note = note,
                width = itemWidth,
                top = CONTROL_HEIGHT + SECTION_GAP + index * (ROW_HEIGHT + ROW_GAP),
            )
        }
    }

    private fun renderNoteRow(parent: Container, note: MapNoteSnapshot, width: Double, top: Double) {
        MapViewerUniversalList.renderItem(
            parent = parent,
            item = MapViewerUniversalListItem(
                title = note.title.ifBlank { "Untitled Note" },
                subtitle = note.body.lineSequence().firstOrNull().orEmpty(),
                trailingLabel = note.referencedObjectIds.size.toString(),
            ),
            width = width,
            top = top,
            height = ROW_HEIGHT,
            onClick = { openNote(note) },
        )
    }

    private fun openNote(note: MapNoteSnapshot, restored: MapViewerPersistedDialog? = null): Boolean {
        MapViewerNoteDialog(
            root = overlay,
            dialogManager = dialogManager,
            note = noteStore.snapshot(note.id) ?: note,
            readOnly = isReadOnly,
            objectSnapshot = objectSnapshot,
            viewportProvider = viewportProvider,
            coordinateSpace = coordinateSpace,
            onSaved = { before, after ->
                noteStore.upsert(after)
                MapNotePatch.diff(before, after)
                    .takeIf { it.isNotEmpty() }
                    ?.let { forward -> onLedgerEntry(EditNoteAction(before.id, forward, MapNotePatch.diff(after, before))) }
                dialogWindow?.refresh()
            },
            onDeleted = { snapshot ->
                noteStore.delete(snapshot.id)
                onLedgerEntry(DeleteNoteAction(snapshot))
                dialogWindow?.refresh()
            },
            onClosed = { dialog -> noteDialogs -= dialog },
            restored = restored,
        ).also { dialog ->
            if (dialog.open()) {
                noteDialogs += dialog
                return true
            }
        }
        return false
    }

    private fun contentHeight(width: Double): Double =
        CONTROL_HEIGHT +
                SECTION_GAP +
                if (noteStore.snapshots().isEmpty()) {
                    EMPTY_HEIGHT
                } else {
                    noteStore.snapshots().size * (ROW_HEIGHT + ROW_GAP)
                }

    private fun layoutViewportWidth(): Int =
        overlay.stage?.views?.actualVirtualWidth ?: 0

    private fun layoutViewportHeight(): Int =
        overlay.stage?.views?.actualVirtualHeight ?: 0

    companion object {
        const val Z_INDEX = 10_000.0
        const val PREFERRED_DIALOG_HEIGHT = 520.0
        const val CONTENT_MINIMUM_WIDTH = 320.0
        const val CREATE_BUTTON_WIDTH = 140.0
        const val CONTROL_HEIGHT = 40.0
        const val SECTION_GAP = 14.0
        const val EMPTY_HEIGHT = 80.0
        const val ROW_HEIGHT = 62.0
        const val ROW_GAP = 10.0
    }
}
