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
    private val contextMenuManager: ContextMenuManager,
    private val onGraphicDropped: (GraphicTemplateSnapshot, Vector2D) -> Unit,
    private val onTemplateCreated: (GraphicTemplateSnapshot) -> Unit,
    private val onTemplateDeleted: (GraphicTemplateSnapshot) -> Unit,
    private val onTemplateEdited: (GraphicTemplateSnapshot, GraphicTemplateSnapshot) -> Unit,
    private val onTransientNotice: (String) -> Unit = {},
) {
    private val overlay = root.container()
    private val toggleButton = overlay.container()
    private val panel = overlay.container()
    private val templates = mutableListOf<GraphicTemplateSnapshot>()
    private var open = false
    private var drag: DragState? = null
    private var toggleBounds = ScreenBounds(0.0, 0.0, TOGGLE_WIDTH, TOGGLE_HEIGHT)

    init {
        overlay.zIndex = 10_100.0
        toggleButton.mouse {
            onClick {
                open = !open
                panel.visible = open
                render()
            }
        }
        overlay.onStageResized { width, height -> layout(width, height) }
        layout(overlay.stage?.views?.actualVirtualWidth ?: 800, overlay.stage?.views?.actualVirtualHeight ?: 600)
    }

    fun blocksScreenPoint(point: Vector2D): Boolean =
        toggleBounds.contains(point) ||
                (open && point.x in panel.x..(panel.x + PANEL_WIDTH) && point.y in panel.y..(panel.y + PANEL_HEIGHT + templates.size * (MapViewerUniversalList.DEFAULT_ROW_HEIGHT + ROW_GAP)))

    fun restoreTemplates(snapshots: List<GraphicTemplateSnapshot>) {
        templates.clear()
        templates += snapshots.sortedBy { it.index }
        render()
    }

    fun addTemplate(snapshot: GraphicTemplateSnapshot, notify: Boolean = true) {
        templates.removeAll { it.id == snapshot.id }
        templates.add(snapshot.index.coerceIn(0, templates.size), snapshot)
        render()
        if (notify) onTemplateCreated(snapshot)
    }

    fun replaceTemplate(snapshot: GraphicTemplateSnapshot, notify: Boolean = true): GraphicTemplateSnapshot? {
        val index = templates.indexOfFirst { it.id == snapshot.id }
        if (index < 0) return null
        val before = templates[index]
        templates[index] = snapshot.copy(index = index)
        render()
        if (notify && before != templates[index]) onTemplateEdited(before, templates[index])
        return templates[index]
    }

    fun removeTemplate(id: GraphicTemplateId, notify: Boolean = true): GraphicTemplateSnapshot? =
        templates.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index ->
            templates.removeAt(index).also { render(); if (notify) onTemplateDeleted(it) }
        }

    fun template(id: GraphicTemplateId): GraphicTemplateSnapshot? = templates.firstOrNull { it.id == id }

    fun update(pointer: Vector2D, leftHeld: Boolean) {
        val state = drag ?: return
        state.preview.xy(pointer.x + 12.0, pointer.y + 12.0)
        if (leftHeld) return
        if (!blocksScreenPoint(pointer)) onGraphicDropped(state.template, pointer)
        state.preview.removeFromParent()
        drag = null
    }

    private fun layout(width: Int, height: Int) {
        val toggleLeft = width - OUTER_PADDING - TOGGLE_WIDTH * 2.0 - TOGGLE_GAP
        toggleBounds = ScreenBounds(toggleLeft, OUTER_PADDING, TOGGLE_WIDTH, TOGGLE_HEIGHT)
        panel.x = toggleLeft
        panel.y = OUTER_PADDING + TOGGLE_HEIGHT + PANEL_TOP_GAP
        panel.visible = open
        renderToggle()
        render()
    }

    private fun renderToggle() {
        toggleButton.removeChildren()
        MapViewerSidePanelChrome.renderToggle(
            parent = toggleButton,
            bounds = toggleBounds,
            label = "GRAPHICS",
        )
    }

    private fun render() {
        panel.removeChildren()
        if (!open) return
        val import = panel.container().xy(0.0, 0.0)
        MapViewerUiBoxes.button(import, PANEL_WIDTH, ROW_HEIGHT)
        MapViewerTextViews.box(import, "New Graphic / Template", PANEL_WIDTH, ROW_HEIGHT, color = Palette.Colours.WHITE)
        import.mouse { this.onClick { importGraphic() } }
        templates.forEachIndexed { index, snapshot ->
            MapViewerUniversalList.renderItem(
                parent = panel,
                item = MapViewerUniversalListItem(title = snapshot.template.name, bitmap = snapshot.template.bitmap),
                width = PANEL_WIDTH,
                top = ROW_HEIGHT + 8.0 + index * (MapViewerUniversalList.DEFAULT_ROW_HEIGHT + ROW_GAP),
                onDown = { button, point ->
                    if (button == MouseButton.LEFT) beginDrag(snapshot) else if (button == MouseButton.RIGHT) openMenu(snapshot, point)
                },
            )
        }
    }

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
        val preview = overlay.container()
        MapViewerUiBoxes.imageBox(preview, template.template.bitmap, 120.0, 40.0)
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
        const val PANEL_TOP_GAP = 12.0
        const val PANEL_WIDTH = 320.0
        const val PANEL_HEIGHT = 80.0
        const val ROW_HEIGHT = 40.0
        const val ROW_GAP = 8.0
    }
}
