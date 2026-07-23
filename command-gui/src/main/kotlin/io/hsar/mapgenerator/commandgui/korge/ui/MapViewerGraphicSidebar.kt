package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.AssetSource
import io.hsar.mapgenerator.commandgui.korge.interaction.ContextMenuManager
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateId
import io.hsar.mapgenerator.commandgui.korge.interaction.GraphicTemplateSnapshot
import io.hsar.mapgenerator.commandgui.korge.interaction.MapContextMenuTarget
import io.hsar.mapgenerator.commandgui.korge.interaction.MapPlacedObjectMenuAction
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerGraphicTemplate
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.onStageResized
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal class MapViewerGraphicSidebar(
    root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val contextMenuManager: ContextMenuManager,
    private val onGraphicDropped: (GraphicTemplateSnapshot, Vector2D) -> Unit,
    private val onTemplateCreated: (GraphicTemplateSnapshot) -> Unit,
    private val onTemplateDeleted: (GraphicTemplateSnapshot) -> Unit,
    private val onTemplateEdited: (GraphicTemplateSnapshot, GraphicTemplateSnapshot) -> Unit,
    private val onTransientNotice: (String) -> Unit = {},
) {
    private val overlay = root.container()
    private val toggleButton = overlay.container()
    private val dragLayer = overlay.container()
    private val templates = mutableListOf<GraphicTemplateSnapshot>()
    private var drag: DragState? = null
    private var dialogWindow: MapViewerDialogWindow? = null
    private var restoredDialog: MapViewerPersistedDialog? = null
    private var toggleBounds = ScreenBounds(0.0, 0.0, TOGGLE_WIDTH, TOGGLE_HEIGHT)

    init {
        overlay.zIndex = 10_100.0
        dragLayer.zIndex = 20_000.0
        dragLayer.mouseEnabled = false
        toggleButton.mouse {
            onClick {
                if (dialogWindow == null) openDialog() else closeDialog()
            }
        }
        overlay.onStageResized { width, height -> layout(width, height) }
        layout(overlay.stage?.views?.actualVirtualWidth ?: 800, overlay.stage?.views?.actualVirtualHeight ?: 600)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        toggleBounds.contains(point) || dialogWindow?.blocksScreenPoint(point) == true

    fun persistedDialog(): MapViewerPersistedDialog? =
        dialogWindow?.persistedDialog()

    fun restoreDialog(dialog: MapViewerPersistedDialog) {
        restoredDialog = dialog
        closeDialog()
        openDialog()
    }

    fun restoreTemplates(snapshots: List<GraphicTemplateSnapshot>) {
        templates.clear()
        templates += snapshots.sortedBy { it.index }
        dialogWindow?.refresh()
    }

    fun addTemplate(snapshot: GraphicTemplateSnapshot, notify: Boolean = true) {
        templates.removeAll { it.id == snapshot.id }
        templates.add(snapshot.index.coerceIn(0, templates.size), snapshot)
        dialogWindow?.refresh()
        if (notify) onTemplateCreated(snapshot)
    }

    fun replaceTemplate(snapshot: GraphicTemplateSnapshot, notify: Boolean = true): GraphicTemplateSnapshot? {
        val index = templates.indexOfFirst { it.id == snapshot.id }
        if (index < 0) return null
        val before = templates[index]
        templates[index] = snapshot.copy(index = index)
        dialogWindow?.refresh()
        if (notify && before != templates[index]) onTemplateEdited(before, templates[index])
        return templates[index]
    }

    fun removeTemplate(id: GraphicTemplateId, notify: Boolean = true): GraphicTemplateSnapshot? =
        templates.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index ->
            templates.removeAt(index).also {
                dialogWindow?.refresh()
                if (notify) onTemplateDeleted(it)
            }
        }

    fun template(id: GraphicTemplateId): GraphicTemplateSnapshot? = templates.firstOrNull { it.id == id }

    fun update(pointer: Vector2D, leftHeld: Boolean) {
        val state = drag ?: return
        state.preview.xy(pointer.x + PREVIEW_OFFSET, pointer.y + PREVIEW_OFFSET)
        if (leftHeld) return
        if (!blocksScreenPoint(pointer)) onGraphicDropped(state.template, pointer)
        state.preview.removeFromParent()
        drag = null
    }

    private fun layout(width: Int, height: Int) {
        val toggleLeft = width - OUTER_PADDING - TOGGLE_WIDTH * 2.0 - TOGGLE_GAP
        toggleBounds = ScreenBounds(toggleLeft, OUTER_PADDING, TOGGLE_WIDTH, TOGGLE_HEIGHT)
        renderToggle()
    }

    private fun renderToggle() {
        toggleButton.removeChildren()
        MapViewerSidePanelChrome.renderToggle(
            parent = toggleButton,
            bounds = toggleBounds,
            label = "GRAPHICS",
        )
    }

    private fun openDialog() {
        val stage = overlay.stage ?: return
        if (dialogWindow != null) return
        dialogWindow = MapViewerDialogWindow(
            root = overlay,
            title = "Graphics",
            initialPosition = { defaultBounds ->
                MapViewerDialogLayoutBuilder.alignRight(defaultBounds, layoutViewportWidth())
            },
            persistence = MapViewerDialogPersistence(
                type = "graphics",
                restored = restoredDialog,
            ),
            preferredWidth = PANEL_WIDTH,
            preferredHeight = contentHeight(),
            contentMinimumWidth = { CONTENT_MINIMUM_WIDTH },
            contentHeight = { contentHeight() },
            onClose = ::closeDialog,
            renderContent = ::renderContent,
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        )
        dialogWindow?.show(stage.views.actualVirtualWidth, stage.views.actualVirtualHeight)
        restoredDialog = null
    }

    private fun closeDialog() {
        dialogWindow?.close()
        dialogWindow = null
    }

    private fun layoutViewportWidth(): Int =
        overlay.stage?.views?.actualVirtualWidth ?: 0

    private fun renderContent(parent: Container, itemWidth: Double) {
        val buttonWidth = 180.0
        parent.container().also { addButton ->
            addButton.xy(itemWidth - buttonWidth, 0.0)
            MapViewerUiBoxes.button(addButton, buttonWidth, HEADER_HEIGHT)
            MapViewerTextViews.box(addButton, "New Graphic / Template", buttonWidth, HEADER_HEIGHT, color = Palette.Colours.WHITE)
            addButton.mouse { onClick { importGraphic() } }
        }
        val list = parent.container().xy(0.0, HEADER_HEIGHT + HEADER_GAP)
        templates.forEachIndexed { index, snapshot ->
            MapViewerUniversalList.renderItem(
                parent = list,
                item = MapViewerUniversalListItem(title = snapshot.template.name, bitmap = snapshot.template.bitmap),
                width = itemWidth,
                top = index * (MapViewerUniversalList.DEFAULT_ROW_HEIGHT + ROW_GAP),
                onDown = { button, point ->
                    if (button == MouseButton.LEFT) beginDrag(snapshot) else if (button == MouseButton.RIGHT) openMenu(snapshot, point)
                },
            )
        }
    }

    private fun contentHeight(): Double =
        HEADER_HEIGHT + HEADER_GAP + templates.size * (MapViewerUniversalList.DEFAULT_ROW_HEIGHT + ROW_GAP)

    private fun importGraphic() {
        val file = JFileChooser().apply {
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")
        }.takeIf { it.showOpenDialog(null) == JFileChooser.APPROVE_OPTION }?.selectedFile ?: return
        val bitmap = MapViewerIconLoader.load(file) ?: return onTransientNotice("Could not import graphic")
        val id = GraphicTemplateId(UUID.randomUUID())
        addTemplate(
            GraphicTemplateSnapshot(
                id = id,
                template = MapViewerGraphicTemplate(id, file.nameWithoutExtension, bitmap, AssetSource.fromFile(file)),
                index = templates.size,
            ),
        )
    }

    private fun beginDrag(template: GraphicTemplateSnapshot) {
        dragLayer.removeChildren()
        val preview = dragLayer.container()
        MapViewerUiBoxes.imageBox(preview, template.template.bitmap, PREVIEW_WIDTH, PREVIEW_HEIGHT)
        drag = DragState(template, preview)
    }

    private fun openMenu(snapshot: GraphicTemplateSnapshot, point: Vector2D) = contextMenuManager.open(
        target = object : MapContextMenuTarget {
            override fun contextMenuActions() = emptyList<MapPlacedObjectMenuAction>()
        },
        screenPoint = point,
        actions = listOf(
            MapPlacedObjectMenuAction("Replace Icon", { replaceIcon(snapshot) }),
            MapPlacedObjectMenuAction("Delete Graphic", { removeTemplate(snapshot.id) }),
        ),
    )

    private fun replaceIcon(snapshot: GraphicTemplateSnapshot) {
        val file = JFileChooser().apply {
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")
        }.takeIf { it.showOpenDialog(null) == JFileChooser.APPROVE_OPTION }?.selectedFile ?: return
        val bitmap = MapViewerIconLoader.load(file) ?: return onTransientNotice("Could not import graphic")
        replaceTemplate(snapshot.copy(template = snapshot.template.copy(bitmap = bitmap, assetSource = AssetSource.fromFile(file))))
    }

    private data class DragState(val template: GraphicTemplateSnapshot, val preview: Container)

    private companion object {
        const val OUTER_PADDING = 16.0
        const val TOGGLE_WIDTH = 120.0
        const val TOGGLE_HEIGHT = 40.0
        const val TOGGLE_GAP = 12.0
        const val PANEL_WIDTH = 320.0
        const val CONTENT_MINIMUM_WIDTH = 280.0
        const val HEADER_HEIGHT = 40.0
        const val HEADER_GAP = 12.0
        const val ROW_GAP = 8.0
        const val PREVIEW_WIDTH = 120.0
        const val PREVIEW_HEIGHT = 40.0
        const val PREVIEW_OFFSET = 12.0
    }
}
